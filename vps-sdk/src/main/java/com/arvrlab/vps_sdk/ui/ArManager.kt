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
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.utilities.AndroidPreconditions
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

internal class ArManager {

    private companion object {
        const val PI_DIV_180 = PI / 180f
        const val _180_DIV_PI = 180 / PI
        const val PI_HALF = PI / 2
    }

    val worldNode: Node by lazy {
        AnchorNode()
    }

    private var arSceneView: ArSceneView? = null

    private val scene: Scene
        get() = checkArSceneView().scene

    private val camera: Camera
        get() = scene.camera

    private var tempWorldPosition: SparseArray<WorldPosition> = SparseArray(1)
    private var cameraStartPosition: Vector3 = Vector3()
    private var cameraStartRotation: Quaternion = Quaternion()
    private var photoMatrix: Matrix? = null

    private var rotationAngle: Float? = null

    private var isModelCreated: Boolean = false

    private val rotationNode: Node by lazy {
        AnchorNode()
            .also { it.addChild(worldNode) }
    }
    private val cameraAlternativeNode: Node by lazy {
        AnchorNode()
            .also { it.addChild(rotationNode) }
    }

    fun bindArSceneView(arSceneView: ArSceneView) {
        this.arSceneView = arSceneView
    }

    fun unbind() {
        this.arSceneView = null
    }

    fun destroy() {
        unbind()
        rotationNode.setParent(null)
        cameraAlternativeNode.setParent(null)
        worldNode.renderable = null

        isModelCreated = false
    }

    fun saveWorldPosition(index: Int) {
        tempWorldPosition.put(
            index,
            WorldPosition(
                cameraStartPosition = camera.worldPosition,
                cameraStartRotation = camera.worldRotation,
                photoMatrix = camera.worldModelMatrix
            )
        )
    }

    fun restoreWorldPosition(index: Int, nodePosition: NodePositionModel) {
        if (!tempWorldPosition.containsKey(index))
            throw IllegalStateException("WorldPosition with index $index not found")

        tempWorldPosition[index].let {
            cameraStartPosition = it.cameraStartPosition
            cameraStartRotation = it.cameraStartRotation
            photoMatrix = it.photoMatrix
        }
        tempWorldPosition.clear()

        updateRotationAngle(nodePosition)

        createNodeHierarchyIfNeed()

        cameraAlternativeNode.worldRotation = cameraStartRotation.toStartRotation()
        cameraAlternativeNode.worldPosition = cameraStartPosition

        rotationNode.localRotation = nodePosition.toQuaternion()
        worldNode.localPosition = nodePosition.toVector3()
    }

    @MainThread
    fun getWorldNodePosition(lastNodePosition: NodePositionModel): NodePositionModel {
        val newCamera = Node()
            .apply {
                worldPosition = camera.worldPosition
                worldRotation = camera.worldRotation
            }

        AnchorNode().apply {
            addChild(newCamera)
            worldRotation = Quaternion(Vector3(0f, rotationAngle ?: 0f, 0f))
        }

        val newPos = Vector3(
            lastNodePosition.x,
            lastNodePosition.y,
            lastNodePosition.z
        )

        val newAngle = Node()
            .apply {
                worldPosition = newCamera.worldPosition
                worldRotation = newCamera.worldRotation
            }
            .localRotation
            .toEulerAngles()

        return NodePositionModel(
            x = newPos.x,
            y = newPos.y,
            z = newPos.z,
            roll = newAngle.x,
            pitch = newAngle.z,
            yaw = newAngle.y,
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

    private fun updateRotationAngle(nodePosition: NodePositionModel) {
        val anglesY = Quaternion(
            Vector3(
                (nodePosition.roll * PI_DIV_180).toFloat(),
                (nodePosition.yaw * PI_DIV_180).toFloat(),
                (nodePosition.pitch * PI_DIV_180).toFloat()
            )
        ).toEulerAngles().y
        val cameraAngles = Quaternion(photoMatrix?.toPositionVector()).toEulerAngles().y

        rotationAngle = anglesY + cameraAngles
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
        val dir = Quaternion.rotateVector(this, Vector3(0f, 0f, 1f))
        dir.y = 0f
        return Quaternion.rotationBetweenVectors(Vector3(0f, 0f, 1f), dir)
    }

    private fun NodePositionModel.toVector3(): Vector3 =
        Vector3(-this.x, -this.y, -this.z)

    private fun NodePositionModel.toQuaternion(): Quaternion =
        Quaternion(Vector3(0f, if (yaw > 0) 180f - yaw else yaw, 0f))
            .inverted()

    private fun Matrix.toPositionVector(): Vector3 =
        Vector3()
            .also { decomposeTranslation(it) }

    //x y z => roll yaw pitch
    private fun Quaternion.toEulerAngles(): Vector3 {
        val test = x * y + z * w
        if (test > 0.499) { // singularity at north pole
            val y = 2 * atan2(x, w)
            val z = PI_HALF
            val x = 0f
            return Vector3(x, y, z.toFloat())
        }
        if (test < -0.499) { // singularity at south pole
            val y = -2 * atan2(x, w)
            val z = -PI_HALF
            val x = 0f
            return Vector3(x, y, z.toFloat())
        }
        val sqx = x * x
        val sqy = y * y
        val sqz = z * z
        val y = atan2(2 * y * w - 2 * x * z, 1 - 2 * sqy - 2 * sqz)
        val z = asin(2 * test)
        val x = atan2(2 * x * w - 2 * y * z, 1 - 2 * sqx - 2 * sqz)

        return Vector3(
            (x * _180_DIV_PI).toFloat(),
            (y * _180_DIV_PI).toFloat(),
            (z * _180_DIV_PI).toFloat()
        )
    }

    private class WorldPosition(
        val cameraStartPosition: Vector3 = Vector3(),
        val cameraStartRotation: Quaternion = Quaternion(),
        val photoMatrix: Matrix? = null
    )

}