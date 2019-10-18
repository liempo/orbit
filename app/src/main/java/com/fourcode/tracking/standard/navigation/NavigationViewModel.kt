package com.fourcode.tracking.standard.navigation

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourcode.tracking.BuildConfig
import com.fourcode.tracking.standard.Utils
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.json.JSONObject
import timber.log.Timber

class NavigationViewModel: ViewModel() {

    /** Last geocoded location and name*/
    private lateinit var geocodedLocationData: LocationData

    /** Socket object for broadcasting location and notifications */
    private lateinit var socket: Socket

    /** json config object for kotlinx serialization*/
    private val json = Json(JsonConfiguration.Stable)

    internal fun initializeSocket(token: String) {
        // Initialize socket object
        socket = IO.socket(BuildConfig.SocketUrl, IO.Options().apply {
            transports = arrayOf("websocket")
        })

        socket.on(Socket.EVENT_CONNECT) {
            val msg = json.toJson(
                AuthData.serializer(),
                AuthData(token)
            ).toString()

            // Once connected, authenticate
            socket.emit(CHANNEL_AUTH, JSONObject(msg))
        }

        socket.connect()
    }

    internal fun disconnectSocket() = socket.disconnect()

    internal fun emitLocation(location: Location) {
        val data = LocationData(
            location.latitude,
            location.longitude,
            location.speed.toInt()
        )

        viewModelScope.launch {
            if (::geocodedLocationData.isInitialized) {

                val shouldGeocode = Utils.shouldRequestGeocode(
                    geocodedLocationData.toLatLng(), data.toLatLng())

                if (shouldGeocode) {
                    data.name = Utils.requestGeocode(data.toPoint())
                        .features()[0].text()!!
                    geocodedLocationData = data
                } else data.name = geocodedLocationData.name

            } else geocodedLocationData = LocationData(
                location.latitude,
                location.longitude,
                location.speed.toInt(),
                name =  Utils.requestGeocode(
                    data.toPoint()).features()[0].text()!!
            )

            // Convert geocodedLocationData to string
            val serialized = json.toJson(
                LocationData.serializer(), data).toString()
            Timber.d("LocationData: $serialized")

            // Broadcast to socket
            socket.emit(CHANNEL_STATUS, serialized)
        }
    }

    internal fun emitNotification(message: String) {
        // Process data
        val data = NotificationData(message)
        val serialized = json.toJson(NotificationData.
            serializer(), data).toString()
        Timber.d("NotificationData: $serialized")

        // Broadcast to socket
        socket.emit(CHANNEL_NOTIFICATION, serialized)
    }

    @Serializable
    internal data class AuthData(
        val token: String
    )

    @Suppress("unused")
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

    companion object {
        private const val CHANNEL_AUTH = "auth"
        private const val CHANNEL_STATUS = "status"
        private const val CHANNEL_NOTIFICATION = "notification"
    }

}