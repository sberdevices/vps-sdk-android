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
import com.arvrlab.vps_sdk.util.getQuaternion
import com.arvrlab.vps_sdk.util.getTranslation
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.utilities.AndroidPreconditions
import java.io.ByteArrayOutputStream

internal class ArManager : Scene.OnUpdateListener {

    private companion object {
        const val SMOOTH_RATIO = 0.5f
    }

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

    private val worldPoseMatrix: Matrix = Matrix()
    private val poseInModelNode: Node by lazy {
        Node()
    }

    fun bindArSceneView(arSceneView: ArSceneView) {
        this.arSceneView = arSceneView
        scene.addOnUpdateListener(this)
    }

    override fun onUpdate(frameTime: FrameTime?) {
        val newPosition = worldPoseMatrix.getTranslation()
        val newQuaternion = worldPoseMatrix.getQuaternion()

        if (!worldNode.localPosition.equals(newPosition) ||
            !worldNode.localRotation.equals(newQuaternion)
        ) {
            updateWorldNodePose(
                Quaternion.slerp(worldNode.localRotation, newQuaternion, SMOOTH_RATIO),
                Vector3.lerp(worldNode.localPosition, newPosition, SMOOTH_RATIO)
            )
        }
    }

    fun destroy() {
        worldNode.renderable = null
        scene.removeChild(worldNode)
        scene.removeOnUpdateListener(this)
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

        var isFirstRestore = false

        if (!scene.children.contains(worldNode)) {
            isFirstRestore = true
            scene.addChild(worldNode)
        }

        val cameraPrevPoseMatrix = getCameraPrevPoseMatrix(cameraPrevPosition, cameraPrevRotation)
        val nodePoseMatrix = getNodePoseMatrix(nodePose)
        Matrix.multiply(cameraPrevPoseMatrix, nodePoseMatrix, worldPoseMatrix)

        if (isFirstRestore) {
            updateWorldNodePose(
                worldPoseMatrix.getQuaternion(),
                worldPoseMatrix.getTranslation()
            )
        }
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

    private fun updateWorldNodePose(rotation: Quaternion, position: Vector3) {
        worldNode.localRotation = rotation
        worldNode.localPosition = position
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