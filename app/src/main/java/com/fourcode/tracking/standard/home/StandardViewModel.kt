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

class StandardViewModel: ViewModel(),
    LocationEngineCallback<LocationEngineResult> {

    internal val origin: MutableLiveData<Waypoint> = MutableLiveData()

    private suspend fun reverseGeocode(point: Point): GeocodingResponse =
        withContext(Dispatchers.IO) {
            MapboxGeocoding.builder()
                .accessToken(BuildConfig.MapboxApiKey)
                .geocodingTypes(GeocodingCriteria.TYPE_LOCALITY)
                .query(point)
                .build().executeCall().body()!!
        }

    override fun onSuccess(result: LocationEngineResult?) {
        // Launch a kotlin co-routine
        viewModelScope.launch {
            val location = result?.lastLocation ?: return@launch

            // Create a point obj
            val point = Point.fromLngLat(
                location.longitude, location.latitude)

            // Run suspend function
            val response = reverseGeocode(point)

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

