package com.arvrlab.vps_sdk.domain.model

import android.location.Location
import android.media.Image

internal data class VpsLocationModel(
    val url: String,
    val locationID: String,
    val location: Location?,
    val image: Image,
    val isNeuro: Boolean,
    val localPosition: LocalPositionModel,
    val force: Boolean
)
