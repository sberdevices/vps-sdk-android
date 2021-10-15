package com.arvrlab.vps_sdk.ui

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.SparseArray
import androidx.annotation.MainThread
import androidx.core.util.containsKey
import com.arvrlab.vps_sdk.domain.model.NodePositionModel
import com.arvrlab.vps_sdk.util.Constant.QUALITY
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.utilities.AndroidPreconditions
import java.io.ByteArrayOutputStream

internal class ArManager {

    val modelNode: AnchorNode by lazy {
        AnchorNode()
            .also { it.addChild(positionInModelNode) }
    }

    private var arSceneView: ArSceneView? = null

    private val scene: Scene
        get() = checkArSceneView().scene

    private val camera: Camera
        get() = scene.camera

    private var tempWorldPosition: SparseArray<WorldPosition> = SparseArray(1)
    private var cameraPrevPosition: Vector3 = Vector3()
    private var cameraPrevRotation: Quaternion = Quaternion()

    private var isModelCreated: Boolean = false

    private val rotationNode: AnchorNode by lazy {
        AnchorNode()
            .also { it.addChild(modelNode) }
    }
    private val cameraAlternativeNode: AnchorNode by lazy {
        AnchorNode()
            .also { it.addChild(rotationNode) }
    }
    private val positionInModelNode: Node by lazy {
        Node()
    }

    fun bindArSceneView(arSceneView: ArSceneView) {
        this.arSceneView = arSceneView
    }

    fun destroy() {
        rotationNode.removeChild(modelNode)
        cameraAlternativeNode.removeChild(rotationNode)
        scene.removeChild(cameraAlternativeNode)
        modelNode.renderable = null
        arSceneView = null

        isModelCreated = false
    }

    fun saveWorldPosition(index: Int) {
        tempWorldPosition.put(
            index,
            WorldPosition(
                cameraStartPosition = camera.worldPosition,
                cameraStartRotation = camera.worldRotation,
            )
        )
    }

    fun restoreWorldPosition(index: Int, nodePosition: NodePositionModel) {
        if (!tempWorldPosition.containsKey(index))
            throw IllegalStateException("WorldPosition with index $index not found")

        tempWorldPosition[index].let {
            cameraPrevPosition = it.cameraStartPosition
            cameraPrevRotation = it.cameraStartRotation
        }
        tempWorldPosition.clear()

        createNodeHierarchyIfNeed()

        cameraAlternativeNode.worldPosition = cameraPrevPosition
        cameraAlternativeNode.worldRotation = cameraPrevRotation.toStartRotation()

        rotationNode.localRotation = nodePosition.toQuaternion()
        modelNode.localPosition = nodePosition.toVector3()
    }

    @MainThread
    fun getWorldNodePosition(): NodePositionModel {
        with(positionInModelNode) {
            worldPosition = camera.worldPosition
            worldRotation = camera.worldRotation
        }

        val localPosition = positionInModelNode.localPosition
        val localRotation = positionInModelNode.localRotation

        return NodePositionModel(
            x = localPosition.x,
            y = localPosition.y,
            z = localPosition.z,
            roll = localRotation.x,
            pitch = localRotation.z,
            yaw = localRotation.y,
        )
    }

    @MainThread
    fun acquireCameraImageAsByteArray(): ByteArray {
        AndroidPreconditions.checkUiThread()
        val image = checkArSceneView().arFrame?.acquireCameraImage()
            ?: throw IllegalStateException("Frame is null")
        return image.toByteArray()
            .also { image.close() }
    }

    private fun checkArSceneView(): ArSceneView =
        checkNotNull(arSceneView) { "ArSceneView is null. Call bindArSceneView(ArSceneView)" }

    private fun createNodeHierarchyIfNeed() {
        if (isModelCreated) return
        isModelCreated = true

        scene.addChild(cameraAlternativeNode)
    }

    private fun Image.toByteArray(): ByteArray {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        return ByteArrayOutputStream().use { out ->
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), QUALITY, out)
            out.toByteArray()
        }
    }

    private fun Quaternion.toStartRotation(): Quaternion {
        val dir = Quaternion.rotateVector(this, Vector3.back())
        dir.y = 0f
        return Quaternion.rotationBetweenVectors(Vector3.back(), dir)
    }

    private fun NodePositionModel.toVector3(): Vector3 =
        Vector3(-this.x, -this.y, -this.z)

    private fun NodePositionModel.toQuaternion(): Quaternion =
        Quaternion(Vector3(0f, if (yaw > 0) 180f - yaw else yaw, 0f))
            .inverted()

    private class WorldPosition(
        val cameraStartPosition: Vector3 = Vector3(),
        val cameraStartRotation: Quaternion = Quaternion(),
    )

}