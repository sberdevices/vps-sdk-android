package com.arvrlab.vps_sdk.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RawRes
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.service.VpsCallback
import java.util.concurrent.CompletableFuture

class VpsArView : FrameLayout {

    private lateinit var vpsArFragment: VpsArFragment

    private val activity: FragmentActivity?
        get() = context as? FragmentActivity

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (id == View.NO_ID) {
            throw IllegalStateException("Must set android:id")
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        vpsArFragment = VpsArFragment()

        activity?.let {
            it.supportFragmentManager.commitNow(true) {
                add(id, vpsArFragment, VpsArFragment::class.java.toString())
            }
        } ?: throw IllegalStateException("VpsArView must be within a FragmentActivity")
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        if (::vpsArFragment.isInitialized && vpsArFragment.view === child) {
            super.addView(child, index, params)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        activity?.supportFragmentManager?.commitNow(true) {
            remove(vpsArFragment)
        }
    }

    fun configureVpsService(vpsConfig: VpsConfig, vpsCallback: VpsCallback) {
        vpsArFragment.configureVpsService(vpsConfig, vpsCallback)
    }

    fun loadModelByRawId(@RawRes rawRes: Int): CompletableFuture<Unit> =
        vpsArFragment.loadModelByRawId(rawRes)

    fun startVpsService() {
        vpsArFragment.startVpsService()
    }

    fun stopVpsService() {
        vpsArFragment.stopVpsService()
    }

    fun pause() {
        vpsArFragment.stopVpsService()
    }

    fun destroy() {
        vpsArFragment.onDestroy()
    }

    fun setArAlpha(alpha: Float) {
        vpsArFragment.setArAlpha(alpha)
    }

}