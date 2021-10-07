package com.arvrlab.vps_android_prototype.ui.select_mode

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
            val imagesCountTextWatcher = object : SimpleTextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.onImagesCountChanged(s.toString())
                }
            }
            val imagesIntervalTextWatcher = object : SimpleTextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.onImagesIntervalChanged(s.toString())
                }
            }
            cbSerialImages.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    etImagesCount.addTextChangedListener(imagesCountTextWatcher)
                    viewModel.onImagesCountChanged(etImagesCount.text.toString())
                    etImagesInterval.addTextChangedListener(imagesIntervalTextWatcher)
                    viewModel.onImagesIntervalChanged(etImagesInterval.text.toString())
                } else {
                    etImagesCount.removeTextChangedListener(imagesCountTextWatcher)
                    viewModel.onImagesCountChanged("1")
                    etImagesInterval.removeTextChangedListener(imagesIntervalTextWatcher)
                }
                tilImagesCount.isVisible = isChecked
                tilImagesInterval.isVisible = isChecked
            }

            btnSubmit.setOnClickListener {
                findNavController().navigate(
                    SelectModeFragmentDirections.actionOpenSceneFragment(viewModel.sceneModel)
                )
            }
        }
    }

    private interface SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?)
    }

}
