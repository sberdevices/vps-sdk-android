package com.arvrlab.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class RequestDataModel(
    @Json(name = "attributes")
    val attributes: RequestAttributesModel,
    @Json(name = "id")
    val id: String = UUID.randomUUID().toString(),
    @Json(name = "type")
    val type: String = "job"
)
