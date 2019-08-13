package com.fourcode.tracking.ui.map


import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar

import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R

import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.*
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

import kotlinx.android.synthetic.main.map_fragment.*

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView

@SuppressLint("MissingPermission")
class MapFragment : Fragment(),
    OnMapReadyCallback, PermissionsListener,
    LocationEngineCallback<LocationEngineResult>,
            Callback<DirectionsResponse> {

    /** MapboxMap object to manipulate map elements
     * NOTE: Do not confuse with MapView (I use kotlin synthetic with that) */
    private lateinit var map: MapboxMap

    /** Object to let Mapbox handle permissions
     * internally, will make things less complicated. */
    private lateinit var permissions: PermissionsManager

    /** Object to get device's location (lat, lng)
     * which might be needed for navigation  */
    private lateinit var engine: LocationEngine

    /** Intent used to search places (reverse geocoding). */
    private lateinit var autocomplete: Intent

    /** Adapter for destinations_recycler_view, will pull out data from here*/
    private lateinit var adapter: DestinationsAdapter

    /** ViewModel object (will implement Android Architecture components) */
    private lateinit var model: MapViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mapbox access token is configured here, null check with context
        context?.let {  Mapbox.getInstance(it, BuildConfig.MapboxApiKey) }

        // Initialize map's ViewModel
        model = ViewModelProviders.of(this)[MapViewModel::class.java]

        // Start listening for updates
        model.location.observe(this, Observer {
            map.locationComponent.forceLocationUpdate(it)
        })

        model.distance.observe(this, Observer {
            val value =
                if (it >= 1000)
                    getString(R.string.format_distance_kilometers, it / 1000)
                else getString(R.string.format_distance_meters, it)
            total_distance_text.text = value
        })

        model.duration.observe(this, Observer {
            total_duration_text.text = getReadableTime(it.toInt())
        })

        model.destinations.value = arrayListOf()
        model.destinations.observe(this, Observer {
            if (it.isNullOrEmpty())
                return@Observer

            // Add item to adapters and update UI
            adapter.items.add(it.last())
            adapter.notifyDataSetChanged()

            // Fetch directions if location is not null
            model.location.value?.run {

                // Request directions from mapbox
                val builder = MapboxDirections.builder()
                    .accessToken(BuildConfig.MapboxApiKey)
                    .origin(Point.fromLngLat(longitude, latitude))
                    .overview(DirectionsCriteria.OVERVIEW_FULL)
                    .profile(DirectionsCriteria.PROFILE_DRIVING)

                adapter.items.forEachIndexed { index, item ->
                    // Extract point
                    val point = item.center()!!

                    // Check if item is the lastIndex
                    if (adapter.items.lastIndex == index)
                        builder.destination(point)
                    else builder.addWaypoint(point)
                }

                // Call to API
                builder.build().enqueueCall(this@MapFragment)
            }

            // Will run once items has been populated
            if (adapter.items.size == 1) {
                bottom_sheet_header.visibility = View.VISIBLE
                destinations_title.setText(R.string.title_destinations)
                navigate_fab.show()

                // Run spotlight
                MaterialShowcaseView.Builder(activity)
                    .setTarget(navigate_fab)
                    .setContentText(R.string.msg_showcase_start_navigation)
                    .setDismissText(R.string.action_showcase_done)
                    .setDismissTextColor(ContextCompat.
                        getColor(context!!, R.color.colorPrimaryDark))
                    // Works with bottom sheet
                    .renderOverNavigationBar()
                    .show()
            }

        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(
        R.layout.map_fragment,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize mapView (kotlin synthetic object)
        map_view.onCreate(savedInstanceState)
        map_view.getMapAsync(this)

        // Intiialize RecyclerView
        with(destinations_recycler_view) {
            layoutManager = LinearLayoutManager(context)
        }

        // Initially hide fab, it ain't ready yet
        add_destination_fab.hide()
        navigate_fab.hide()
    }

    /** Will run when a place got picked from autocomplete*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_AUTOCOMPLETE) {
           model.destinations.value =  model.destinations.value?.
               plus(PlaceAutocomplete.getPlace(data))
        }
    }

    /** Callback function for MapView.getAsync().
     * Runs after Mapview is initialized. */
    override fun onMapReady(mapboxMap: MapboxMap) {
        // Set a style then enable location component
        map = mapboxMap.apply {
            // Initialize map and set style
                setStyle(Style.TRAFFIC_DAY) {
                // Check if permissions are granted
                if (PermissionsManager.areLocationPermissionsGranted(context)) {
                    initializeMapComponents(it)
                    initializeLocationEngine()
                } else {
                    permissions = PermissionsManager(this@MapFragment)
                    permissions.requestLocationPermissions(activity)
                }
            }
        }

        // Initialize adapter for recycler view
        adapter = DestinationsAdapter(arrayListOf(), map)
        destinations_recycler_view.adapter = adapter
    }

    /** Enables location component, must be called after
     * style is loaded and permissions are granted */
    private fun initializeMapComponents(style: Style) {
        context?.let {context ->

            // Initialize location component
            with(map.locationComponent) {
                // Activate location component with the following options
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style)
                        .useDefaultLocationEngine(false)
                        .build()
                )

                // Set component modes and enable
                renderMode = RenderMode.NORMAL
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true

                Timber.i("Initialized location component")
            }

            // Initialize route source
            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID,
                FeatureCollection.fromFeatures(arrayOf())))

            // Initialize route source
            style.addSource(GeoJsonSource(DEST_SOURCE_ID,
                FeatureCollection.fromFeatures(arrayOf())))


            // Add route layer to style
            style.addLayer(LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineWidth(5f),
                lineColor(Color.parseColor("#009688"))
            ))

            // Add icon to style
            style.addImage(DEST_ICON_ID, resources.getDrawable(
                R.drawable.ic_place_primary_24dp, activity?.theme))

            // Add symbol layer for the dest icon
            style.addLayer(SymbolLayer(DEST_LAYER_ID, DEST_SOURCE_ID).withProperties(
                iconImage(DEST_ICON_ID),
                iconIgnorePlacement(true),
                iconIgnorePlacement(true),
                iconOffset(arrayOf(0f, -4f))
            ))

        }
    }

    /** Initialize a location engine, and start it */
    private fun initializeLocationEngine() {
        context?.let { context ->
            engine = LocationEngineProvider
                .getBestLocationEngine(context).also {
                    it.requestLocationUpdates(

                        // Build a LocationEngineRequest
                        LocationEngineRequest.Builder(DEFAULT_INTERVAL_MS)
                            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                            .build(),

                        // Set callback to this class and pass activity's main looper
                        this, context.mainLooper
                    )
                }
            Timber.i("Initialized location engine")
        }
    }

    /** Android Permission callback, will be run
     * after user interacts with permission dialog. */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        this.permissions.onRequestPermissionsResult(
            requestCode, permissions, grantResults)
    }

    /** Shown when app asks for permission rationale. */
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Snackbar.make(map_view,
            R.string.msg_location_permissions_needed,
            Snackbar.LENGTH_LONG)
            .setAction(R.string.action_retry) {
                permissions.requestLocationPermissions(activity)
            }.show()
    }

    /** Function is called after onRequestPermissionResult. */
    override fun onPermissionResult(granted: Boolean) {
        // Set a style then enable location component
        if (granted) initializeLocationEngine()
    }

    /** LocationEngineCallbaack method  */
    override fun onSuccess(result: LocationEngineResult?) {
        result?.lastLocation?.run {

            // Will initialize stuff once location is found
            // NOTE: Should only run once
            if (model.location.value == null) {
                model.location.value = this

                // Update initial camera position and zooming
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(latitude, longitude), 12.0))

                // Build intent for autocomplete
                val options = PlaceOptions.builder()
                    .backgroundColor(ContextCompat.getColor(context!!,
                        R.color.colorAutocompleteBackground))
                    .proximity(Point.fromLngLat(longitude, latitude))
                    .build(PlaceOptions.MODE_CARDS)
                autocomplete = PlaceAutocomplete.IntentBuilder()
                    .accessToken(BuildConfig.MapboxApiKey)
                    .placeOptions(options)
                    .build(activity)

                // Set up fab to open PlaceAutocomplete activity on click
                with(add_destination_fab) {
                    setOnClickListener {
                        startActivityForResult(autocomplete, REQUEST_AUTOCOMPLETE)
                    }

                    // Show fab, cuz it's fucking ready
                    show()

                    // Run spotlight
                    MaterialShowcaseView.Builder(activity)
                        .setTarget(this)
                        .setContentText(R.string.msg_showcase_add_destination)
                        .setDismissText(R.string.action_showcase_done)
                        .setDismissTextColor(ContextCompat.
                            getColor(context, R.color.colorPrimaryDark))
                        // Works with bottom sheet
                        .renderOverNavigationBar()
                        .show()
                }
            }
        }
    }

    /** LocationEngineCallbaack method  */
    override fun onFailure(exception: Exception) {
        Timber.w(exception,"Failed retrieving location")
    }

    /** Mapbox Matrix API callback method */
    override fun onResponse(call: Call<DirectionsResponse>,
                            response: Response<DirectionsResponse>) {
        val directions = response.body()!!
        val route = directions.routes()[0]

        // Update ViewModel
        model.duration.value = route.duration()!!
        model.distance.value = route.distance()!!

        // Convert geometry to map feature
        val routeFeature = Feature.fromGeometry(LineString.
            fromPolyline(route.geometry()!!, PRECISION_6))

        val destFeature = Feature.fromGeometry(
            model.destinations.value?.last()?.center()
        )
        // Must explicitly compare to true,
        // cuz isFullyLoaded might be null
        if (map.style?.isFullyLoaded == true) {

            // Change route data
            (map.style?.getSource(ROUTE_SOURCE_ID) as
                    GeoJsonSource).setGeoJson(routeFeature)

            // Change dest data
            (map.style?.getSource(DEST_SOURCE_ID)
                    as GeoJsonSource).setGeoJson(destFeature)


            // Create a LatLngBounds to be moved to
            val boundsBuilder = LatLngBounds.Builder()

            // Add current location to LatLng Bounds
            model.location.value?.let {
                boundsBuilder.include(LatLng(it.latitude, it.longitude))
            }

            // Add destination coordinates from recycler view adapter
            adapter.items.forEach {
                it.center()?.let {point -> boundsBuilder.
                    include(LatLng(point.latitude(), point.longitude())) }
            }

            map.moveCamera(CameraUpdateFactory.
                newLatLngBounds(boundsBuilder.build(), 500))
        }

    }

    /** Mapbox Matrix API callback method */
    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {

    }

    override fun onStart() {
        super.onStart()
        map_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (map_view != null)
            map_view.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        if (map_view != null)
            map_view.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map_view.onDestroy()
    }

    companion object {
        // Location update variables
        private const val DEFAULT_INTERVAL_MS = 2000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_MS * 5

        // Mapbox style source
        private const val ROUTE_LAYER_ID = "route_layer"
        private const val ROUTE_SOURCE_ID = "route_source"
        private const val DEST_ICON_ID = "dest_icon_id"
        private const val DEST_LAYER_ID = "dest_layer_id"
        private const val DEST_SOURCE_ID = "dest_source_id"

        // For intent stuff
        internal const val REQUEST_AUTOCOMPLETE = 420

        private fun getReadableTime(totalSeconds: Int): String {

            val minutesInHour = 60
            val secondsInMinute = 60

            val totalMinutes = totalSeconds / secondsInMinute
            val minutes = totalMinutes % minutesInHour
            val hours = totalMinutes / minutesInHour

            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
    }
}