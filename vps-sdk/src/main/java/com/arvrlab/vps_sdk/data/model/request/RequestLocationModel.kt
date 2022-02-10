package com.arvrlab.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RequestLocationModel(
    @Json(name = "clientCoordinateSystem")
    val clientCoordinateSystem: String = "arcore",
    @Json(name = "compass")
    val compass: RequestCompassModel,
    @Json(name = "gps")
    val gps: RequestGpsModel?,
    @Json(name = "localPos")
    val localPos: RequestLocalPosModel,
    @Json(name = "location_id")
    val locationId: String,
    @Json(name = "type")
    val type: String = "relative"
)
