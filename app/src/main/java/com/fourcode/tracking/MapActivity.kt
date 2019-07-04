package com.fourcode.tracking

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fourcode.tracking.models.SocketLocationData
import com.google.gson.Gson
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import io.socket.client.IO
import io.socket.client.Socket
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn
import java.lang.Exception

class MapActivity : AppCompatActivity(),
    AnkoLogger, PermissionsListener,
    LocationEngineCallback<LocationEngineResult> {

    private lateinit var socket: Socket
    private lateinit var engine: LocationEngine
    private lateinit var permissions: PermissionsManager

    override fun onCreate(savedInstanceState: Bundle?) {

        // Mapbox access token is configured here.
        Mapbox.getInstance(this, BuildConfig.MapboxApiKey)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

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
                    info("Socket: $it")
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

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /* PermissionListener methods */
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {

    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) initializeLocationEngine()

    }

    /* LocationEngineCallback methods */
    override fun onSuccess(result: LocationEngineResult?) {
        result?.lastLocation?.run {
            val data = SocketLocationData(latitude, longitude)
            socket.emit("location", Gson().toJson(data))

            info("Broadcasting location $data")
        }
    }

    override fun onFailure(exception: Exception) {
        warn("Failed retrieving location", exception)
    }

    override fun onDestroy() {
        super.onDestroy()

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
