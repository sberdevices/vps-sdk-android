package com.arvrlab.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RequestCompassModel(
    @Json(name = "accuracy")
    val accuracy: Float = 0.0f,
    @Json(name = "heading")
    val heading: Float = 0.0f,
    @Json(name = "timestamp")
    val timestamp: Double = 0.0
)
