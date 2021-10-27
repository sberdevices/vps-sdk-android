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
import com.arvrlab.vps_sdk.util.getEulerAngles
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

    private val alternativeCamera: Node by lazy {
        Node()
    }
    private val rotationNode: Node by lazy {
        Node()
    }
    private val translationNode: Node by lazy {
        Node()
    }

    private val positionInModelNode: Node by lazy {
        Node()
    }

    fun bindArSceneView(arSceneView: ArSceneView) {
        this.arSceneView = arSceneView
    }

    fun destroy() {
        modelNode.renderable = null
        translationNode.removeChild(modelNode)
        rotationNode.removeChild(translationNode)
        alternativeCamera.removeChild(rotationNode)
        scene.removeChild(alternativeCamera)
        arSceneView = null
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

        val (cameraPrevPosition, cameraPrevRotation) = tempWorldPosition[index]
        tempWorldPosition.clear()

        if (modelNode.parent == null) {
            translationNode.addChild(modelNode)
            rotationNode.addChild(translationNode)
            alternativeCamera.addChild(rotationNode)
            scene.addChild(alternativeCamera)
        }
        alternativeCamera.localRotation = cameraPrevRotation
            .alignHorizontal()
        alternativeCamera.localPosition = cameraPrevPosition

        rotationNode.localRotation = nodePosition.getRotation()
            .alignHorizontal()
        translationNode.localPosition = nodePosition.getPosition()
    }

    @MainThread
    fun getWorldNodePosition(): NodePositionModel {
        positionInModelNode.worldPosition = camera.worldPosition
        positionInModelNode.worldRotation = camera.worldRotation

        val localPosition = positionInModelNode.localPosition
        val localRotation = positionInModelNode.localRotation
            .getEulerAngles()

        return NodePositionModel(
            x = localPosition.x,
            y = localPosition.y,
            z = localPosition.z,
            roll = localRotation.x,
            pitch = localRotation.y,
            yaw = localRotation.z,
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

    private fun Quaternion.alignHorizontal(): Quaternion =
        this.apply {
            x = 0f
            z = 0f
        }

    private data class WorldPosition(
        val cameraStartPosition: Vector3,
        val cameraStartRotation: Quaternion,
    )

}