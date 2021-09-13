package com.arvrlab.vps_sdk.domain.interactor

import android.media.Image
import com.arvrlab.vps_sdk.domain.model.LocalPositionModel
import com.arvrlab.vps_sdk.util.getConvertedCameraStartRotation
import com.arvrlab.vps_sdk.util.toEulerAngles
import com.arvrlab.vps_sdk.util.toPositionVector
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.math.PI

internal class ArInteractor(private val arSceneViewLazy: Lazy<ArSceneView>) : IArInteractor {

    private val arSceneView: ArSceneView
        get() = arSceneViewLazy.value

    private val scene: Scene
        get() = arSceneView.scene

    private val camera: Camera
        get() = scene.camera

    private var cameraStartPosition: Vector3 = Vector3()
    private var cameraStartRotation: Quaternion = Quaternion()

    private var photoTransform: Matrix? = null

    private var rotationAngle: Float? = null

    private var isModelCreated: Boolean = false

    override val modelNode: Node by lazy {
        AnchorNode()
    }
    private val rotationNode: Node by lazy {
        AnchorNode()
            .also { it.addChild(modelNode) }
    }
    private val cameraAlternativeNode: Node by lazy {
        AnchorNode()
            .also { it.addChild(rotationNode) }
    }

    override fun updateLocalization() {
        cameraStartPosition = camera.worldPosition
        cameraStartRotation = camera.worldRotation
        photoTransform = camera.worldModelMatrix
    }

    override fun updateRotationAngle(lastLocalPosition: LocalPositionModel?) {
        val anglesY = Quaternion(
            Vector3(
                ((lastLocalPosition?.roll ?: 0f) * PI / 180f).toFloat(),
                ((lastLocalPosition?.yaw ?: 0f) * PI / 180f).toFloat(),
                ((lastLocalPosition?.pitch ?: 0f) * PI / 180f).toFloat()
            )
        ).toEulerAngles().y
        val cameraAngles = Quaternion(photoTransform?.toPositionVector()).toEulerAngles().y

        rotationAngle = anglesY + cameraAngles
    }

    override fun localize(rotation: Quaternion, position: Vector3) {
        createNodeHierarchyIfNeed()

        cameraAlternativeNode.worldRotation = getConvertedCameraStartRotation(cameraStartRotation)
        cameraAlternativeNode.worldPosition = cameraStartPosition
        cameraAlternativeNode.worldPosition?.z = 0f

        rotationNode.localRotation = rotation
        modelNode.localPosition = position
    }

    override fun getLocalPosition(lastLocalPosition: LocalPositionModel): LocalPositionModel {
        val lastCamera = Node()
        val newCamera = Node()
        val anchorParent = AnchorNode()

        lastCamera.worldPosition = photoTransform?.toPositionVector()
        newCamera.worldPosition = camera.worldPosition
        newCamera.worldRotation = camera.worldRotation

        anchorParent.run {
            addChild(lastCamera)
            addChild(newCamera)
            worldRotation = Quaternion(Vector3(0f, rotationAngle ?: 0f, 0f))
        }

        val correct = Vector3(
            newCamera.worldPosition.x - lastCamera.worldPosition.x,
            newCamera.worldPosition.y - lastCamera.worldPosition.y,
            newCamera.worldPosition.z - lastCamera.worldPosition.z
        )

        val eulerNode = Node()
        eulerNode.worldPosition = newCamera.worldPosition
        eulerNode.worldRotation = newCamera.worldRotation

        val newPos = Vector3(
            lastLocalPosition.x + correct.x,
            lastLocalPosition.y + correct.y,
            lastLocalPosition.z + correct.z
        )
        val newAngle = eulerNode.localRotation.toEulerAngles()

        return LocalPositionModel(
            x = newPos.x,
            y = newPos.y,
            z = newPos.z,
            roll = newAngle.x,
            pitch = newAngle.z,
            yaw = newAngle.y,
        )
    }

    override fun destroyHierarchy() {
        cameraAlternativeNode.setParent(null)
        rotationNode.setParent(null)
        cameraAlternativeNode.setParent(null)
        modelNode.renderable = null

        isModelCreated = false
    }

    override fun acquireCameraImage(): Image? =
        arSceneView.arFrame?.acquireCameraImage()

    private fun createNodeHierarchyIfNeed() {
        if (isModelCreated) return
        isModelCreated = true

        scene.addChild(cameraAlternativeNode)
    }

}