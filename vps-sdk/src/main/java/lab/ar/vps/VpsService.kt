package lab.ar.vps

import android.location.LocationManager
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.coroutines.CoroutineScope
import lab.ar.network.dto.ResponseDto
import lab.ar.ui.VpsArFragment

class VpsService(
    private val coroutineScope: CoroutineScope,
    private val vpsArFragment: VpsArFragment,
    private val modelRenderable: ModelRenderable,
    private val url: String? = null,
    private val locationID: String,
    private val onlyForce: Boolean = true,
    private val locationManager: LocationManager
) {

    private val vpsService: Vps by lazy { initVps() }

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
        Vps(coroutineScope, vpsArFragment, modelRenderable, url, locationID, onlyForce, locationManager)

}