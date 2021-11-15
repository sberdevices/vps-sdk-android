package com.arvrlab.vps_sdk.ui

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.SparseArray
import androidx.annotation.MainThread
import androidx.core.util.containsKey
import com.arvrlab.vps_sdk.data.model.CameraIntrinsics
import com.arvrlab.vps_sdk.domain.model.NodePoseModel
import com.arvrlab.vps_sdk.util.Constant.QUALITY
import com.arvrlab.vps_sdk.util.getEulerAngles
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Matrix
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

    private val poseInModelNode: Node by lazy {
        Node()
    }

    fun bindArSceneView(arSceneView: ArSceneView) {
        this.arSceneView = arSceneView
    }

    fun destroy() {
        worldNode.renderable = null
        scene.removeChild(worldNode)
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
            scene.addChild(worldNode)
        }

        val cameraPrevPoseMatrix = getCameraPrevPoseMatrix(cameraPrevPosition, cameraPrevRotation)
        val nodePoseMatrix = getNodePoseMatrix(nodePose)

        updateWorldNodePose(cameraPrevPoseMatrix, nodePoseMatrix)
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

    fun getCameraIntrinsics(): CameraIntrinsics {
        val camera = arSceneView?.arFrame?.camera ?: return CameraIntrinsics.EMPTY

        val principalPoint = camera.imageIntrinsics.principalPoint
        val focalLength = camera.imageIntrinsics.focalLength

        return CameraIntrinsics(
            fx = focalLength[0],
            fy = focalLength[1],
            cx = principalPoint[0],
            cy = principalPoint[1]
        )
    }

    private fun checkArSceneView(): ArSceneView =
        checkNotNull(arSceneView) { "ArSceneView is null. Call bindArSceneView(ArSceneView)" }

    private fun getCameraPrevPoseMatrix(position: Vector3, rotation: Quaternion): Matrix =
        Matrix().apply {
            makeTrs(position, rotation.alignHorizontal(), Vector3.one())
        }

    private fun getNodePoseMatrix(nodePose: NodePoseModel): Matrix =
        Matrix().apply {
            val positionMatrix = Matrix()
                .apply { makeTranslation(nodePose.getPosition()) }
            val rotationMatrix = Matrix()
                .apply { makeRotation(nodePose.getRotation().alignHorizontal()) }

            Matrix.multiply(rotationMatrix, positionMatrix, this)
        }

    private fun updateWorldNodePose(cameraPrevPoseMatrix: Matrix, nodePoseMatrix: Matrix) {
        val worldPoseMatrix = Matrix()
        Matrix.multiply(cameraPrevPoseMatrix, nodePoseMatrix, worldPoseMatrix)

        val rotation = Quaternion()
        worldPoseMatrix.decomposeRotation(Vector3.one(), rotation)
        worldNode.localRotation = rotation
        val translation = Vector3()
        worldPoseMatrix.decomposeTranslation(translation)
        worldNode.localPosition = translation
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

    private fun Quaternion.alignHorizontal(): Quaternion {
        val forward = Vector3.forward()
        val dir = Quaternion.rotateVector(this, forward)
        dir.y = 0f
        return Quaternion.rotationBetweenVectors(forward, dir)
    }

    private data class CameraPose(
        val cameraPrevPosition: Vector3,
        val cameraPrevRotation: Quaternion,
    )

}