package com.fourcode.tracking.models

import com.google.gson.annotations.SerializedName

data class SocketLocationData(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("speed") val speed: Float
) {

    override fun toString(): String {
        return "($latitude, $longitude), $speed m/s"
    }
}