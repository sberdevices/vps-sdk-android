package lab.ar.vps

import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.coroutines.CoroutineScope
import lab.ar.network.dto.ResponseDto
import lab.ar.ui.VpsArFragment

class VpsService(
    private val coroutineScope: CoroutineScope,
    private val vpsArFragment: VpsArFragment,
    private val modelRenderable: ModelRenderable,
    private val url: String,
    private val locationID: String,
    private val onlyForce: Boolean = true) {

    private val vpsService: VpsService by lazy { initVps() }

    fun start() {
        vpsService.start()
    }

    fun stop() {
        vpsService.stop()
    }

    fun enableForceLocalization(enabled: Boolean) {
        vpsService.enableForceLocalization(enabled)
    }

    fun localizeWithMockData(mockData: ResponseDto) {
        vpsService.localizeWithMockData(mockData)
    }

    private fun initVps() =
        VpsService(coroutineScope, vpsArFragment, modelRenderable, url, locationID, onlyForce)

}