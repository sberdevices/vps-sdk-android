package com.arvrlab.vps_sdk.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.arvrlab.vps_sdk.R
import com.arvrlab.vps_sdk.VpsSdk
import com.arvrlab.vps_sdk.ui.VpsArViewModel.Dialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Config.FocusMode
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

open class VpsArFragment : ArFragment() {

    private companion object {
        const val FAR_CLIP_PLANE = 1000f
        const val PACKAGE = "package"
    }

    private val viewModel: VpsArViewModel by viewModel()

    val vpsService: VpsService
        get() = viewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        VpsSdk.init(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewLifecycleOwner.lifecycle.addObserver(viewModel)

        lifecycleScope.launch {
            viewModel.requestPermissions.collect { (arrayPermissions, requestCode) ->
                requestPermissions(arrayPermissions, requestCode)
            }
        }
        lifecycleScope.launch {
            viewModel.showDialog.collect { (dialog, requestCode) ->
                when (dialog) {
                    Dialog.CAMERA_PERMISSION -> showCameraPermissionDialog(requestCode)
                    Dialog.LOCATION_PERMISSION -> showLocationPermissionDialog()
                    Dialog.LOCATION_ENABLE -> showLocationEnableDialog(requestCode)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.locationSettings.collect { requestCode ->
                showLocationSettings(requestCode)
            }
        }
        vpsService.bindArSceneView(arSceneView)

        instructionsController.isEnabled = false
        arSceneView.scene.camera.farClipPlane = FAR_CLIP_PLANE
        arSceneView.planeRenderer.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        vpsService.resume()
    }

    override fun onPause() {
        super.onPause()
        vpsService.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        vpsService.destroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {
        viewModel.onRequestPermissionsResult(requestCode)
    }

    override fun onCreateSessionConfig(session: Session): Config {
        session.cameraConfig = getHighestResolution(session)
        session.resume()
        return super.onCreateSessionConfig(session)
            .apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                planeFindingMode = Config.PlaneFindingMode.DISABLED
                lightEstimationMode = Config.LightEstimationMode.DISABLED
            }
    }

    fun setAutofocus(autofocus: Boolean) {
        val session = arSceneView.session ?: return
        val config = session.config ?: return

        config.focusMode = if (autofocus) FocusMode.AUTO else FocusMode.FIXED
        session.configure(config)
    }

    fun isAutofocus(): Boolean =
        arSceneView.session?.config?.focusMode == FocusMode.AUTO

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

    private fun showCameraPermissionDialog(requestCode: Int) {
        showDialog(R.string.camera_permission_warning) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                viewModel.requestCameraPermission(requestCode)
            } else {
                showApplicationSettings()
            }
        }
    }

    private fun showLocationPermissionDialog() {
        showDialog(R.string.location_permission_warning) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                viewModel.requestLocationPermission()
            } else {
                showApplicationSettings()
            }
        }
    }

    private fun showLocationEnableDialog(requestCode: Int) {
        showDialog(R.string.location_enable) {
            showLocationSettings(requestCode)
        }
    }

    private fun showDialog(message: Int, positiveAction: () -> Unit) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.warning)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showApplicationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts(PACKAGE, requireActivity().packageName, null)
        }
        requireActivity().startActivity(intent)
    }

    private fun showLocationSettings(requestCode: Int) {
        startActivityForResult(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
            requestCode
        )
    }

}