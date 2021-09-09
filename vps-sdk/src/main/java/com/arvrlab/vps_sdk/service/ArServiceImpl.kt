package com.arvrlab.vps_sdk.service

import com.google.ar.core.Frame
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.Scene

internal class ArServiceImpl(private val arSceneView: ArSceneView) : ArService {

    override val scene: Scene
        get() = arSceneView.scene

    override val camera: Camera
        get() = scene.camera

    override val arFrame: Frame?
        get() = arSceneView.arFrame

}