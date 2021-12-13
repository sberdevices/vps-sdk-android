package com.arvrlab.vps_sdk.domain.model

import kotlin.Float.Companion.NaN

data class GpsPoseModel(
    val altitude: Float = 0f,
    val latitude: Float = 0f,
    val longitude: Float = 0f,
    val heading: Float = 0f
) {
    companion object {
        val DEFAULT = GpsPoseModel(0f, 0f, 0f, 0f)
        val EMPTY = GpsPoseModel(NaN, NaN, NaN, NaN)
    }
}
