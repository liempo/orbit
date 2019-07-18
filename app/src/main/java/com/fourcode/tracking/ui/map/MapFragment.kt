package com.fourcode.tracking.ui.map

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import kotlinx.android.synthetic.main.map_fragment.*
import timber.log.Timber
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import java.lang.Exception

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

    /** Current updated position will be saved here. */
    private lateinit var currentLocation: Location

    /** Intent used to search places (reverse geocoding). */
    private lateinit var autocomplete: Intent

    /** Autocomplete results as a CarmenFeature list (idk why the class name is like that)
     * WARNING: I GOT A FEELING THAT I MIGHT BE USING A
     * QUEUE DATA STRUCTURE HERE BUT STILL NOT SURE THO
     * NOTE: Always check if populated cuz it might break */
    private val destinations = arrayListOf<CarmenFeature>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mapbox access token is configured here, null check with context
        context?.let {  Mapbox.getInstance(it, BuildConfig.MapboxApiKey) }
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

        // Initially hide fab, it ain't ready yet
        add_destination_fab.hide()
        navigate_fab.hide()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == REQUEST_AUTOCOMPLETE) {
            // Extract results from intent
            val destination = PlaceAutocomplete.getPlace(data)
            destinations.add(destination)

            // Extract latLng from feature
            // TODO Update this to get distance route
            //  instead of point to point
            val destLatLng = LatLng(
                (destination.geometry() as Point).latitude(),
                (destination.geometry() as Point).longitude())
            val currentLatLng = LatLng(currentLocation)

            // Calculate distance between two latLng (in meters)
            val distanceInKilometers = currentLatLng.distanceTo(destLatLng) / 1000.0

            // Update the fucking bottom sheet
            destination_text.text = destination.text()
            distance_text.text = getString(R.string.
                format_distance_km, distanceInKilometers)
            bottom_sheet_header.visibility = View.VISIBLE
            with(navigate_fab) {
                // Show navigate button cuz
                // it's finally fucking usable as of nw
                show()

                // Check if destinations has one
                // (means first run) else run showcase
                if (destinations.size == 1)
                    MaterialShowcaseView.Builder(activity)
                        .setTarget(this)
                        .setContentText(R.string.msg_showcase_start_navigation)
                        .setDismissText(R.string.action_showcase_done)
                        .setDismissTextColor(
                            ContextCompat.getColor(
                                context, R.color.colorPrimary))
                        // Works with bottom sheet
                        .renderOverNavigationBar()
                        .show()
            }
        }
    }

    /** Enables location component, must be called after
     * style is loaded and permissions are granted */
    private fun initializeLocationComponent(style: Style) {
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

    /** Callback function for MapView.getAsync(). Runs after Mapview is initialized. */
    override fun onMapReady(mapboxMap: MapboxMap) {
        // Set a style then enable location component
        mapboxMap.apply {
            // Initialize map and set style
            map = this; setStyle(Style.TRAFFIC_DAY) {
                // Check if permissions are granted
                if (PermissionsManager.areLocationPermissionsGranted(context)) {
                    initializeLocationComponent(it)
                    initializeLocationEngine()
                } else {
                    permissions = PermissionsManager(this@MapFragment)
                    permissions.requestLocationPermissions(activity)
                }
            }
        }
    }

    /** Android Permission callback, will be run after user interacts with permission dialog. */
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

    /** LocationEngineCallbaack methods. Method name explains it. */
    override fun onSuccess(result: LocationEngineResult?) {
        // Update location component with new location
        result?.lastLocation?.run {
            map.locationComponent.forceLocationUpdate(this)

            // If current location is not initialize,
            // run this block for the first time
            if (::currentLocation.isInitialized.not()) {
                currentLocation = this

                // Update initial camera position and zooming
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(latitude, longitude), 12.0))

                // Create intent for autocomplete
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
                        .setDismissTextColor(ContextCompat.getColor(
                            context, R.color.colorPrimary))
                        // Works with bottom sheet
                        .renderOverNavigationBar()
                        .show()
                }

            }
        }
    }

    /** LocationEngineCallbaack methods. Method name explains it. */
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