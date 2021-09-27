package com.arvrlab.vps_sdk.domain.model

import android.location.Location

internal data class GpsLocationModel(
    val accuracy: Double,
    val altitude: Double,
    val latitude: Double,
    val longitude: Double,
    val elapsedRealtimeNanos: Double
) {
    companion object {
        fun from(location: Location): GpsLocationModel =
            GpsLocationModel(
                accuracy = location.accuracy.toDouble(),
                altitude = location.altitude,
                latitude = location.latitude,
                longitude = location.longitude,
                elapsedRealtimeNanos = location.elapsedRealtimeNanos.toDouble()
            )
    }
}
