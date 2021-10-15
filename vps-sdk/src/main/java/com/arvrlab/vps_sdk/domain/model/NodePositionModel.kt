package com.arvrlab.vps_sdk.domain.model

internal data class NodePositionModel(
    val x: Float = 0.0f,
    val y: Float = 0.0f,
    val z: Float = 0.0f,
    val roll: Float = 0.0f,
    val pitch: Float = 0.0f,
    val yaw: Float = 0.0f
) {
    companion object {
        val EMPTY = NodePositionModel()
    }
}
