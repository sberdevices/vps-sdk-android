package com.arvrlab.vps_android_prototype.ui.scene

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.databinding.FmtSceneBinding
import com.arvrlab.vps_android_prototype.util.Logger
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.ui.VpsArFragment
import com.arvrlab.vps_sdk.ui.VpsCallback
import com.arvrlab.vps_sdk.ui.VpsService
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.ModelRenderable

class SceneFragment : Fragment(R.layout.fmt_scene) {

    private companion object {
        const val ALPHA_ENABLE = 0.5f
        const val ALPHA_DISABLE = 0f
    }

    private val binding by viewBinding(FmtSceneBinding::bind)

    private val vpsArFragment: VpsArFragment
        get() = childFragmentManager.findFragmentById(binding.vFragmentContainer.id) as VpsArFragment

    private val vpsService: VpsService
        get() = vpsArFragment.vpsService

    private val viewModel: SceneViewModel by viewModels()
    private val navArgs: SceneFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initVpsService()
        initClickListeners()
    }

    private fun initVpsService() {
        val vpsConfig = navArgs.sceneModel
            .let { sceneModel ->
                VpsConfig(
                    vpsUrl = sceneModel.url,
                    locationID = sceneModel.locationID,
                    onlyForce = sceneModel.onlyForce,
                    intervalLocalizationMS = sceneModel.intervalLocalizationMS,
                    useGps = sceneModel.useGps,
                    localizationType = sceneModel.localizationType,
                    useSerialImages = sceneModel.useSerialImages,
                    countImages = sceneModel.imagesCount,
                    intervalImagesMS = sceneModel.intervalImagesMS
                )
            }

        with(vpsService) {
            setVpsConfig(vpsConfig)
            setVpsCallback(getVpsCallback())
        }
        loadModel(navArgs.sceneModel.modelRawId)
        binding.cbPolytechVisibility.isChecked = true
    }

    private fun initClickListeners() {
        changeButtonsAvailability(false)

        with(binding) {
            btnStart.setOnClickListener {
                vpsService.startVpsService()
            }
            btnStop.setOnClickListener {
                vpsService.stopVpsService()
            }
            cbPolytechVisibility.setOnCheckedChangeListener { _, isChecked ->
                vpsService.worldNode.setArAlpha(if (isChecked) ALPHA_ENABLE else ALPHA_DISABLE)
            }
        }
    }

    private fun getVpsCallback(): VpsCallback {
        return object : VpsCallback {
            override fun onSuccess() {
                Toast.makeText(requireContext(), "localization success", Toast.LENGTH_SHORT).show()
                Logger.debug("VPS localization success")
            }

            override fun onFail() {
                Logger.debug("VPS localization fail")
                Toast.makeText(requireContext(), "localization fail", Toast.LENGTH_SHORT).show()
            }

            override fun onStateChange(state: VpsService.State) {
                Logger.debug("VPS service: $state")
                changeButtonsAvailability(state == VpsService.State.RUN)
            }

            override fun onError(error: Throwable) {
                Logger.error(error)
                showError(error)
            }
        }
    }

    private fun loadModel(@RawRes rawRes: Int) {
        ModelRenderable.builder()
            .setSource(context, rawRes)
            .setIsFilamentGltf(true)
            .build()
            .thenApply { renderable ->
                with(vpsService.worldNode) {
                    setRenderable(renderable)
                    setArAlpha(ALPHA_ENABLE)
                }
            }
            .exceptionally { Logger.error(it) }
    }

    private fun Node.setArAlpha(alpha: Float) {
        val engine = EngineInstance.getEngine().filamentEngine
        val renderableManager = engine.renderableManager

        this.renderableInstance?.filamentAsset?.let { asset ->
            for (entity in asset.entities) {
                val entityName = asset.getName(entity)
                if (entityName == "Plane") {
                    engine.destroyEntity(entity)
                    continue
                }
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

    private fun showError(e: Throwable) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(e.toString())
            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun changeButtonsAvailability(isStarted: Boolean) {
        with(binding) {
            btnStart.isEnabled = !isStarted
            btnStop.isEnabled = isStarted
        }
    }
}
