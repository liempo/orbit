package com.fourcode.tracking.ui.navigation


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

import com.fourcode.tracking.R
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener


import kotlinx.android.synthetic.main.fragment_navigation.*
import timber.log.Timber

class NavigationFragment : Fragment(), NavigationListener {

    private val args: NavigationFragmentArgs by navArgs()

    // Location request, for the engine parameters
    private val locationEngineRequest: LocationEngineRequest by lazy {
        LocationEngineRequest.Builder(NAV_INTERVAL_MS)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(NAV_MAX_WAIT_TIME)
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_navigation,
        container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        nav_view.onCreate(savedInstanceState)
        nav_view.initialize {

            // Start navigation when nav_view is initialized
            val options = NavigationViewOptions.builder()
                .directionsRoute(DirectionsRoute.fromJson(args.routeJson))
                .navigationListener(this)
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
        // Broadcast if location is acquired
        nav_view.retrieveMapboxNavigation()?.addRawLocationListener {
            Timber.d("NavLocation: (${it.latitude}, ${it.longitude})")
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

    companion object {
        // constants for the LocationEngineResult
        internal const val NAV_INTERVAL_MS = 5000L
        internal const val NAV_MAX_WAIT_TIME = NAV_INTERVAL_MS * 5
    }
}
