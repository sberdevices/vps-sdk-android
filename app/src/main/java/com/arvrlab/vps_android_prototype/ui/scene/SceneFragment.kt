package com.arvrlab.vps_android_prototype.ui.scene

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.databinding.FmtSceneBinding
import com.arvrlab.vps_android_prototype.util.Logger
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.network.dto.ResponseDto
import com.arvrlab.vps_sdk.service.VpsCallback
import com.arvrlab.vps_sdk.ui.VpsArFragment

class SceneFragment : Fragment(R.layout.fmt_scene) {

    private val binding by viewBinding(FmtSceneBinding::bind)

    private val vpsArFragment: VpsArFragment by lazy {
        childFragmentManager.findFragmentById(binding.vFragmentContainer.id) as VpsArFragment
    }

    private val viewModel: SceneViewModel by viewModels()
    private val navArgs: SceneFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initVpsService()
        initClickListeners()
        initObservers()
    }

    override fun onPause() {
        super.onPause()
        vpsArFragment.stopVpsService()
        changeButtonsAvailability(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        vpsArFragment.onDestroy()
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

        vpsArFragment.configureVpsService(vpsConfig, getVpsCallback())
        vpsArFragment.loadModelByRawId(navArgs.sceneModel.modelRawId)
            .thenApply { binding.cbPolytechVisibility.isChecked = true }
    }

    private fun initClickListeners() {
        changeButtonsAvailability(false)

        with(binding) {
            btnStart.setOnClickListener {
                changeButtonsAvailability(true)
                vpsArFragment.startVpsService()
            }
            btnStop.setOnClickListener {
                changeButtonsAvailability(false)
                vpsArFragment.stopVpsService()
            }
            cbPolytechVisibility.setOnCheckedChangeListener { _, isChecked ->
                vpsArFragment.setArAlpha(if (isChecked) 0.5f else 0.0f)
            }
        }
    }

    private fun initObservers() {
        viewModel.run {
            positionVps.observe(viewLifecycleOwner, { responseFromServer ->
                Logger.debug(responseFromServer.toString())
            })

            vpsError.observe(viewLifecycleOwner, { error ->
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
