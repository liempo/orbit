package com.fourcode.tracking.ui.navigation


import android.content.Context.MODE_PRIVATE
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fourcode.tracking.BuildConfig

import com.fourcode.tracking.R
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import io.github.centrifugal.centrifuge.*

import kotlinx.android.synthetic.main.fragment_navigation.*
import timber.log.Timber

class NavigationFragment : Fragment(),
    NavigationListener,
    ProgressChangeListener,
    MilestoneEventListener,
    ReplyCallback<PublishResult> {

    private val args: NavigationFragmentArgs by navArgs()

    // Centrifuge client to broadcast device's location
    private val client = Client(BuildConfig.CentrifugeUrl,
        Options(), CentrifugeListener())

    // Authentication token (retrieved in auth frag)
    private var token: String? = null
    private var adminId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with (requireActivity().getSharedPreferences(getString(
            R.string.shared_pref_credentials), MODE_PRIVATE)) {

            token = getString(getString(R.string.shared_pref_token), null)
            adminId = getString(getString(R.string.shared_pref_admin_id), null)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_navigation,
        container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Connect to socket
        client.setToken(token)
        client.connect()

        // Init and star navigation
        nav_view.onCreate(savedInstanceState)
        nav_view.initialize {

            // Start navigation when nav_view is initialized
            val options = NavigationViewOptions.builder()
                .directionsRoute(DirectionsRoute.fromJson(args.routeJson))
                .navigationListener(this)
                .milestoneEventListener(this)
                .progressChangeListener(this)
                .build()
            nav_view.startNavigation(options)

        }
    }

    override fun onCancelNavigation() {
        nav_view.stopNavigation()
        findNavController().popBackStack()
    }

    override fun onNavigationFinished() {
        nav_view.stopNavigation()
        findNavController().popBackStack()
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

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {
        location?.let {
            val dataToSend = getString(R.string.
                format_location_centrifuge,
                it.latitude, it.longitude)
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

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            nav_view.onRestoreInstanceState(it)
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        nav_view.onDestroy()
    }

}
