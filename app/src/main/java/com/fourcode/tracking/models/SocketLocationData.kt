package com.fourcode.tracking.models

import com.google.gson.annotations.SerializedName

data class SocketLocationData(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
) {

    override fun toString(): String {
        return "($latitude, $longitude)"
    }
}