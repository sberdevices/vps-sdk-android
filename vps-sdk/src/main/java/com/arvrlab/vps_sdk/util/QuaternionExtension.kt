package com.arvrlab.vps_sdk.util

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

private const val RAD_2_DEG = 180f / PI
private const val DEG_2_RAD = 180f * PI

fun Quaternion.getEulerAngles(): Vector3 {
    // https://github.com/coderlirui/quat2eul/blob/master/quat2eul.cpp
    // YXZ (intrinsic rotations)
    val rY = atan2(
        2 * (x * z + y * w),
        (w * w - x * x - y * y + z * z)
    )
    val rX = asin(2 * (x * w - y * z))
    val rZ = atan2(
        2 * (x * y + z * w),
        (w * w - x * x + y * y - z * z)
    )
    return Vector3(rX.toDegrees(), rY.toDegrees(), rZ.toDegrees())
}

fun Float.toDegrees(): Float =
    (this * RAD_2_DEG).toFloat()

fun Float.toRadians(): Float =
    (this / DEG_2_RAD).toFloat()