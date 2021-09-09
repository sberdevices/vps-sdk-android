package com.arvrlab.vps_sdk.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.RawRes
import androidx.core.app.ActivityCompat
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.service.*
import com.arvrlab.vps_sdk.util.Logger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import java.util.*
import java.util.concurrent.CompletableFuture

class VpsArFragment : ArFragment(), RequestPermissions {

    private companion object {
        const val REQUEST_FOREGROUND_ONLY_PERMISSION_RC = 34
        const val CAMERA_PERMISSION_RC = 1010
        const val FAR_CLIP_PLANE = 1000f
    }

    private var vpsService: VpsService? = null

    private val arService: ArService by lazy {
        ArServiceImpl(arSceneView)
    }

    private val needLocation: Boolean
        get() = vpsService?.vpsConfig?.needLocation ?: false

    @CallSuper
    override fun onAttach(context: Context) {
        super.onAttach(context)
        vpsService = VpsService(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vpsService?.bindArService(arService)

        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        arSceneView.scene.camera.farClipPlane = FAR_CLIP_PLANE
    }

    override fun onDestroy() {
        super.onDestroy()
        vpsService?.unbindArService()
        vpsService?.destroy()
        vpsService = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {

        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSION_RC) {
            checkResultLocation()
        }

        if (requestCode == CAMERA_PERMISSION_RC) {
            checkResultCamera()
        }
    }

    override fun getSessionConfiguration(session: Session): Config {
        session.cameraConfig = getHighestResolution(session)

        return Config(session).apply {
            focusMode = Config.FocusMode.AUTO
        }
    }

    fun configureVpsService(vpsConfig: VpsConfig, vpsCallback: VpsCallback) {
        vpsService?.setVpsConfig(vpsConfig)
        vpsService?.setVpsCallback(vpsCallback)
    }

    fun loadModelByRawId(@RawRes rawRes: Int): CompletableFuture<Unit> =
        ModelRenderable.builder()
            .setSource(context, rawRes)
            .setIsFilamentGltf(true)
            .build()
            .thenApply { renderable ->
                vpsService?.let {
                    it.anchorNode.renderable = renderable
                } ?: throw IllegalStateException("VpsService yet not created")
            }
            .exceptionally { Logger.error(it) }

    fun startVpsService() {
        if (needLocation) {
            checkPermission()
        } else {
            vpsService?.start()
        }
    }

    fun stopVpsService() {
        vpsService?.stop()
    }

    fun enableForceLocalization(enabled: Boolean) {
        vpsService?.enableForceLocalization(enabled)
    }

    fun setArAlpha(alpha: Float) {
        val modelNode = vpsService?.anchorNode ?: return

        val engine = EngineInstance.getEngine().filamentEngine
        val renderableManager = engine.renderableManager

        modelNode.renderableInstance?.filamentAsset?.let { asset ->
            for (entity in asset.entities) {
                val renderable = renderableManager.getInstance(entity)
                if (renderable != 0) {
                    val r = 7f / 255
                    val g = 7f / 225
                    val b = 143f / 225
                    val materialInstance = renderableManager.getMaterialInstanceAt(renderable, 0)
                    materialInstance.setParameter("baseColorFactor", r, g, b, alpha)
                }
            }
        }
    }

    private fun checkPermission() {
        if (foregroundPermissionApproved()) {
            vpsService?.start()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSION_RC
            )
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

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
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

}