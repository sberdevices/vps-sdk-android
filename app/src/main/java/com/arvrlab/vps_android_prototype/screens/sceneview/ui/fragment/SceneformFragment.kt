package com.arvrlab.vps_android_prototype.screens.sceneview.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.databinding.SceneformFragmentBinding
import com.arvrlab.vps_android_prototype.infrastructure.utils.POLYTECH_LOCATION_ID
import com.arvrlab.vps_android_prototype.screens.sceneview.viewmodel.SceneformViewModel
import com.arvrlab.vps_sdk.network.dto.ResponseDto
import com.arvrlab.vps_sdk.service.VpsCallback
import com.arvrlab.vps_sdk.ui.VpsArFragment
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.ModelRenderable

class SceneformFragment : Fragment(R.layout.sceneform_fragment) {

    private val binding by viewBinding(SceneformFragmentBinding::bind)

    private val viewModel: SceneformViewModel by viewModels()
    private val arFragment: VpsArFragment by lazy { (childFragmentManager.findFragmentById(R.id.sceneform_fargment)) as VpsArFragment }
    private val navArgs: SceneformFragmentArgs by navArgs()

    private var modelNode: AnchorNode? = null
    private var modelRenderable: ModelRenderable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClickListeners()
        renderModels()
        initObservers()
    }

    private fun initObservers() {
        viewModel.run {
            positionVps.observe(viewLifecycleOwner, Observer { responseFromServer ->
                Log.i("SceneformFragment", responseFromServer.toString())
            })

            vpsError.observe(viewLifecycleOwner, Observer { error ->
                showError(error)
            })
        }
    }

    private fun getVpsCallback(): VpsCallback {
        return object : VpsCallback {
            override fun onPositionVps(responseDto: ResponseDto) {
                viewModel.onPositionVps(responseDto)
            }

            override fun onError(error: Exception) {
                viewModel.onVpsErrorCallback(error)
            }
        }
    }

    private fun showError(e: Exception) {
        changeButtonsAvailability(false)
        AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(e.toString())
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                dialog.cancel()
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        arFragment.arSceneView?.pause()
        arFragment.stopVpsService()
        changeButtonsAvailability(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        arFragment.arSceneView?.destroy()
        arFragment.destroy()
    }

    private fun renderModels() {
        val model =
            if (navArgs.settings.locationID == POLYTECH_LOCATION_ID) R.raw.polytech else R.raw.bootcamp
        ModelRenderable.builder()
            .setSource(requireContext(), model)
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { renderable ->
                modelRenderable = renderable
                // initSepulkaRenderable()
                initVpsService()
            }.exceptionally { error ->
                Toast.makeText(requireContext(), error.toString(), Toast.LENGTH_LONG).show()
                return@exceptionally null
            }
    }

//    private fun initSepulkaRenderable() {
//        ModelRenderable.builder()
//            .setSource(requireContext(), R.raw.sepulka)
//            .setIsFilamentGltf(true)
//            .build()
//            .thenAccept { renderable ->
//                sepulkaRenderable = renderable
//                initVpsService()
//            }.exceptionally { error ->
//                Toast.makeText(requireContext(), error.toString(), Toast.LENGTH_LONG).show()
//                return@exceptionally null
//            }
//    }

    private fun initVpsService() {
        modelNode = AnchorNode().also {
            it.renderable = modelRenderable
            it.setAlpha()
        }

        binding.cbPolytechVisibility.isChecked = true

//        sepulkaNode = Node().apply {
//            renderable = sepulkaRenderable
//            setParent(polytechNode)
//            worldScale = Vector3(25f, 25f, 25f)
//            localPosition = Vector3(0f, 80f, 0f)
//            localRotation = Quaternion(Vector3(90f, 90f, 0f))
//        }

        //  val settings = Settings(VpsApi.BASE_URL, "Polytech", true, 6000, false)

//        val settings = Settings(VpsApi.BASE_URL, "eeb38592-4a3c-4d4b-b4c6-38fd68331521", true, 6000,
//            needLocation = false,
//            isNeuro = false
//        )

        arFragment.initVpsService(
            positionNode = modelNode ?: return,
            callback = getVpsCallback(),
            settings = navArgs.settings
        )
    }

    private fun initClickListeners() {
        changeButtonsAvailability(false)

        with(binding) {
            btnStart.setOnClickListener {
                changeButtonsAvailability(true)
                arFragment.startVpsService()
            }
            btnStop.setOnClickListener {
                changeButtonsAvailability(false)
                arFragment.stopVpsService()
            }
            cbPolytechVisibility.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    modelNode?.setAlpha()
                } else {
                    modelNode?.setAlpha(0.0f)
                }
            }
        }
    }

    private fun Node.setAlpha(alpha: Float = 0.1f) {
        val engine = EngineInstance.getEngine().filamentEngine
        val rm = engine.renderableManager

        renderableInstance?.filamentAsset?.let { asset ->
            for (entity in asset.entities) {
                val renderable = rm.getInstance(entity)
                if (renderable != 0) {
                    val r = 7f / 255
                    val g = 7f / 225
                    val b = 143f / 225
                    val materialInstance = rm.getMaterialInstanceAt(renderable, 0)
                    materialInstance.setParameter("baseColorFactor", r, g, b, alpha)
                }
            }
        }
    }

    private fun changeButtonsAvailability(isStarted: Boolean) {
        with(binding) {
            btnStart.isEnabled = !isStarted
            btnStop.isEnabled = isStarted
        }
    }
}
