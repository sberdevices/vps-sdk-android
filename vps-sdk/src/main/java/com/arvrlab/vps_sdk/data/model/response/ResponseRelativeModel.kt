package com.arvrlab.vps_sdk.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ResponseRelativeModel(
    @Json(name = "x")
    val x: Float? = null,
    @Json(name = "y")
    val y: Float? = null,
    @Json(name = "z")
    val z: Float? = null,
    @Json(name = "pitch")
    val pitch: Float? = null,
    @Json(name = "roll")
    val roll: Float? = null,
    @Json(name = "yaw")
    val yaw: Float? = null
)
