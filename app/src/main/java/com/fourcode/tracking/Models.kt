package com.fourcode.tracking

import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.serialization.Serializable

object Models {

    @Serializable
    internal data class AuthData(
        val token: String
    )

    @Serializable
    internal data class LocationData(
        val lat: Double,
        val lng: Double,
        val speed: Int,
        var name: String = ""
    ) {

        fun toLatLng(): LatLng = LatLng(lat, lng)
        fun toPoint(): Point = Point.fromLngLat(lng, lat)
    }

    @Serializable
    internal data class NotificationData(
        val message: String
    )
}