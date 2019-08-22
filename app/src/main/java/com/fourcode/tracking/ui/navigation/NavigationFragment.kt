package com.fourcode.tracking.ui.navigation

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

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

import kotlinx.android.synthetic.main.navigation_fragment.*

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class NavigationFragment : Fragment(),
    OnNavigationReadyCallback, NavigationListener,
    Callback<DirectionsResponse>, ProgressChangeListener {

    private val args: NavigationFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.navigation_fragment,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigation_view.onCreate(savedInstanceState)
        navigation_view.initialize(this)
    }

    override fun onNavigationReady(isRunning: Boolean) {
        val builder = NavigationRoute.builder(context)
            .accessToken(BuildConfig.MapboxApiKey)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)

        Timber.d("Waypoint size: %s", args.waypoints.size)

        args.waypoints.forEachIndexed { i, latLng ->

            val point = Point.fromLngLat(
                latLng.longitude, latLng.latitude)

            when (i) {
                0 -> builder.origin(point)
                args.waypoints.lastIndex ->
                    builder.destination(point)
                else -> builder.addWaypoint(point)
            }
        }

        builder.build().getRoute(this)
    }

    override fun onCancelNavigation() {
        navigation_view.stopNavigation()
        findNavController().popBackStack()
    }

    override fun onNavigationFinished() {
        // no-op
    }

    override fun onNavigationRunning() {
        // no-op
    }

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress?) {

    }

    override fun onResponse(
        call: Call<DirectionsResponse>,
        response: Response<DirectionsResponse>
    ) {
        val route = response.body()!!.routes()[0]

        val options = NavigationViewOptions.builder()
            .directionsRoute(route)
            .shouldSimulateRoute(true)
            .navigationListener(this)
            .progressChangeListener(this)
            .build()

        navigation_view.startNavigation(options)
    }

    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
        Timber.e(t)
    }

    override fun onStart() {
        super.onStart()
        navigation_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        navigation_view.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigation_view.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            navigation_view.onRestoreInstanceState(it)
        }
    }

    override fun onPause() {
        super.onPause()
        navigation_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        navigation_view.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigation_view.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigation_view.onDestroy()
    }
}
