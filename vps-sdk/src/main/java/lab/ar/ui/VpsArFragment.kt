package lab.ar.ui

import android.os.Bundle
import android.view.View
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import java.util.*

class VpsArFragment : ArFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arSceneView.scene.camera.farClipPlane = 1000f
    }

    override fun onResume() {
        super.onResume()

        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
    }

    override fun getSessionConfiguration(session: Session): Config {
        session.cameraConfig = getHighestResolution(session)

        val config = Config(session)
        config.focusMode = Config.FocusMode.AUTO

        return config
    }


    private fun getHighestResolution(session: Session): CameraConfig? {
        val cameraConfigFilter = CameraConfigFilter(session)
            .setTargetFps(
                EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30,
                    CameraConfig.TargetFps.TARGET_FPS_60))

        val cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter)

        return cameraConfigs.maxBy { it.imageSize.height }
    }
}