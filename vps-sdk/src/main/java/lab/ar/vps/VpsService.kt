package lab.ar.vps

import android.location.LocationManager
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.CoroutineScope
import lab.ar.network.dto.ResponseDto
import lab.ar.ui.VpsArFragment

class VpsService(
    coroutineScope: CoroutineScope,
    vpsArFragment: VpsArFragment,
    modelRenderable: ModelRenderable,
    locationManager: LocationManager,
    callback: VpsCallback,
    vpsSettings: VpsSettings,
    onCreateHierarchy: ((tranformableNode: TransformableNode) -> Unit)? = null
) {

    private val vpsDelegate = VpsDelegate(
        coroutineScope,
        vpsArFragment,
        modelRenderable,
        locationManager,
        callback,
        vpsSettings,
        onCreateHierarchy
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

}