package com.arvrlab.vps_sdk.ui

import com.arvrlab.vps_sdk.data.VpsConfig
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import org.koin.core.context.GlobalContext

interface VpsService {

    companion object {

        @JvmStatic
        fun newInstance(): VpsService =
            GlobalContext.get().get()
    }

    val worldNode: Node
    val isRun: Boolean

    fun bindArSceneView(arSceneView: ArSceneView)
    fun resume()
    fun pause()
    fun unbindArSceneView()
    fun destroy()

    fun setVpsConfig(vpsConfig: VpsConfig)
    fun setVpsCallback(vpsCallback: VpsCallback)

    fun startVpsService()
    fun stopVpsService()

}