package com.arvrlab.vps_android_prototype.ui.select_mode

import android.os.Bundle
import android.view.View
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

            btnSubmit.setOnClickListener {
                findNavController().navigate(
                    SelectModeFragmentDirections.actionOpenSceneFragment(viewModel.settings)
                )
            }
        }
    }
}
