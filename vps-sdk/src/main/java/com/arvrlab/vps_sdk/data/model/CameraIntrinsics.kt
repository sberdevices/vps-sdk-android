package com.arvrlab.vps_sdk.data.model

internal data class CameraIntrinsics(
    val fx: Float,
    val fy: Float,
    val cx: Float,
    val cy: Float,
) {
    companion object {
        val EMPTY = CameraIntrinsics(0f, 0f, 0f, 0f)
    }
}
