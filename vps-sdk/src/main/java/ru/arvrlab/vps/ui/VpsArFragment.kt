package ru.arvrlab.vps.ui

import android.Manifest
import android.os.Bundle
import android.view.View
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.util.*

class VpsArFragment : ArFragment() {

    companion object {
        private const val FAR_CLIP_PLANE = 1000f
    }

    override fun getAdditionalPermissions(): Array<String> {
        return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arSceneView.scene.camera.farClipPlane = FAR_CLIP_PLANE
    }

    override fun onResume() {
        super.onResume()

        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
    }

    override fun getSessionConfiguration(session: Session): Config {
        session.cameraConfig = getHighestResolution(session)

        return Config(session).apply {
            focusMode = Config.FocusMode.AUTO
        }
    }

    private fun getHighestResolution(session: Session): CameraConfig? {
        val cameraConfigFilter = CameraConfigFilter(session)
            .setTargetFps(
                EnumSet.of(
                    CameraConfig.TargetFps.TARGET_FPS_30,
                    CameraConfig.TargetFps.TARGET_FPS_60
                )
            )

        val cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter)

        return cameraConfigs.maxBy { it.imageSize.height }
    }
}