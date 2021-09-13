package com.arvrlab.vps_sdk.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ResponseLocationModel(
    @Json(name = "clientCoordinateSystem")
    val clientCoordinateSystem: String? = null,
    @Json(name = "compass")
    val compass: ResponseCompassModel? = null,
    @Json(name = "gps")
    val gps: ResponseGpsModel? = null,
    @Json(name = "location_id")
    val locationId: String? = null,
    @Json(name = "relative")
    val relative: ResponseRelativeModel? = null,
    @Json(name = "type")
    val type: String? = null
)
