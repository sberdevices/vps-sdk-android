package com.arvrlab.vps_sdk.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class VpsArFragment : ArFragment() {

    private companion object {
        const val FAR_CLIP_PLANE = 1000f
    }

    private val viewModel: VpsArViewModel by viewModels {
        VpsArViewModelFactory(requireActivity().application)
    }

    val vpsService: VpsService
        get() = viewModel

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
            viewModel.locationPermissionDialog.collect {
                showLocationPermissionDialog()
            }
        }
        lifecycleScope.launch {
            viewModel.cameraPermissionDialog.collect {
                showCameraPermissionDialog()
            }
        }
        viewModel.bindArSceneView(arSceneView)

        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        arSceneView.scene.camera.farClipPlane = FAR_CLIP_PLANE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {
        viewModel.onRequestPermissionsResult(requestCode)
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

    private fun showCameraPermissionDialog() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle("Camera permission required")
            .setMessage("Add camera permission via Settings?")
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", requireActivity().packageName, null)
                }

                requireActivity().startActivity(intent)
                canRequestDangerousPermissions = true
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener {
                // canRequestDangerousPermissions will be true if "OK" was selected from the dialog,
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

    private fun showLocationPermissionDialog() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle("Location permission required")
            .setMessage("Add location permission via Settings?")
            .setPositiveButton(
                android.R.string.ok
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

}