package com.arvrlab.vps_sdk.util

import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

internal fun Matrix.getTranslation(): Vector3 =
    Vector3()
        .also { this.decomposeTranslation(it) }

internal fun Matrix.getQuaternion(): Quaternion =
    Quaternion()
        .also { this.extractQuaternion(it) }