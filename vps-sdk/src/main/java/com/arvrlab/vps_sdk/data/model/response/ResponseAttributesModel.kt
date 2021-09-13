package com.arvrlab.vps_sdk.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ResponseAttributesModel(
    @Json(name = "location")
    val location: ResponseLocationModel? = null,
    @Json(name = "status")
    val status: String? = null
)
