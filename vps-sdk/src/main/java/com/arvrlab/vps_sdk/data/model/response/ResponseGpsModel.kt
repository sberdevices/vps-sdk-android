package com.arvrlab.vps_sdk.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ResponseGpsModel(
    @Json(name = "altitude")
    val altitude: Float? = null,
    @Json(name = "latitude")
    val latitude: Float? = null,
    @Json(name = "longitude")
    val longitude: Float? = null
)
