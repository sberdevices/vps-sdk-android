package com.arvrlab.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RequestLocalPosModel(
    @Json(name = "x")
    val x: Float = 0f,
    @Json(name = "y")
    val y: Float = 0f,
    @Json(name = "z")
    val z: Float = 0f,
    @Json(name = "pitch")
    val pitch: Float = 0f,
    @Json(name = "roll")
    val roll: Float = 0f,
    @Json(name = "yaw")
    val yaw: Float = 0f
)
