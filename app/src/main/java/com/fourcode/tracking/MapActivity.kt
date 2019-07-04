package com.fourcode.tracking

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_map.*

import com.google.gson.Gson
import com.fourcode.tracking.models.SocketLocationData

import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

import io.socket.client.IO
import io.socket.client.Socket

import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

class MapActivity : AppCompatActivity(),
    AnkoLogger, PermissionsListener,
    LocationEngineCallback<LocationEngineResult> {

    // Location attributes
    private var isLocationFound = false
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

        // Initialize mapView
        map_view.onCreate(savedInstanceState)
        map_view.getMapAsync {
            map = it.apply { setStyle(Style.TRAFFIC_NIGHT) }
        }

        // Initialize socket IO
        initializeSocket()

        // Check if permissions are granted
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine()

        // else request for permissions
        } else permissions = PermissionsManager(this).also {
            it.requestLocationPermissions(this@MapActivity)
        }
    }

    private fun initializeSocket() {
        socket = IO.socket(SOCKET_URI).apply {

            // Log when connected
            on(Socket.EVENT_CONNECT) {
                info("Socket connected")
            }

            // Log messages received
            on(Socket.EVENT_MESSAGE) { results ->
                results.forEach {
                    info("Socket Message: $it")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {
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
        info("Initialized LocationEngine")

        // Connect socket once engine is initialized
        if (::socket.isInitialized) socket.connect()
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
        if (granted) initializeLocationEngine()
    }

    /* LocationEngineCallback methods */
    override fun onSuccess(result: LocationEngineResult?) {
        result?.lastLocation?.run {

            // Create socket data
            val data = SocketLocationData(latitude, longitude, speed)

            // Emit data if moving or if is initial location is not found
            if (data.speed > 0 || isLocationFound.not()) {

                info("Broadcasting location $data")
                socket.emit("location", Gson().toJson(data))

                // set isLocationFound to true so data will only emit if speed > 0
                isLocationFound = true
            }

        }
    }

    override fun onFailure(exception: Exception) {
        warn("Failed retrieving location", exception)
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
        private const val SOCKET_URI = "https://tracking-project.herokuapp.com"
        private const val DEFAULT_INTERVAL_MS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_MS * 5
    }
}
