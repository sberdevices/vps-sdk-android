package lab.ar.vps

import android.annotation.SuppressLint
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lab.ar.extentions.getConvertedCameraStartRotation
import lab.ar.extentions.setAlpha
import lab.ar.network.NetworkHelper
import lab.ar.network.dto.RequestDataDto
import lab.ar.network.dto.RequestDto
import lab.ar.network.dto.ResponseDto
import lab.ar.ui.VpsArFragment
import java.util.*


class Vps(
    private val coroutineScope: CoroutineScope,
    private val vpsArFragment: VpsArFragment,
    private val modelRenderable: ModelRenderable,
    private val url: String? = null,
    private val locationID: String,
    private var onlyForce: Boolean = true,
    private val locationManager: LocationManager
) {

    private var error = ""

    private var cameraStartRotation = Quaternion()
    private var cameraStartPosition = Vector3()
    private var isModelCreated = false

    private var politechParentParentNode: TransformableNode? = null
    private var politechParentNode: TransformableNode? = null
    private var politechTransformableNode: TransformableNode? = null

    private var timer: Timer? = Timer()

    fun start() {
        if(timer == null) {
            timer = Timer()
        }

        timer?.schedule(object : TimerTask() {
            override fun run() {
                updateLocalization()
            }

        }, 1000, 6000)
    }


    @SuppressLint("MissingPermission")
    private fun updateLocalization() {
        coroutineScope.launch {
            val criteria = Criteria()

            val provider = locationManager.getBestProvider(criteria, false)

            val location = provider?.let { locationManager.getLastKnownLocation(it) }

            cameraStartPosition = vpsArFragment.arSceneView.scene.camera.worldPosition
            cameraStartRotation = vpsArFragment.arSceneView.scene.camera.worldRotation

            try {
                val newLocationData =
                    NetworkHelper.takePhotoAndSendRequestToServer(vpsArFragment.arSceneView, createJsonToSend(location), url)
                val newRotation = newLocationData.first
                val newPosition = newLocationData.second

                localize(newRotation, newPosition)
            } catch (e: Exception) {
                error = e.toString()
            }
        }
    }

    private fun localize(newRotation: Quaternion, newPosition: Vector3) {
        if (!isModelCreated) {
            createNodeHierarchy()
            politechTransformableNode?.setAlpha()
            isModelCreated = true
        }

        politechParentParentNode?.worldRotation =
            getConvertedCameraStartRotation(cameraStartRotation)
        politechParentParentNode?.worldPosition = cameraStartPosition

        politechParentNode?.localRotation = newRotation

        politechTransformableNode?.localPosition = newPosition
    }

    private fun createNodeHierarchy() {
        politechParentParentNode = TransformableNode(vpsArFragment.transformationSystem)

        politechParentNode = TransformableNode(vpsArFragment.transformationSystem)

        politechTransformableNode = TransformableNode(vpsArFragment.transformationSystem).apply {
            renderable = modelRenderable
            scaleController.isEnabled = true
            scaleController.minScale = 0.01f
            scaleController.maxScale = 1f
        }

        vpsArFragment.arSceneView.scene.addChild(politechParentParentNode)
        politechParentParentNode?.addChild(politechParentNode)
        politechParentNode?.addChild(politechTransformableNode)

    }

    fun stop() {
        timer?.cancel()
        timer = null
        onlyForce = true
    }

    fun enableForceLocalization(enabled: Boolean) {
        onlyForce = enabled
    }

    fun localizeWithMockData(mockData: ResponseDto) {

    }

    private fun createJsonToSend(location: Location?): RequestDto {
        val request = RequestDto(RequestDataDto())

        request.data.attributes.forcedLocalisation = onlyForce
        request.data.attributes.location.locationId = locationID

        request.data.attributes.location.gps.apply {
            accuracy = location?.accuracy?.toDouble() ?: 0.0
            longitude = location?.longitude ?: 0.0
            latitude = location?.latitude ?: 0.0
            altitude = location?.altitude ?: 0.0
            timestamp = location?.elapsedRealtimeNanos?.toDouble() ?: 0.0
        }

        return request

    }
}