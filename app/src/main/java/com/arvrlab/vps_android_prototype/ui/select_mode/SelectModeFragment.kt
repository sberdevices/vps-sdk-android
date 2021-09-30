package com.arvrlab.vps_android_prototype.ui.select_mode

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.databinding.FmtSelectModeBinding

class SelectModeFragment : Fragment(R.layout.fmt_select_mode) {

    private val binding by viewBinding(FmtSelectModeBinding::bind)
    private val viewModel: SelectModeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initOnClickListeners()
    }

    private fun initOnClickListeners() {
        with(binding) {
            rbPolytech.setOnClickListener {
                viewModel.onPolytechSelected()
            }
            rbBootcamp.setOnClickListener {
                viewModel.onBootcampSelected()
            }
            etTimerInterval.addTextChangedListener { editable ->
                viewModel.onIntervalChanged(editable.toString())
            }
            cbOnlyForce.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onOnlyForseChanged(isChecked)
            }
            cbNeedLocation.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onNeedLocationChanged(isChecked)
            }
            cbUseNeuro.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onUseNeuroChanged(isChecked)
            }
            cbSerialImages.setOnCheckedChangeListener { _, isChecked ->
                tilImagesCount.isVisible = isChecked
                tilImagesInterval.isVisible = isChecked
                if (isChecked) {
                    viewModel.onImagesCountChanged(etImagesCount.text.toString())
                    viewModel.onImagesIntervalChanged(etImagesInterval.text.toString())
                } else {
                    viewModel.onImagesCountChanged("1")
                }
            }
            etImagesCount.addTextChangedListener { editable ->
                viewModel.onImagesCountChanged(editable.toString())
            }
            etImagesInterval.addTextChangedListener { editable ->
                viewModel.onImagesIntervalChanged(editable.toString())
            }

            btnSubmit.setOnClickListener {
                findNavController().navigate(
                    SelectModeFragmentDirections.actionOpenSceneFragment(viewModel.sceneModel)
                )
            }
        }
    }
}
