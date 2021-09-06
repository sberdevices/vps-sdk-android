package com.arvrlab.vps_sdk.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RawRes
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import com.arvrlab.vps_sdk.service.Settings
import com.arvrlab.vps_sdk.service.VpsCallback
import com.arvrlab.vps_sdk.util.Logger
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.ModelRenderable
import java.util.concurrent.CompletableFuture

class VpsArView : FrameLayout {

    private lateinit var vpsArFragment: VpsArFragment
    private lateinit var modelNode: Node

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
            it.supportFragmentManager.commitNow {
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

    fun initVpsService(
        @RawRes model: Int,
        callback: VpsCallback,
        settings: Settings
    ): CompletableFuture<Void> {
        return ModelRenderable.builder()
            .setSource(context, model)
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { renderable ->
                modelNode = AnchorNode().also {
                    it.renderable = renderable
                }
                vpsArFragment.initVpsService(positionNode = modelNode, callback = callback, settings = settings)
            }
            .exceptionally { error ->
                Logger.error(error)
                return@exceptionally null
            }
    }

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
        vpsArFragment.destroy()
    }

    fun setArAlpha(alpha: Float) {
        val engine = EngineInstance.getEngine().filamentEngine
        val renderableManager = engine.renderableManager

        modelNode.renderableInstance?.filamentAsset?.let { asset ->
            for (entity in asset.entities) {
                val renderable = renderableManager.getInstance(entity)
                if (renderable != 0) {
                    val r = 7f / 255
                    val g = 7f / 225
                    val b = 143f / 225
                    val materialInstance = renderableManager.getMaterialInstanceAt(renderable, 0)
                    materialInstance.setParameter("baseColorFactor", r, g, b, alpha)
                }
            }
        }
    }

}