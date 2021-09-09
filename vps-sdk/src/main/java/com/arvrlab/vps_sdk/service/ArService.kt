package com.arvrlab.vps_sdk.service

import com.google.ar.core.Frame
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.Scene

interface ArService {

    val scene: Scene

    val camera: Camera

    val arFrame: Frame?

}