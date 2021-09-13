package com.arvrlab.vps_sdk.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ResponseVpsModel(
    @Json(name = "data")
    val data: ResponseDataModel? = null
)
