package com.arvrlab.vps_sdk.ui

import com.arvrlab.vps_sdk.data.VpsConfig
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.Renderable

interface VpsService {

    val modelNode: Node

    fun setVpsConfig(vpsConfig: VpsConfig)

    fun setVpsCallback(vpsCallback: VpsCallback)

    fun setRenderable(renderable: Renderable)

    fun startVpsService()

    fun stopVpsService()

    fun enableForceLocalization(enabled: Boolean)

}