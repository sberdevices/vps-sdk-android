package com.arvrlab.vps_android_prototype.ui.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.RawRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.arvrlab.vps_android_prototype.R
import com.arvrlab.vps_android_prototype.databinding.FmtSceneBinding
import com.arvrlab.vps_android_prototype.databinding.MenuSceneBinding
import com.arvrlab.vps_android_prototype.util.Logger
import com.arvrlab.vps_sdk.data.MobileVps
import com.arvrlab.vps_sdk.data.Photo
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.ui.VpsArFragment
import com.arvrlab.vps_sdk.ui.VpsCallback
import com.arvrlab.vps_sdk.ui.VpsService
import com.google.ar.core.Config.FocusMode.*
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import kotlinx.coroutines.delay

abstract class SceneFragment : Fragment(R.layout.fmt_scene), VpsCallback {

    private companion object {
        const val INDICATOR_COLOR_DELAY = 1000L
    }

    abstract var vpsConfig: VpsConfig

    protected var occluderEnable: Boolean = false
        private set

    protected val binding by viewBinding(FmtSceneBinding::bind)

    protected val vpsArFragment: VpsArFragment
        get() = childFragmentManager.findFragmentById(binding.vFragmentContainer.id) as VpsArFragment

    protected val vpsService: VpsService
        get() = vpsArFragment.vpsService

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.vTouchZone.setOnLongClickListener {
            showMenu()
            true
        }

        initVpsService()

        vpsService.startVpsService()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vpsService.destroy()
    }

    override fun onSuccess() {
        updateVpsStatus(true)
    }

    override fun onFail() {
        updateVpsStatus(false)
    }

    override fun onStateChange(state: VpsService.State) {
        Logger.debug("VPS service: $state")
    }

    override fun onError(error: Throwable) {
        Logger.error(error)
        showError(error)
    }

    protected fun loadModel(@RawRes rawRes: Int, completeCallback: (Renderable) -> Unit) {
        ModelRenderable.builder()
            .setSource(context, rawRes)
            .setIsFilamentGltf(true)
            .build()
            .thenApply(completeCallback)
            .exceptionally { Logger.error(it) }
    }

    protected abstract fun updateOccluderState()

    private fun showMenu() {
        val menuBinding = MenuSceneBinding.inflate(layoutInflater)

        menuBinding.cbAutofocus.isChecked = vpsArFragment.isAutofocus()
        menuBinding.cbSerialPhotos.isChecked = vpsConfig.useSerialImages
        menuBinding.cbMobileVps.isChecked = vpsConfig.localizationType is MobileVps
        menuBinding.cbAlwaysForce.isChecked = vpsConfig.onlyForce
        menuBinding.cbGps.isChecked = vpsConfig.useGps
        menuBinding.cbOccluder.isChecked = occluderEnable

        AlertDialog.Builder(requireContext())
            .setView(menuBinding.root)
            .setPositiveButton(R.string.apply) { _, _ ->
                vpsArFragment.setAutofocus(menuBinding.cbAutofocus.isChecked)
                occluderEnable = menuBinding.cbOccluder.isChecked

                restartVpsService(
                    menuBinding.cbSerialPhotos.isChecked,
                    menuBinding.cbMobileVps.isChecked,
                    menuBinding.cbAlwaysForce.isChecked,
                    menuBinding.cbGps.isChecked,
                )
            }
            .show()
    }

    private fun initVpsService() {
        with(vpsService) {
            setVpsCallback(this@SceneFragment)
            setVpsConfig(vpsConfig)
        }
    }

    private fun restartVpsService(
        serialPhotosEnable: Boolean,
        mobileVpsEnable: Boolean,
        alwaysForceEnable: Boolean,
        gpsEnable: Boolean,
    ) {
        vpsService.stopVpsService()

        vpsConfig = vpsConfig.copy(
            localizationType = if (mobileVpsEnable) MobileVps() else Photo,
            useSerialImages = serialPhotosEnable,
            onlyForce = alwaysForceEnable,
            useGps = gpsEnable
        )
        vpsService.setVpsConfig(vpsConfig)
        updateOccluderState()

        vpsService.startVpsService()
    }

    private fun updateVpsStatus(isSuccess: Boolean) {
        val indicatorColor = if (isSuccess) Color.GREEN else Color.RED
        binding.vIndicator.background.setTint(indicatorColor)

        lifecycleScope.launchWhenCreated {
            delay(INDICATOR_COLOR_DELAY)
            binding.vIndicator.background.setTint(Color.WHITE)
        }
    }

    private fun showError(e: Throwable) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(e.toString())
            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

}