package com.arvrlab.vps_sdk.util

import com.arvrlab.vps_sdk.domain.model.LocalPositionModel
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

fun Matrix.toPositionVector(): Vector3 {
    val m31: Float = this.data[13]
    val m32: Float = this.data[14]
    val m33: Float = this.data[15]

    return Vector3(m31, m32, m33)
}

//x y z => roll yaw pitch
fun Quaternion.toEulerAngles(): Vector3 {

    val test = x * y + z * w
    if (test > 0.499) { // singularity at north pole
        val y = 2 * atan2(x, w)
        val z = Math.PI / 2
        val x = 0f
        return Vector3(x, y, z.toFloat())
    }
    if (test < -0.499) { // singularity at south pole
        val y = -2 * atan2(x, w)
        val z = -Math.PI / 2;
        val x = 0f
        return Vector3(x, y, z.toFloat())
    }
    val sqx = x * x
    val sqy = y * y
    val sqz = z * z
    val y = atan2(2 * y * w - 2 * x * z, 1 - 2 * sqy - 2 * sqz);
    val z = asin(2 * test);
    val x = atan2(2 * x * w - 2 * y * z, 1 - 2 * sqx - 2 * sqz)

    return Vector3((x * 180 / PI).toFloat(), (y * 180 / PI).toFloat(), (z * 180 / PI).toFloat())
}

fun getConvertedCameraStartRotation(cameraRotation: Quaternion): Quaternion {
    val dir = Quaternion.rotateVector(cameraRotation, Vector3(0f, 0f, 1f))
    dir.y = 0f
    return Quaternion.rotationBetweenVectors(Vector3(0f, 0f, 1f), dir)
}

internal fun LocalPositionModel.toNewRotationAndPositionPair(): Pair<Quaternion, Vector3> {
    val yaw = this.yaw
    val x = -this.x
    val y = -this.y
    val z = -this.z

    val newPosition = Vector3(x, y, z)

    val newRotation = if (yaw > 0) {
        Quaternion(Vector3(0f, 180f - yaw, 0f)).inverted()
    } else {
        Quaternion(Vector3(0f, yaw, 0f)).inverted()
    }

    return Pair(newRotation, newPosition)
}
