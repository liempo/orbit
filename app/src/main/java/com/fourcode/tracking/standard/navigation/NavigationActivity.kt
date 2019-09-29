package com.fourcode.tracking.standard.navigation

import android.content.Context
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
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import io.github.centrifugal.centrifuge.*
import kotlinx.android.synthetic.main.activity_navigation.*
import timber.log.Timber

class NavigationActivity : AppCompatActivity(),
    NavigationListener,
    ProgressChangeListener,
    MilestoneEventListener,
    ReplyCallback<PublishResult> {

    private val args: NavigationActivityArgs by navArgs()

    // Centrifuge client to broadcast device's location
    private val client: Client by lazy {
        Client(BuildConfig.CentrifugeUrl, Options(), CentrifugeListener())
    }

    // Authentication token (retrieved in auth frag)
    private var token: String? = null
    private var adminId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        // Get token and adminId
        with (getSharedPreferences(getString(
            R.string.shared_pref_credentials),
            Context.MODE_PRIVATE)) {

            token = getString(getString(
                R.string.shared_pref_token), null)
            adminId = getString(getString(
                R.string.shared_pref_admin_id), null)
        }

        // Connect to centrifugal socket
        client.setToken(token)
        client.connect()

        // Initialize navigation view
        nav_view.onCreate(savedInstanceState)
        nav_view.initialize {
            // Parse json to object (DirectionsRoute)
            val directions = DirectionsRoute.fromJson(args.routeJson)

            // Configure options
            val builder = NavigationViewOptions.builder()
                .directionsRoute(directions)
                .navigationListener(this)
                .milestoneEventListener(this)
                .progressChangeListener(this)

            val simulate = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("pref_should_simulate", false)

            if (simulate) builder.locationEngine(
                ReplayRouteLocationEngine().apply { assign(directions) })

            nav_view.startNavigation(builder.build())
        }

    }

    override fun onCancelNavigation() {
        nav_view.stopNavigation()
        onBackPressed()
    }

    override fun onNavigationFinished() {
        nav_view.stopNavigation()
        onBackPressed()
    }

    override fun onNavigationRunning() {
        nav_view.retrieveMapboxNavigation()?.let {
            it.addOffRouteListener {
                val notif = getString(R.string.format_notification_centrifuge,
                    getString(R.string.msg_notification_off_route))
                client.publish("notification:$adminId",
                    notif.toByteArray(), this)
            }
        }
    }

    override fun onProgressChange(
        location: Location?,
        routeProgress: RouteProgress?) {
        location?.let {
            val dataToSend = getString(R.string.
                format_location_centrifuge,
                it.latitude, it.longitude, it.speed)
            client.publish("location:$adminId",
                dataToSend.toByteArray(), this)
        }
    }

    override fun onMilestoneEvent(
        routeProgress: RouteProgress?,
        instruction: String?,
        milestone: Milestone?
    ) {
        if (instruction.isNullOrEmpty().not())
            client.publish("notification:$adminId",
                getString(R.string.format_notification_centrifuge,
                    instruction).toByteArray(), this)
    }

    override fun onDone(error: ReplyError?, result: PublishResult?) {
        if (error != null) Timber.e(error.message)
        else Timber.d("Published new data to centrifugal server")
    }

    override fun onFailure(e: Throwable?) { Timber.e(e) }

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
