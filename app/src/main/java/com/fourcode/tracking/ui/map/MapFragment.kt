package com.fourcode.tracking.ui.map


import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager

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

import kotlinx.android.synthetic.main.map_fragment.*

import timber.log.Timber
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView

@SuppressLint("MissingPermission")
class MapFragment : Fragment(),
    OnMapReadyCallback, PermissionsListener,
    LocationEngineCallback<LocationEngineResult> {

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
    private val adapter = DestinationsAdapter(arrayListOf())

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

        model.destination.observe(this, Observer {

            // Add item to adapters and update UI
            adapter.items.add(it)
            adapter.notifyDataSetChanged()

            // Will run once items has been populated
            if (adapter.items.size == 1) {
                navigate_fab.show()
                destinations_title.setText(R.string.title_destinations)
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
            adapter = this@MapFragment.adapter
        }

        // Initially hide fab, it ain't ready yet
        add_destination_fab.hide()
        navigate_fab.hide()
    }

    /** Will run when a place got picked from autocomplete*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_AUTOCOMPLETE) {
           model.destination.value = PlaceAutocomplete.getPlace(data)
        }
    }

    /** Callback function for MapView.getAsync().
     * Runs after Mapview is initialized. */
    override fun onMapReady(mapboxMap: MapboxMap) {
        // Set a style then enable location component
        mapboxMap.apply {
            // Initialize map and set style
                map = this; setStyle(Style.TRAFFIC_DAY) {
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
    }

    /** Enables location component, must be called after
     * style is loaded and permissions are granted */
    private fun initializeMapComponents(style: Style) {
        context?.let {context ->
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

    /** LocationEngineCallbaack method. Method name explains it. */
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
                            getColor(context, R.color.colorPrimary))
                        // Works with bottom sheet
                        .renderOverNavigationBar()
                        .show()
                }
            }
        }
    }

    /** LocationEngineCallbaack method. Method name explains it. */
    override fun onFailure(exception: Exception) {
        Timber.w(exception,"Failed retrieving location")
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

        // For intent stuff
        internal const val REQUEST_AUTOCOMPLETE = 420
    }
}