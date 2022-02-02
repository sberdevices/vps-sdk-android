package com.arvrlab.vps_android_prototype.ui.polytech

import android.os.Bundle
import android.view.View
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.ui.base.SceneFragment
import com.arvrlab.vps_sdk.data.VpsConfig
import com.google.ar.core.Config
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.EngineInstance

class PolytechFragment : SceneFragment() {

    private companion object {
        const val URL = "https://vps.arvr.sberlabs.com/polytech-pub/Polytech/"
        const val LOCATION_ID = "Polytech"
    }

    override var vpsConfig: VpsConfig = VpsConfig.getOutdoorConfig(
        vpsUrl = URL,
        locationID = LOCATION_ID
    )

    private val occluderNode: Node =
        Node()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadModel(R.raw.robot) {
            vpsService.worldNode
                .addChild(Node().apply { renderable = it })
        }
        loadModel(R.raw.polytech) {
            occluderNode.renderable = it
            setupOccluder()
        }
        vpsArFragment.setOnSessionConfigurationListener { session, config ->
            config.focusMode = Config.FocusMode.FIXED
            session.resume()
        }
    }

    override fun updateOccluderState() {
        if (occluderEnable) {
            vpsService.worldNode.addChild(occluderNode)
        } else {
            vpsService.worldNode.removeChild(occluderNode)
        }
    }

    private fun setupOccluder() {
        val engine = EngineInstance.getEngine().filamentEngine
        val renderableManager = engine.renderableManager

        occluderNode.renderableInstance?.filamentAsset?.let { asset ->
            for (entity in asset.entities) {
                val renderable = renderableManager.getInstance(entity)
                if (renderable != 0) {
                    val r = 7f / 255
                    val g = 7f / 225
                    val b = 143f / 225
                    val materialInstance = renderableManager.getMaterialInstanceAt(renderable, 0)
                    materialInstance.setParameter("baseColorFactor", r, g, b, 0.3f)
                }
            }
        }
    }

}