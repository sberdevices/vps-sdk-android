package com.arvrlab.vps_sdk.util

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

private const val RAD_2_DEG = 180f / PI

fun Quaternion.getEulerAngles(): Vector3 {
    val rZ = atan2(
        2 * (z * w - x * y),
        (w * w - x * x + y * y - z * z)
    )
    val rX = asin(2 * (x * w + y * z))
    val rY = atan2(
        2 * (y * w - z * x),
        (w * w - x * x - y * y + z * z)
    )

    return Vector3(rX.toDegrees(), rY.toDegrees(), rZ.toDegrees())
}

private fun Float.toDegrees(): Float =
    (this * RAD_2_DEG).toFloat()