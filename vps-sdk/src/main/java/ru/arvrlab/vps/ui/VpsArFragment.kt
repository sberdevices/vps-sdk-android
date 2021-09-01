package ru.arvrlab.vps.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ru.arvrlab.vps.network.dto.ResponseDto
import ru.arvrlab.vps.service.Settings
import ru.arvrlab.vps.service.VpsCallback
import ru.arvrlab.vps.service.VpsService
import java.util.*

class VpsArFragment : ArFragment() {

    private var vpsService: VpsService? = null
    private var needLocation = false

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

        return cameraConfigs.maxByOrNull { it.imageSize.height }
    }

    fun initVpsService(
        positionNode: Node,
        callback: VpsCallback,
        settings: Settings
    ) {
        val locationManager = ContextCompat.getSystemService(
            requireContext(),
            LocationManager::class.java
        ) as LocationManager
        needLocation = settings.needLocation

        vpsService = VpsService.Builder()
            .setCoroutineScope(CoroutineScope(lifecycleScope.coroutineContext + Dispatchers.IO))
            .setVpsArFragment(this)
            .setNode(positionNode)
            .setLocationManager(locationManager)
            .setVpsCallback(callback)
            .setSettings(settings)
            .build()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        destroy()
    }

    fun startVpsService() {
        if (needLocation) {
            checkPermission()
        } else {
            vpsService?.start()
        }
    }

    private fun checkPermission() {
        if(foregroundPermissionApproved()) {
            vpsService?.start()
        } else {
            requestForegroundPermissions()
        }
    }

    fun stopVpsService() {
        vpsService?.stop()
    }

    fun enableForceLocalization(enabled: Boolean) {
        vpsService?.enableForceLocalization(enabled)
    }

    fun localizeWithMockData(mockData: ResponseDto) {
        vpsService?.localizeWithMockData(mockData)
    }

    fun destroy() {
        vpsService?.destroy()
    }



    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestForegroundPermissions() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_ONLY_PERMISSION_RC
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {

        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSION_RC) {
            checkResultLocation()
        }

        if(requestCode == CAMERA_PERMISSION_RC) {
            checkResultCamera()
        }
    }

    private fun checkResultLocation() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVpsService()
            return
        }

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle("Location permission required")
            .setMessage("Add location permission via Settings?")
            .setPositiveButton(
               "ok"
            ) { _, _ ->
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", requireActivity().packageName, null)
                }
                requireActivity().startActivity(intent)

                canRequestDangerousPermissions = true
            }
            .setCancelable(false)
            .show()
    }

    private fun checkResultCamera() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle("Camera permission required")
            .setMessage("Add camera permission via Settings?")
            .setPositiveButton(
                "ok"
            ) { _, _ ->
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", requireActivity().packageName, null)
                }

                requireActivity().startActivity(intent)
                canRequestDangerousPermissions = true
            }
            .setNegativeButton("cancel", null)
            .setOnDismissListener { // canRequestDangerousPermissions will be true if "OK" was selected from the dialog,
                // false otherwise.  If "OK" was selected do nothing on dismiss, the app will
                // continue and may ask for permission again if needed.
                // If anything else happened, finish the activity when this dialog is
                // dismissed.
                if (!canRequestDangerousPermissions) {
                    requireActivity().finish()
                }
            }
            .show()
    }

    companion object{
        private const val REQUEST_FOREGROUND_ONLY_PERMISSION_RC = 34
        private const val CAMERA_PERMISSION_RC = 1010
        private const val FAR_CLIP_PLANE = 1000f
    }


}