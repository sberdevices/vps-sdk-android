package com.arvrlab.vps_sdk.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ResponseCompassModel(
    @Json(name = "heading")
    val heading: Float? = null
)
