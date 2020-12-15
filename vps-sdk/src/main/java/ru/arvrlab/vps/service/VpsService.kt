package ru.arvrlab.vps.service

import android.location.LocationManager
import com.google.ar.sceneform.Node
import kotlinx.coroutines.CoroutineScope
import ru.arvrlab.vps.network.dto.ResponseDto
import ru.arvrlab.vps.ui.VpsArFragment

class VpsService private constructor(
    coroutineScope: CoroutineScope,
    vpsArFragment: VpsArFragment,
    node: Node,
    locationManager: LocationManager?,
    callback: VpsCallback?,
    settings: Settings
) {

    private val vpsDelegate = VpsDelegateNeuro(
        coroutineScope,
        vpsArFragment,
        node,
        locationManager,
        callback,
        settings
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
        private var node: Node? = null
        private var settings: Settings? = null
        private var locationManager: LocationManager? = null
        private var callback: VpsCallback? = null

        fun setCoroutineScope(coroutineScope: CoroutineScope): Builder {
            this.coroutineScope = coroutineScope
            return this
        }

        fun setVpsArFragment(vpsArFragment: VpsArFragment): Builder {
            this.vpsArFragment = vpsArFragment
            return this
        }

        fun setNode(node: Node): Builder {
            this.node = node
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

        fun build(): VpsService {
            return VpsService(
                coroutineScope ?: throw Exception("No coroutineScope was set in VpsDelegate.Builder"),
                vpsArFragment ?: throw Exception("No VpsArFragment was set in VpsDelegate.Builder"),
                node ?: throw Exception("No Renderable was set in VpsDelegate.Builder"),
                locationManager,
                callback,
                settings ?: throw Exception("No VpsSettings was set in VpsDelegate.Builder")
            )
        }
    }

}