package com.arvrlab.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RequestImageTransformModel(
    @Json(name = "mirrorX")
    val mirrorX: Boolean = false,
    @Json(name = "mirrorY")
    val mirrorY: Boolean = false,
    @Json(name = "orientation")
    val orientation: Int = 0
)
