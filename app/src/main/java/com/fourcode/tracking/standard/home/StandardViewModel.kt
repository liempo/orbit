package com.fourcode.tracking.standard.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import timber.log.Timber
import java.lang.Exception

import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.standard.home.WaypointsAdapter.Waypoint
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsRoute

class StandardViewModel: ViewModel(),
    LocationEngineCallback<LocationEngineResult> {

    internal val origin: MutableLiveData<Waypoint> = MutableLiveData()

    internal val route: MutableLiveData<DirectionsRoute> = MutableLiveData()

    private suspend fun runReverseGeocode(point: Point): GeocodingResponse =
        withContext(Dispatchers.IO) {
            MapboxGeocoding.builder()
                .accessToken(BuildConfig.MapboxApiKey)
                .geocodingTypes(GeocodingCriteria.TYPE_LOCALITY)
                .query(point)
                .build().executeCall().body()!!
        }

    internal fun getBestRoute(waypoints: List<Waypoint>) {
        viewModelScope.launch {

            val builder = MapboxDirections.builder()
                .accessToken(BuildConfig.MapboxApiKey)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
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
                 return@withContext  if (routes.isNullOrEmpty().not()) routes!![0] else null
            }
        }
    }

    override fun onSuccess(result: LocationEngineResult?) {
        // Launch a kotlin co-routine
        viewModelScope.launch {
            val location = result?.lastLocation ?: return@launch

            // Create a point obj
            val point = Point.fromLngLat(
                location.longitude, location.latitude)

            // Run suspend function
            val response = runReverseGeocode(point)

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

