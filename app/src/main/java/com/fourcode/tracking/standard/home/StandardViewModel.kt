package com.fourcode.tracking.standard.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.standard.Utils
import com.fourcode.tracking.standard.home.WaypointsAdapter.Waypoint
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class StandardViewModel : ViewModel(),
    LocationEngineCallback<LocationEngineResult> {

    internal val origin: MutableLiveData<Waypoint> = MutableLiveData()

    internal val route: MutableLiveData<DirectionsRoute> = MutableLiveData()

    internal fun getBestRoute(waypoints: List<Waypoint>) {
        viewModelScope.launch {

            val builder = MapboxDirections.builder()
                .accessToken(BuildConfig.MapboxApiKey)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .voiceInstructions(true)
                .bannerInstructions(true)
                // I don' know what the
                // fuck does this param do
                .steps(true)

            // Build a directions request object
            // Populate waypoint and destinations
            waypoints.forEachIndexed { index, item ->
                when (index) {
                    0 -> builder.origin(item.point)
                    waypoints.lastIndex -> builder.destination(item.point)
                    else -> builder.addWaypoint(item.point)
                }
            }

            route.value = withContext(Dispatchers.IO) {
                // first route is the best route
                val routes = builder.build().executeCall().body()?.routes()

                // return best route, if route is empty return null
                return@withContext if (routes.isNullOrEmpty().not()) routes!![0] else null
            }
        }
    }

    override fun onSuccess(result: LocationEngineResult?) {
        val location = result?.lastLocation

        // Check if location is now available else return
        // Let fragment handle it (ask for location again)
        if (location == null) {
            origin.value = null; return
        }

        // Create a point obj
        val point = Point.fromLngLat(
            location.longitude, location.latitude
        )

        // Launch a kotlin co-routine
        viewModelScope.launch {
            // Run suspend function
            val response = Utils.requestGeocode(point)

            // If response is valid (not empty)
            val name = response.features()[0].text()

            // Log if name is null (empty results from geocode)
            if (name == null) {
                Timber.w("Geocode results are empty.")
                return@launch
            }

            // Update origin live data
            origin.value = Waypoint(name, point)
        }
    }

    override fun onFailure(exception: Exception) {
        Timber.e(exception, "Error retrieving location")
    }
}

