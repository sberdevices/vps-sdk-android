package lab.ar.vps

import android.location.LocationManager
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.CoroutineScope
import lab.ar.network.dto.ResponseDto
import lab.ar.ui.VpsArFragment

class VpsService private constructor(
    coroutineScope: CoroutineScope,
    vpsArFragment: VpsArFragment,
    renderable: Renderable,
    locationManager: LocationManager?,
    callback: VpsCallback?,
    settings: Settings,
    onCreateHierarchy: ((tranformableNode: TransformableNode) -> Unit)? = null
) {

    private val vpsDelegate = VpsDelegate(
        coroutineScope,
        vpsArFragment,
        renderable,
        locationManager,
        callback,
        settings,
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

    class Builder {
        private var coroutineScope: CoroutineScope? = null
        private var vpsArFragment: VpsArFragment? = null
        private var renderable: Renderable? = null
        private var settings: Settings? = null
        private var locationManager: LocationManager? = null
        private var callback: VpsCallback? = null
        private var onCreateHierarchy: ((tranformableNode: TransformableNode) -> Unit)? = null

        fun setCoroutineScope(coroutineScope: CoroutineScope): Builder {
            this.coroutineScope = coroutineScope
            return this
        }

        fun setVpsArFragment(vpsArFragment: VpsArFragment): Builder {
            this.vpsArFragment = vpsArFragment
            return this
        }

        fun setRenderable(renderable: Renderable): Builder {
            this.renderable = renderable
            return this
        }

        fun setSettings(settings: Settings): Builder {
            this.settings = settings
            return this
        }

        /**
         * need only when settings.needLocation = true
         */
        fun setLocationManager(locationManager: LocationManager): Builder {
            this.locationManager = locationManager
            return this
        }

        fun setVpsCallback(callback: VpsCallback): Builder {
            this.callback = callback
            return this
        }

        fun setActionOnCreateHierarchy(onCreateHierarchy: ((tranformableNode: TransformableNode) -> Unit)): Builder {
            this.onCreateHierarchy = onCreateHierarchy
            return this
        }

        fun build(): VpsService {
            return VpsService(
                coroutineScope ?: throw Exception("No CoroutineScope was set in VpsDelegate.Builder"),
                vpsArFragment ?: throw Exception("No VpsArFragment was set in VpsDelegate.Builder"),
                renderable ?: throw Exception("No Renderable was set in VpsDelegate.Builder"),
                locationManager,
                callback,
                settings ?: throw Exception("No VpsSettings was set in VpsDelegate.Builder"),
                onCreateHierarchy
            )
        }
    }

}