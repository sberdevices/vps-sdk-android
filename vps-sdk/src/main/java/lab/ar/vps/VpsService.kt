package lab.ar.vps

import android.location.LocationManager
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.coroutines.CoroutineScope
import lab.ar.network.dto.ResponseDto
import lab.ar.ui.VpsArFragment

class VpsService(
    coroutineScope: CoroutineScope,
    vpsArFragment: VpsArFragment,
    modelRenderable: ModelRenderable,
    locationManager: LocationManager,
    callback: VpsCallback,
    vpsSettings: VpsSettings
) {

    private val vpsDelegate = VpsDelegate(
        coroutineScope,
        vpsArFragment,
        modelRenderable,
        locationManager,
        callback,
        vpsSettings
    )

    fun start() {
        vpsDelegate.start()
    }

    fun stop() {
        vpsDelegate.stop()
    }

    fun enableForceLocalization(enabled: Boolean) {
        vpsDelegate.enableForceLocalization(enabled)
    }

    fun localizeWithMockData(mockData: ResponseDto) {
        vpsDelegate.localizeWithMockData(mockData)
    }

    fun destroy() {
        vpsDelegate.destroy()
    }

    fun getRenderableNode() = vpsDelegate.getRenderableNode()
}