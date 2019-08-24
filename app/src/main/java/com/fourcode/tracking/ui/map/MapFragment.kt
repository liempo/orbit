package com.fourcode.tracking.ui.map

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.fourcode.tracking.BuildConfig

import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.*
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.modes.*
import com.mapbox.mapboxsdk.maps.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

// Location Engine is too long for me i'm sorry guys
import com.mapbox.android.core.location.LocationEngineCallback as EngineCallback
import com.mapbox.android.core.location.LocationEngineResult as EngineResult
import com.mapbox.android.core.location.LocationEngineRequest as EngineRequest

import com.fourcode.tracking.ui.map.MapViewModel.Companion.ROUTE_SOURCE_ID
import com.fourcode.tracking.ui.map.MapViewModel.Companion.ROUTE_LAYER_ID
import com.fourcode.tracking.ui.map.MapViewModel.Companion.DEST_SOURCE_ID
import com.fourcode.tracking.ui.map.MapViewModel.Companion.DEST_LAYER_ID
import com.fourcode.tracking.ui.map.MapViewModel.Companion.DEST_ICON_ID
import com.fourcode.tracking.ui.map.MapViewModel.Companion.DEFAULT_INTERVAL_MS
import com.fourcode.tracking.ui.map.MapViewModel.Companion.DEFAULT_MAX_WAIT_TIME
import com.fourcode.tracking.R
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString

import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions

import kotlinx.android.synthetic.main.map_fragment.*
import kotlinx.coroutines.*
import timber.log.Timber

import java.lang.Exception
import kotlin.coroutines.CoroutineContext

class MapFragment : Fragment(),
    CoroutineScope,
    EngineCallback<EngineResult>,
    PermissionsListener {

    // Implementation of CorutineScope interface
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    // LiveData object (Android jetpack)
    private lateinit var model: MapViewModel

    // Adapter destinations_recycler_view
    private lateinit var adapter: DestinationAdapter

    // Main Mapbox Object
    private lateinit var map: MapboxMap

    // Will make things less complicated. /
    private lateinit var permissions: PermissionsManager

    // AUtocomplete Intent for opening mapbox's place picker
    private lateinit var autocompleteIntent: Intent

    // Location engine, retrieves device's location
    private lateinit var locationEngine: LocationEngine

    // Location request, for the engine parameters
    private val locationEngineRequest: EngineRequest by lazy {
        EngineRequest.Builder(DEFAULT_INTERVAL_MS)
            .setPriority(EngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token is configured here. This needs to be called either
        // in your application object or in the same activity which contains the map_view.
        context?.let { Mapbox.getInstance(it, BuildConfig.MapboxApiKey) }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Initialize model and its observers
        model = ViewModelProviders.of(this)
            .get(MapViewModel::class.java)
        model.destinations.observe(this, Observer {

            // Will run once
            if (it.size == 1) {
                bottom_sheet_title.visibility = View.GONE
                bottom_sheet_header.visibility = View.VISIBLE
            }
        })
        model.location.observe(this, Observer {
            map.locationComponent.forceLocationUpdate(it)
        })
        model.destinations.observe(this, Observer {

            if (model.location.value == null)
                return@Observer

            // Convert location object to point (downgrade)
            val originPoint = Point.fromLngLat(
                model.location.value!!.longitude,
                model.location.value!!.latitude
            )

            // Convert list of features to list of points (downgrade)
            val destinationPoints = it.map { d -> d.center()!! }

            launch {
                // Run call
                val route = getBestRoute(
                    originPoint, destinationPoints)
                // Update model value
                model.route.value = route
            }
        })
        model.route.observe(this, Observer {

            // Draw a polyline from route on map
            // Must explicitly compare to true,
            // cuz isFullyLoaded might be null
            if (map.style?.isFullyLoaded == true) {

                // Extract elements from API call and map's style
                val source = (map.style?.getSource(
                    ROUTE_SOURCE_ID) as GeoJsonSource)
                val line = LineString.fromPolyline(
                    it.geometry()!!, Constants.PRECISION_6)
                val geoJson = FeatureCollection.
                    fromFeature(Feature.fromGeometry(line))

                // Draw route on map
                source.apply {
                    setGeoJson(geoJson)
                }
            }

            // Update distance if it.distance() does not return null
            it.distance()?.let { distance ->
                total_distance_text.text =
                    if (distance >= 1000)
                        getString(R.string.format_distance_kilometers, distance / 1000)
                    else getString(R.string.format_distance_meters, distance)
            }

            // Update duration if it.duration() does not retunrn null
            it.duration()?.let { duration ->
                total_duration_text.text = getReadableTime(duration.toInt())
            }
        })

        // Initialize and configure map_view
        map_view.onCreate(savedInstanceState)

        // Get MapboxMap object
        map_view.getMapAsync {

            // Use class attribute from now in
            map = it

            // Initialize a style then run callback
            map.setStyle(Style.TRAFFIC_DAY) { style ->

                // Initialize location layer, and polylines
                initializeMapComponents(style)

                // Check permission before enabling stuff
                if (PermissionsManager.areLocationPermissionsGranted(context))
                    initializeLocationEngine()
                else {
                    permissions = PermissionsManager(this@MapFragment)
                    permissions.requestLocationPermissions(requireActivity())
                }
            }
        }

        // Setup some pretty simple stuff
        add_destination_fab.setOnClickListener { startAutocomplete() }

        // Intiialize RecyclerView
        with(destinations_recycler_view) {
            layoutManager = LinearLayoutManager(context)

            this@MapFragment.adapter = DestinationAdapter()
            adapter = this@MapFragment.adapter

            ItemTouchHelper(DestinationItemTouchHelperCallback(model))
                .attachToRecyclerView(destinations_recycler_view)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.map_fragment,
        container, false)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == AUTOCOMPLETE_REQUEST)
            with (PlaceAutocomplete.getPlace(data)) {
                model.destinations.add(this)
                adapter.add(this)
            }
    }

    private fun <T> MutableLiveData<ArrayList<T>>.add(item: T) {
        // Crate an new list if list live data is null
        val newList =
            if (value == null)
                arrayListOf()
            else value!!
        newList.add(item)
        // Change value (will notify observers)
        this.value = newList
    }

    private fun initializeMapComponents(style: Style) {
        context?.let {context ->

            // Initialize location component
            with(map.locationComponent) {
                // Activate location component with the following options
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style)
                        .useDefaultLocationEngine(false)
                        .build())

                // Set component modes and enable
                renderMode = RenderMode.NORMAL
                cameraMode = CameraMode.TRACKING
                isLocationComponentEnabled = true
            }

            // Initialize route source
            style.addSource(
                GeoJsonSource(
                    ROUTE_SOURCE_ID,
                    FeatureCollection.fromFeatures(arrayOf())
                )
            )

            // Initialize route source
            style.addSource(
                GeoJsonSource(
                    DEST_SOURCE_ID,
                    FeatureCollection.fromFeatures(arrayOf())
                )
            )

            // Add route layer to style
            style.addLayer(
                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND),
                    lineWidth(5f),
                    lineColor(Color.parseColor("#009688"))
                )
            )

            // Add icon to style
            style.addImage(
                DEST_ICON_ID, resources.getDrawable(
                    R.drawable.ic_place_primary_24dp, activity?.theme
                )
            )

            // Add symbol layer for the dest icon
            style.addLayer(
                SymbolLayer(DEST_LAYER_ID, DEST_SOURCE_ID).withProperties(
                    iconImage(DEST_ICON_ID),
                    iconIgnorePlacement(true),
                    iconIgnorePlacement(true),
                    iconOffset(arrayOf(0f, -4f))
                )
            )
        }
    }

    private fun initializeLocationEngine() {
        context?.let { context ->
            locationEngine = LocationEngineProvider
                .getBestLocationEngine(context)
            locationEngine.requestLocationUpdates(
                locationEngineRequest,
                this, context.mainLooper)
        }
    }

    private fun startAutocomplete(point: Point? = null) {
        // Initialize autocomplete if it ain't (if it ain't, if it ain't)
        if (::autocompleteIntent.isInitialized.not()) {
            // Build intent for autocomplete
            val builder = PlaceOptions.builder()
                .backgroundColor(ContextCompat.getColor(context!!,
                    R.color.colorAutocompleteBackground))
            if (point != null) builder.proximity(point)

            autocompleteIntent = PlaceAutocomplete.IntentBuilder()
                .accessToken(BuildConfig.MapboxApiKey)
                .placeOptions(builder.build())
                .build(requireActivity())
        }

        startActivityForResult(autocompleteIntent, AUTOCOMPLETE_REQUEST)
    }

    private suspend fun getBestRoute(
        origin: Point,
        destinations: List<Point>):
            DirectionsRoute {

        // Build a directions request object
        val builder = MapboxDirections.builder()
            .accessToken(BuildConfig.MapboxApiKey)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .origin(Point.fromLngLat(origin.longitude(), origin.latitude()))
        // Populate waypoint and destinations
        destinations.forEachIndexed { index, item ->
            if (destinations.lastIndex == index)
                builder.destination(item)
            else builder.addWaypoint(item)
        }

        // Execute call and use directionsCallback
        return withContext(Dispatchers.IO) {
            return@withContext builder.build()
                .executeCall().body()!!.routes()[0]
        } // Coroutines are a whole new level
    }

    /************ Mapbox permission listeners methods  ************/
    override fun onPermissionResult(granted: Boolean) {
        initializeLocationEngine()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {}

    /************ Mapbox location engine callback methods  ************/
    override fun onSuccess(result: EngineResult?) {
        result?.lastLocation?.run {

            // Run on first detected location
            if (model.location.value == null) {
                // Move camera to center
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(latitude, longitude),  MAP_ZOOM_DEFAULT))
            }

            model.location.value = this
        }
    }

    override fun onFailure(exception: Exception) {
        Timber.e(exception)
    }

    /************* Mapbox lifecycle boiler plate alert!!!! **********/
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

    override fun onStop() {
        super.onStop()
        map_view.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    companion object {
        // Mapbox constants
        internal const val MAP_ZOOM_DEFAULT = 15.0
        internal const val AUTOCOMPLETE_REQUEST = 12494

        // Formats totalSeconds to Hh Mm Ss
        private fun getReadableTime(totalSeconds: Int): String {
            val minutesInHour = 60
            val secondsInMinute = 6
            val totalMinutes = totalSeconds / secondsInMinute
            val minutes = totalMinutes % minutesInHour
            val hours = totalMinutes / minutesInHour
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
    }

}
