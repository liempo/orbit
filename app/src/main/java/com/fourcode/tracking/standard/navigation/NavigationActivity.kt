package com.fourcode.tracking.standard.navigation

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.navArgs
import androidx.preference.PreferenceManager
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.R
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import timber.log.Timber

class NavigationActivity :
    AppCompatActivity(),
    NavigationListener,
    ProgressChangeListener {

    private val args: NavigationActivityArgs by navArgs()

    // Authentication token (retrieved in auth frag)
    private var token: String? = null
    private lateinit var socket: Socket
    private lateinit var json: Json

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        // Get token and adminId
        token = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getString(getString(R.string.shared_pref_token), null)

        // Json serializer (ktx)
        json = Json(JsonConfiguration.Stable)

        // Initialize socket object
        socket = IO.socket(BuildConfig.SocketUrl, IO.Options().apply {
            transports = arrayOf("websocket")
        })

        socket.on(Socket.EVENT_CONNECT) {
            Timber.d("Socket is connected. Authenticating.")

            // Once connected, authenticate
            socket.emit(CHANNEL_AUTH, json.toJson(
                AuthData.serializer(), AuthData(token!!)
            ))
        }

        // Initialize navigation view
        nav_view.onCreate(savedInstanceState)
        nav_view.initialize {
            // Parse json to object (DirectionsRoute)
            val directions = DirectionsRoute.fromJson(args.routeJson)

            // Configure options
            val builder = NavigationViewOptions.builder()
                .directionsRoute(directions)
                .navigationListener(this)
                .progressChangeListener(this)

            val simulate = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("pref_should_simulate", false)

            if (simulate) builder.locationEngine(
                ReplayRouteLocationEngine().apply { assign(directions) })

            socket.connect()
            nav_view.startNavigation(builder.build())
        }

    }

    override fun onCancelNavigation() {
        // TODO Send notification that
        //  user has cancelled the navigation
        socket.disconnect()

        nav_view.stopNavigation()
        onBackPressed()
    }

    override fun onNavigationFinished() {
        // TODO Send notification that user
        //  has finished navigation
        socket.disconnect()

        nav_view.stopNavigation()
        onBackPressed()
    }

    override fun onNavigationRunning() {
        nav_view.retrieveMapboxNavigation()?.let {
            it.addOffRouteListener {
                // TODO Send notification that
                //  user has gone off-route
            }
        }
    }

    override fun onProgressChange(
        location: Location?,
        routeProgress: RouteProgress?
    ) {
        location?.let {
            val data = LocationData(
                it.latitude,
                it.longitude,
                routeProgress!!.currentState()!!.name,
                it.speed.toInt()
            )

            socket.emit(CHANNEL_STATUS, json.toJson(
                LocationData.serializer(), data))
        }
    }

    override fun onStart() {
        super.onStart()
        nav_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        nav_view.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        nav_view.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        nav_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        nav_view.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        nav_view.onLowMemory()
    }

    @Serializable
    private data class AuthData(
        val token: String
    )

    @Serializable
    private data class LocationData(
        val lat: Double,
        val lng: Double,
        val location: String,
        val speed: Int
    )

    companion object {
        private const val CHANNEL_AUTH = "auth"
        private const val CHANNEL_STATUS = "status"
    }
}
