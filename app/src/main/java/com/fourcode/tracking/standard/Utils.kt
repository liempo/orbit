package com.fourcode.tracking.standard

import com.fourcode.tracking.BuildConfig
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object Utils {

    internal suspend fun requestGeocode(point: Point): GeocodingResponse =
        withContext(Dispatchers.IO) {
            MapboxGeocoding.builder()
                .accessToken(BuildConfig.MapboxApiKey)
                .geocodingTypes(GeocodingCriteria.TYPE_LOCALITY)
                .query(point)
                .build().executeCall().body()!!
        }

    internal fun shouldRequestGeocode(previous: LatLng, current: LatLng): Boolean {
        val distance = previous.distanceTo(current)
        Timber.d(distance.toString())
        return distance > 500
    }

}