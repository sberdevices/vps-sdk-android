package com.arvrlab.vps_android_prototype.screens.selectmode.fragment

import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.databinding.SelectFragmentBinding
import com.arvrlab.vps_android_prototype.screens.selectmode.viewmodel.SelectViewModel

class SelectFragment : Fragment(R.layout.select_fragment) {

    private val binding by viewBinding(SelectFragmentBinding::bind)
    private val viewModel: SelectViewModel by viewModels()

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
                    SelectFragmentDirections.actionSelectFragmentToSceneformFragment(
                        viewModel.settings
                    )
                )
            }
        }
    }
}
