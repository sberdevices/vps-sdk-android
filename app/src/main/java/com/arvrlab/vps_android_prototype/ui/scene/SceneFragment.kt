package com.arvrlab.vps_android_prototype.ui.scene

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.annotation.RawRes
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

    override fun onPause() {
        super.onPause()
        changeButtonsAvailability(false)
    }

    private fun initVpsService() {
        val vpsConfig = navArgs.sceneModel
            .let { sceneModel ->
                VpsConfig(
                    sceneModel.url,
                    sceneModel.locationID,
                    sceneModel.onlyForce,
                    sceneModel.timerInterval,
                    sceneModel.needLocation,
                    sceneModel.isNeuro
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
                changeButtonsAvailability(true)
                vpsService.startVpsService()
            }
            btnStop.setOnClickListener {
                changeButtonsAvailability(false)
                vpsService.stopVpsService()
            }
            cbPolytechVisibility.setOnCheckedChangeListener { _, isChecked ->
                vpsService.modelNode.setArAlpha(if (isChecked) 0.5f else 0.0f)
            }
        }
    }

    private fun getVpsCallback(): VpsCallback {
        return object : VpsCallback {
            override fun onPositionVps() {
                Logger.debug("onPositionVps success")
            }

            override fun onError(error: Exception) {
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
                with(vpsService) {
                    setRenderable(renderable)
                    modelNode.setArAlpha(0.5f)
                }
            }
            .exceptionally { Logger.error(it) }
    }

    private fun Node.setArAlpha(alpha: Float) {
        val engine = EngineInstance.getEngine().filamentEngine
        val renderableManager = engine.renderableManager

        this.renderableInstance?.filamentAsset?.let { asset ->
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

    private fun showError(e: Exception) {
        changeButtonsAvailability(false)
        AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Error")
            .setMessage(e.toString())
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun changeButtonsAvailability(isStarted: Boolean) {
        with(binding) {
            btnStart.isEnabled = !isStarted
            btnStop.isEnabled = isStarted
        }
    }
}
