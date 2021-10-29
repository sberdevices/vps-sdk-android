package com.arvrlab.vps_sdk.ui

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.SparseArray
import androidx.annotation.MainThread
import androidx.core.util.containsKey
import com.arvrlab.vps_sdk.domain.model.NodePoseModel
import com.arvrlab.vps_sdk.util.Constant.QUALITY
import com.arvrlab.vps_sdk.util.getEulerAngles
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.utilities.AndroidPreconditions
import java.io.ByteArrayOutputStream

internal class ArManager {

    val worldNode: Node by lazy {
        Node()
            .also { it.addChild(poseInModelNode) }
    }

    private var arSceneView: ArSceneView? = null

    private val scene: Scene
        get() = checkArSceneView().scene

    private val camera: Camera
        get() = scene.camera

    private var tempCameraPose: SparseArray<CameraPose> = SparseArray(1)

    private val cameraPrevPoseNode: Node by lazy {
        Node()
    }
    private val rotationNode: Node by lazy {
        Node()
    }
    private val translationNode: Node by lazy {
        Node()
    }

    private val poseInModelNode: Node by lazy {
        Node()
    }

    fun bindArSceneView(arSceneView: ArSceneView) {
        this.arSceneView = arSceneView
    }

    fun destroy() {
        worldNode.renderable = null
        translationNode.removeChild(worldNode)
        rotationNode.removeChild(translationNode)
        cameraPrevPoseNode.removeChild(rotationNode)
        scene.removeChild(cameraPrevPoseNode)
        arSceneView = null
    }

    /**
    * index для локализации по одному фото всегда 0,
    * для локализации по серии фото - передается порядковый номер фото, начиная с 0
    */
    fun saveCameraPose(index: Int) {
        tempCameraPose.put(
            index,
            CameraPose(
                cameraPrevPosition = camera.worldPosition,
                cameraPrevRotation = camera.worldRotation,
            )
        )
    }

    /**
     * index для локализации по одному фото всегда 0,
     * для локализации по серии фото - используется индекс, который вернул сервер
     */
    fun restoreCameraPose(index: Int, nodePose: NodePoseModel) {
        if (!tempCameraPose.containsKey(index))
            throw IllegalStateException("WorldPosition with index $index not found")

        val (cameraPrevPosition, cameraPrevRotation) = tempCameraPose[index]
        tempCameraPose.clear()

        if (worldNode.parent == null) {
            translationNode.addChild(worldNode)
            rotationNode.addChild(translationNode)
            cameraPrevPoseNode.addChild(rotationNode)
            scene.addChild(cameraPrevPoseNode)
        }
        cameraPrevPoseNode.localRotation = cameraPrevRotation
            .alignHorizontal()
        cameraPrevPoseNode.localPosition = cameraPrevPosition

        rotationNode.localRotation = nodePose.getRotation()
            .alignHorizontal()
        translationNode.localPosition = nodePose.getPosition()
    }

    @MainThread
    fun getCameraLocalPose(): NodePoseModel {
        poseInModelNode.worldPosition = camera.worldPosition
        poseInModelNode.worldRotation = camera.worldRotation

        val localPosition = poseInModelNode.localPosition
        val localRotation = poseInModelNode.localRotation
            .getEulerAngles()

        return NodePoseModel(
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

    //TODO: также добавить нормализацию(Quaternion.normalized()) и проерить его работу
    private fun Quaternion.alignHorizontal(): Quaternion =
        this.apply {
            x = 0f
            z = 0f
        }

    private data class CameraPose(
        val cameraPrevPosition: Vector3,
        val cameraPrevRotation: Quaternion,
    )

}