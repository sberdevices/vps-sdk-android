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

class SceneFragment : Fragment(R.layout.fmt_scene) {

    private val binding by viewBinding(FmtSceneBinding::bind)

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
        binding.vpsArView.pause()
        changeButtonsAvailability(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.vpsArView.destroy()
    }

    private fun initVpsService() {
        val vpsConfig = navArgs.sceneModel
            .let { sceneModel ->
                VpsConfig(
                    sceneModel.url,
                    sceneModel.locationID,
                    sceneModel.modelRawId,
                    sceneModel.onlyForce,
                    sceneModel.timerInterval,
                    sceneModel.needLocation,
                    sceneModel.isNeuro
                )
            }
        binding.vpsArView.initVpsService(vpsConfig, getVpsCallback())
            .thenApply {
                binding.cbPolytechVisibility.isChecked = true
            }
    }

    private fun initClickListeners() {
        changeButtonsAvailability(false)

        with(binding) {
            btnStart.setOnClickListener {
                changeButtonsAvailability(true)
                binding.vpsArView.startVpsService()
            }
            btnStop.setOnClickListener {
                changeButtonsAvailability(false)
                binding.vpsArView.stopVpsService()
            }
            cbPolytechVisibility.setOnCheckedChangeListener { _, isChecked ->
                binding.vpsArView.setArAlpha(if (isChecked) 0.5f else 0.0f)
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
