package com.arvrlab.vps_sdk.ui

import android.media.Image
import androidx.annotation.MainThread
import com.arvrlab.vps_sdk.domain.model.NodePositionModel
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.utilities.AndroidPreconditions
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
        cameraAlternativeNode.setParent(null)
        rotationNode.setParent(null)
        cameraAlternativeNode.setParent(null)
        worldNode.renderable = null

        isModelCreated = false
    }

    fun savePositions() {
        cameraStartPosition = camera.worldPosition
        cameraStartRotation = camera.worldRotation
        photoMatrix = camera.worldModelMatrix
    }

    fun updatePositions(nodePosition: NodePositionModel) {
        updateRotationAngle(nodePosition)

        createNodeHierarchyIfNeed()

        cameraAlternativeNode.worldRotation = cameraStartRotation.toStartRotation()
        cameraAlternativeNode.worldPosition = cameraStartPosition.also { it.z = 0f }

        rotationNode.localRotation = nodePosition.toQuaternion()
        worldNode.localPosition = nodePosition.toVector3()
    }

    fun getCurrentNodePosition(lastNodePosition: NodePositionModel): NodePositionModel {
        val lastCamera = Node()
        val newCamera = Node()
        val anchorParent = AnchorNode()

        lastCamera.worldPosition = photoMatrix?.toPositionVector()
        newCamera.worldPosition = camera.worldPosition
        newCamera.worldRotation = camera.worldRotation

        with(anchorParent) {
            addChild(lastCamera)
            addChild(newCamera)
            worldRotation = Quaternion(Vector3(0f, rotationAngle ?: 0f, 0f))
        }

        val correct = Vector3(
            newCamera.worldPosition.x - lastCamera.worldPosition.x,
            newCamera.worldPosition.y - lastCamera.worldPosition.y,
            newCamera.worldPosition.z - lastCamera.worldPosition.z
        )

        val newPos = Vector3(
            lastNodePosition.x + correct.x,
            lastNodePosition.y + correct.y,
            lastNodePosition.z + correct.z
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
    fun acquireCameraImage(): Image {
        AndroidPreconditions.checkUiThread()
        return checkArSceneView().arFrame?.acquireCameraImage()
            ?: throw IllegalStateException("Frame is null")
    }

    private fun checkArSceneView(): ArSceneView =
        checkNotNull(arSceneView) { "ArSceneView is null. Call bindArSceneView(ArSceneView)" }

    private fun createNodeHierarchyIfNeed() {
        if (isModelCreated) return
        isModelCreated = true

        scene.addChild(cameraAlternativeNode)
    }

    @Suppress("LocalVariableName")
    private fun updateRotationAngle(nodePosition: NodePositionModel?) {
        val _nodePosition = nodePosition ?: NodePositionModel()
        val anglesY = Quaternion(
            Vector3(
                (_nodePosition.roll * PI_DIV_180).toFloat(),
                (_nodePosition.yaw * PI_DIV_180).toFloat(),
                (_nodePosition.pitch * PI_DIV_180).toFloat()
            )
        ).toEulerAngles().y
        val cameraAngles = Quaternion(photoMatrix?.toPositionVector()).toEulerAngles().y

        rotationAngle = anglesY + cameraAngles
    }

    private fun Quaternion.toStartRotation(): Quaternion {
        val dir = Quaternion.rotateVector(this, Vector3(0f, 0f, 1f))
        dir.y = 0f
        return Quaternion.rotationBetweenVectors(Vector3(0f, 0f, 1f), dir)
    }

    private fun NodePositionModel.toVector3(): Vector3 =
        Vector3(-this.x, -this.y, -this.z)

    private fun NodePositionModel.toQuaternion(): Quaternion =
        if (yaw > 0) {
            Quaternion(Vector3(0f, 180f - yaw, 0f)).inverted()
        } else {
            Quaternion(Vector3(0f, yaw, 0f)).inverted()
        }

    private fun Matrix.toPositionVector(): Vector3 =
        Vector3(data[13], data[14], data[15])

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

        return Vector3((x * _180_DIV_PI).toFloat(), (y * _180_DIV_PI).toFloat(), (z * _180_DIV_PI).toFloat())
    }

}