package com.arvrlab.vps_sdk.domain.model

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

internal data class NodePoseModel(
    val x: Float = 0.0f,
    val y: Float = 0.0f,
    val z: Float = 0.0f,
    val roll: Float = 0.0f,
    val pitch: Float = 0.0f,
    val yaw: Float = 0.0f
) {
    companion object {
        val EMPTY = NodePoseModel()
    }

    fun getPosition(): Vector3 =
        Vector3(-x, -y, -z)

    fun getRotation(): Quaternion =
        Quaternion.eulerAngles(Vector3(roll, pitch, yaw))
            .inverted()

}
