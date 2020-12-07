package lab.ar.vps

import android.location.LocationManager
import com.google.ar.sceneform.rendering.ModelRenderable
import lab.ar.network.dto.ResponseDto
import lab.ar.ui.VpsArFragment

class VpsService(
    vpsArFragment: VpsArFragment,
    modelRenderable: ModelRenderable,
    url: String,
    locationID: String,
    onlyForce: Boolean = true,
    locationManager: LocationManager,
    callback: VpsCallback
) {

    private val vpsDelegate = VpsDelegate(
        vpsArFragment,
        modelRenderable,
        url,
        locationID,
        onlyForce,
        locationManager,
        callback
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