package com.arvrlab.vps_android_prototype.screens.sceneview.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.databinding.SceneformFragmentBinding
import com.arvrlab.vps_android_prototype.infrastructure.utils.POLYTECH_LOCATION_ID
import com.arvrlab.vps_android_prototype.screens.sceneview.viewmodel.SceneformViewModel
import com.arvrlab.vps_android_prototype.util.Logger
import com.arvrlab.vps_sdk.network.dto.ResponseDto
import com.arvrlab.vps_sdk.service.VpsCallback

class SceneformFragment : Fragment(R.layout.sceneform_fragment) {

    private val binding by viewBinding(SceneformFragmentBinding::bind)

    private val viewModel: SceneformViewModel by viewModels()
    private val navArgs: SceneformFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClickListeners()
        renderModels()
        initObservers()
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

    override fun onPause() {
        super.onPause()
        binding.vpsArView.pause()
        changeButtonsAvailability(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.vpsArView.destroy()
    }

    private fun renderModels() {
        val model = if (navArgs.settings.locationID == POLYTECH_LOCATION_ID) R.raw.polytech else R.raw.bootcamp
        binding.vpsArView.initVpsService(
            model = model,
            callback = getVpsCallback(),
            settings = navArgs.settings
        ).handle { _, _ ->
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

    private fun changeButtonsAvailability(isStarted: Boolean) {
        with(binding) {
            btnStart.isEnabled = !isStarted
            btnStop.isEnabled = isStarted
        }
    }
}
