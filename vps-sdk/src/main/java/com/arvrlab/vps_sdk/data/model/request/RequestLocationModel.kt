package com.arvrlab.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RequestLocationModel(
    @Json(name = "clientCoordinateSystem")
    val clientCoordinateSystem: String = "arkit",
    @Json(name = "compass")
    val compass: RequestCompassModel = RequestCompassModel(),
    @Json(name = "gps")
    val gps: RequestGpsModel? = null,
    @Json(name = "localPos")
    val localPos: RequestLocalPosModel = RequestLocalPosModel(),
    @Json(name = "location_id")
    val locationId: String = "Polytech",
    @Json(name = "type")
    val type: String = "relative"
)
