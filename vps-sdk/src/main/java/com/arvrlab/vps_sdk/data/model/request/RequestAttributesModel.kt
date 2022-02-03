package com.arvrlab.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RequestAttributesModel(
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "timestamp")
    val timestamp: Double,
    @Json(name = "forced_localization")
    val forcedLocalisation: Boolean,
    @Json(name = "imageTransform")
    val imageTransform: RequestImageTransformModel = RequestImageTransformModel(),
    @Json(name = "intrinsics")
    val intrinsics: RequestIntrinsicsModel,
    @Json(name = "location")
    val location: RequestLocationModel
)
