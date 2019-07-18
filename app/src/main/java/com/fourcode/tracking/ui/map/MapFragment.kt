package com.fourcode.tracking.ui.map

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.map_fragment.*
import timber.log.Timber
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

    /** Set to true if initiale location is found,
     * this is used for camera position and zooming */
    private var isInitialLocationFound = false

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

            // Update initial camera position and zooming
            if (isInitialLocationFound.not()) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(latitude, longitude), 12.0))
                isInitialLocationFound = true
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

    }

}
