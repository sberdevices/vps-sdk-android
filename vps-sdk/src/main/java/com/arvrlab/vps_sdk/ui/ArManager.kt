package com.arvrlab.vps_sdk.ui

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.SparseArray
import androidx.annotation.MainThread
import androidx.core.util.containsKey
import com.arvrlab.vps_sdk.data.VpsConfig
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
        const val MS_IN_SEC = 1000f
        const val MAX_RATIO = 1f
        const val TIMER_DISABLE = -1f

        const val DISTANCE_LIMIT = 2f
        const val ANGLE_LIMIT = 10f
        val FORWARD: Vector3 = Vector3.forward()
    }

    val worldNode: Node by lazy {
        Node()
            .also { it.addChild(poseInWorldNode) }
    }

    private var arSceneView: ArSceneView? = null

    private val scene: Scene
        get() = checkArSceneView().scene

    private val camera: Camera
        get() = scene.camera

    private var tempCameraPose: SparseArray<CameraPose> = SparseArray(1)

    private val prevWorldPoseMatrix: Matrix = Matrix()
    private val nextWorldPoseMatrix: Matrix = Matrix()
    private val poseInWorldNode: Node by lazy {
        Node()
    }

    private var worldInterpolationDuration: Float = 1f
    private var worldInterpolationTimer: Float = TIMER_DISABLE

    fun init(arSceneView: ArSceneView, vpsConfig: VpsConfig) {
        this.arSceneView = arSceneView
        this.worldInterpolationDuration = minOf(
            vpsConfig.worldInterpolationDurationMS,
            vpsConfig.intervalLocalizationMS
        ) / MS_IN_SEC

        scene.addOnUpdateListener(this)
    }

    override fun onUpdate(frameTime: FrameTime) {
        updateWorldNodePose(frameTime.deltaSeconds)
    }

    fun destroy() {
        worldNode.renderable = null
        scene.removeChild(worldNode)
        scene.removeOnUpdateListener(this)
        arSceneView = null
        worldInterpolationTimer = -1f
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

        if (!scene.children.contains(worldNode)) {
            scene.addChild(worldNode)
        }

        val cameraPrevPoseMatrix = getCameraPrevPoseMatrix(cameraPrevPosition, cameraPrevRotation)
        val nodePoseMatrix = getNodePoseMatrix(nodePose)
        Matrix.multiply(cameraPrevPoseMatrix, nodePoseMatrix, nextWorldPoseMatrix)
        prevWorldPoseMatrix.set(worldNode.worldModelMatrix)

        worldInterpolationTimer = 0f
    }

    @MainThread
    fun getCameraLocalPose(): NodePoseModel {
        poseInWorldNode.worldPosition = camera.worldPosition
        poseInWorldNode.worldRotation = camera.worldRotation

        val localPosition = poseInWorldNode.localPosition
        val localRotation = poseInWorldNode.localRotation
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

    private fun updateWorldNodePose(deltaTime: Float) {
        if (worldInterpolationTimer == TIMER_DISABLE) return

        val ratio = minOf(MAX_RATIO, worldInterpolationTimer / worldInterpolationDuration)
        if (worldInterpolationTimer > worldInterpolationDuration) {
            worldInterpolationTimer = TIMER_DISABLE
        } else {
            worldInterpolationTimer += deltaTime
        }

        updateWorldNodePosition(ratio)
        updateWorldNodeRotation(ratio)
    }

    private fun updateWorldNodePosition(ratio: Float) {
        val newPosition = nextWorldPoseMatrix.getTranslation()

        if (!worldNode.localPosition.equals(newPosition)) {
            val prevPosition = prevWorldPoseMatrix.getTranslation()
            worldNode.localPosition = if (length(prevPosition, newPosition) < DISTANCE_LIMIT) {
                Vector3.lerp(prevPosition, newPosition, ratio)
            } else {
                newPosition
            }
        }
    }

    private fun updateWorldNodeRotation(ratio: Float) {
        val newRotation = nextWorldPoseMatrix.getQuaternion()

        if (!worldNode.localRotation.equals(newRotation)) {
            val prevRotation = prevWorldPoseMatrix.getQuaternion()
            worldNode.localRotation = if (length(prevRotation, newRotation) < ANGLE_LIMIT) {
                Quaternion.slerp(prevRotation, newRotation, ratio)
            } else {
                newRotation
            }
        }
    }

    private fun length(lhs: Vector3, rhs: Vector3): Float =
        Vector3.subtract(lhs, rhs).length()

    private fun length(lhs: Quaternion, rhs: Quaternion): Float =
        Vector3.angleBetweenVectors(
            Quaternion.rotateVector(lhs, FORWARD),
            Quaternion.rotateVector(rhs, FORWARD)
        )

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
        val dir = Quaternion.rotateVector(this, FORWARD)
        dir.y = 0f
        return Quaternion.rotationBetweenVectors(FORWARD, dir)
    }

    private data class CameraPose(
        val cameraPrevPosition: Vector3,
        val cameraPrevRotation: Quaternion,
    )

}