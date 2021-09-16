package com.arvrlab.vps_sdk.ui

import com.arvrlab.vps_sdk.data.VpsConfig
import com.google.ar.sceneform.Node

interface VpsService {

    val worldNode: Node

    fun setVpsConfig(vpsConfig: VpsConfig)

    fun setVpsCallback(vpsCallback: VpsCallback)

    fun startVpsService()

    fun stopVpsService()

}