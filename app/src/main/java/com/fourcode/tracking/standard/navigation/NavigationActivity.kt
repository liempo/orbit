package com.fourcode.tracking.standard.navigation

import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.navArgs
import androidx.preference.PreferenceManager
import com.fourcode.tracking.R
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import kotlinx.android.synthetic.main.activity_navigation.*
import timber.log.Timber

class NavigationActivity :
    AppCompatActivity(),
    NavigationListener,
    ProgressChangeListener {

    private val args: NavigationActivityArgs by navArgs()

    // View model for this activity
    private lateinit var model: NavigationViewModel

    // Determines if location should still be broadcasted
    private var shouldBroadcastLocation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        // Initialize view model
        model = ViewModelProviders.of(this)
            .get(NavigationViewModel::class.java)

        // Get token and adminId
        val token = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getString(getString(R.string.shared_pref_token), null)
        model.initializeSocket(token!!)

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
                ReplayRouteLocationEngine().apply { assign(directions)
                    this.updateSpeed(100) })

            model.emitNotification("User has started navigation.")
            nav_view.startNavigation(builder.build())
        }

    }

    override fun onCancelNavigation() {
        shouldBroadcastLocation = false
        model.disconnectSocket()
        nav_view.stopNavigation()
        onBackPressed()
    }

    override fun onNavigationFinished() {
        shouldBroadcastLocation = false
        model.emitNotification("User has arrived.")
        model.disconnectSocket()

        Timber.d("Finished navigation")
        nav_view.stopNavigation()
        onBackPressed()
    }

    override fun onNavigationRunning() {
        nav_view.retrieveMapboxNavigation()?.let {
            it.addOffRouteListener {
                model.emitNotification("User has gone off-route.")
            }
        }
    }

    override fun onProgressChange(
        location: Location?,
        routeProgress: RouteProgress?
    ) {
        if (shouldBroadcastLocation)
            location?.let { model.emitLocation(it) }
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

}
