package com.arvrlab.vps_sdk.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResponseDto(
    @Json(name = "data")
    val responseData: ResponseDataDto? = null
)

@JsonClass(generateAdapter = true)
data class ResponseDataDto(
    @Json(name = "attributes")
    val responseAttributes: ResponseAttributesDto? = null,
    @Json(name = "id")
    val id: String? = null,
    @Json(name = "type")
    val type: String = "job"
)

@JsonClass(generateAdapter = true)
data class ResponseAttributesDto(
    @Json(name = "location")
    val responseLocation: ResponseLocationDto? = null,
    @Json(name = "status")
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponseLocationDto(
    @Json(name = "clientCoordinateSystem")
    val clientCoordinateSystem: String? = null,
    @Json(name = "compass")
    val responseCompass: ResponseCompassDto? = null,
    @Json(name = "gps")
    val gps: GpsDto? = null,
    @Json(name = "location_id")
    val locationId: String? = null,
    @Json(name = "relative")
    val responseRelative: ResponseRelativeDto? = null,
    @Json(name = "type")
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class GpsDto(
    @Json(name = "altitude")
    val altitude: Float? = null,
    @Json(name = "latitude")
    val latitude: Float? = null,
    @Json(name = "longitude")
    val longitude: Float? = null
)

@JsonClass(generateAdapter = true)
data class ResponseRelativeDto(
    @Json(name = "pitch")
    val pitch: Float? = null,
    @Json(name = "roll")
    val roll: Float? = null,
    @Json(name = "x")
    val x: Float? = null,
    @Json(name = "y")
    val y: Float? = null,
    @Json(name = "yaw")
    val yaw: Float? = null,
    @Json(name = "z")
    val z: Float? = null
)

@JsonClass(generateAdapter = true)
data class ResponseCompassDto(
    @Json(name = "heading")
    val heading: Float? = null
)