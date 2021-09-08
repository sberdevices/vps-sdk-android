package com.arvrlab.vps_sdk.ui

import android.media.Image
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

interface IArSceneView {

    fun getWorldPosition(): Vector3
    fun getWorldRotation(): Quaternion
    fun getWorldModelMatrix(): Matrix
    fun acquireCameraImage(): Image?
    fun addChildNode(node: AnchorNode?)
}