package com.arvrlab.vps_sdk.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow

internal class VpsArView : FrameLayout {

    private var vpsArFragment: VpsArFragment? = null

    val vpsService: VpsService
        get() = checkNotNull(vpsArFragment).vpsService

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
        val vpsArFragment = VpsArFragment()

        activity?.let {
            it.supportFragmentManager.commitNow(true) {
                add(id, vpsArFragment, VpsArFragment::class.java.toString())
            }
        } ?: throw IllegalStateException("VpsArView must be within a FragmentActivity")

        this.vpsArFragment = vpsArFragment
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        if (vpsArFragment?.view === child) {
            super.addView(child, index, params)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        val vpsArFragment = vpsArFragment ?: return
        activity?.supportFragmentManager?.commitNow(true) {
            remove(vpsArFragment)
        }
    }

}