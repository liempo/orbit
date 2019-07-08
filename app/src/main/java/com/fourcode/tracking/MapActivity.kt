package com.fourcode.tracking

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_map.*

import com.google.gson.Gson
import com.github.ajalt.timberkt.Timber
import com.fourcode.tracking.models.SocketLocationData

import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.*
import com.mapbox.mapboxsdk.location.modes.*
import com.mapbox.mapboxsdk.maps.*

import io.socket.client.IO
import io.socket.client.Socket


// Need to suppress this because Mapbox
@SuppressLint("MissingPermission")
class MapActivity : AppCompatActivity(),
    OnMapReadyCallback, PermissionsListener,
    LocationEngineCallback<LocationEngineResult> {

    // Location attributes
    private var isInitialLocationFound = false
    private lateinit var socket: Socket
    private lateinit var engine: LocationEngine
    private lateinit var permissions: PermissionsManager

    // Map attributes
    private lateinit var map: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token is configured here.
        Mapbox.getInstance(this, BuildConfig.MapboxApiKey)
        setContentView(R.layout.activity_map)


        // Initialize socket IO
        socket = IO.socket(getString(R.string.url_socket_to_broadcast_location)).apply {
            // Log connection is established
            on(Socket.EVENT_CONNECT) {
                Timber.i{ "Socket connected" }
            }
        }

        // Initialize mapView
        map_view.onCreate(savedInstanceState)
        map_view.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        map.setStyle(Style.TRAFFIC_DAY) {

            // Check if permissions are granted
            if (PermissionsManager.areLocationPermissionsGranted(this)) {
                enableLocationComponent(it)
                startLocationEngine()
            } else {
                permissions = PermissionsManager(this)
                permissions.requestLocationPermissions(this@MapActivity)
            }
        }
    }

    private fun startLocationEngine() {
        engine = LocationEngineProvider
            .getBestLocationEngine(this).also {
                it.requestLocationUpdates(

                    // Build a LocationEngineRequest
                    LocationEngineRequest.Builder(DEFAULT_INTERVAL_MS)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                        .build(),

                    // Set callback to this class and pass activity's main looper
                    this@MapActivity, mainLooper)
            }
        Timber.i {"Initialized LocationEngine" }

        // Connect socket once engine is initialized
        if (::socket.isInitialized) socket.connect()
    }

    private fun enableLocationComponent(style: Style) {
        // Enable location component on style loaded
        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(this, style)
                .useDefaultLocationEngine(false)
                .build()

        with(map.locationComponent) {
            activateLocationComponent(locationComponentActivationOptions)
            isLocationComponentEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        this.permissions.onRequestPermissionsResult(
            requestCode, permissions, grantResults)
    }

    /* PermissionListener methods */
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {}

    override fun onPermissionResult(granted: Boolean) {
        if (granted) startLocationEngine()
    }

    /* LocationEngineCallback methods */
    override fun onSuccess(result: LocationEngineResult?) {
        result?.lastLocation?.run {
            // Update location component
            map.locationComponent.apply {

                renderMode = RenderMode.GPS
                cameraMode = CameraMode.TRACKING_GPS

                zoomWhileTracking(15.0)
                tiltWhileTracking(45.0, 1000)

                forceLocationUpdate(this@run)
            }

            // Create socket data
            val data = SocketLocationData(latitude, longitude, speed)

            // Emit data if moving or if is initial location is not found
            if (CONTINUOUS_BROADCAST || data.speed > 0 || isInitialLocationFound.not()) {

                Timber.v { "Broadcasting location $data" }
                socket.emit("location", Gson().toJson(data))

                // set isInitialLocationFound to true so data will only emit if speed > 0
                isInitialLocationFound = true
            }
        }
    }

    override fun onFailure(exception: Exception) {
        Timber.w(exception) { "Failed retrieving location" }
    }

    override fun onStart() {
        super.onStart()
        map_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        map_view.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()

        // Disable location updates
        if (::engine.isInitialized)
            engine.removeLocationUpdates(this)

        // Disconnect socket if connected
        if (socket.connected()) socket.disconnect()
    }

    companion object {
        // Location update variables
        private const val DEFAULT_INTERVAL_MS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_MS * 5

        // For debugging purposes
        private const val CONTINUOUS_BROADCAST = false
    }
}
