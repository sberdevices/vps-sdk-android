package com.arvrlab.vps_sdk.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import androidx.core.content.ContextCompat
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.network.VpsApi
import com.arvrlab.vps_sdk.network.dto.*
import com.arvrlab.vps_sdk.neuro.NeuroModel
import com.arvrlab.vps_sdk.util.*
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.*
import okhttp3.MultipartBody
import kotlin.coroutines.CoroutineContext
import kotlin.math.PI

internal class VpsService(
    private val context: Context
) : CoroutineScope {

    private companion object {
        const val VPS_THREAD_NAME = "VPS-Service"
        const val MIN_INTERVAL_MS = 1000L
        const val MIN_DISTANCE_IN_METERS = 1f
    }

    override val coroutineContext: CoroutineContext = SupervisorJob() + newSingleThreadContext(VPS_THREAD_NAME)

    val anchorNode: Node by lazy {
        AnchorNode()
    }

    lateinit var vpsConfig: VpsConfig
        private set

    private var lastLocation: Location? = null

    private var timerJob: Job? = null

    private var cameraStartRotation = Quaternion()
    private var cameraStartPosition = Vector3()

    private var isModelCreated = false
    private var isTimerRunning = false

    private var cameraAlternativeNode: AnchorNode? = null
    private var rotationNode: AnchorNode? = null

    private var failureCount = 0
    private var force = true
    private var firstLocalize = true

    private var lastResponse: ResponseRelativeDto? = null
    private var photoTransform: Matrix? = null
    private var successPhotoTransform: Matrix? = null
    private var rotationAngle: Float? = null

    private val neuroModel by lazy { NeuroModel(context) }

    private val locationListener: LocationListener by lazy {
        LocationListener { location -> lastLocation = location }
    }
    private val locationManager: LocationManager by lazy {
        ContextCompat.getSystemService(context, LocationManager::class.java) as LocationManager
    }

    private var arService: ArService? = null
    private var vpsCallback: VpsCallback? = null

    internal fun bindArService(arService: ArService) {
        this.arService = arService
    }

    internal fun unbindArService() {
        this.arService = null
    }

    fun setVpsConfig(vpsConfig: VpsConfig) {
        this.vpsConfig = vpsConfig
    }

    fun setVpsCallback(vpsCallback: VpsCallback) {
        this.vpsCallback = vpsCallback
    }

    fun start() {
        if (isTimerRunning) return

        if (vpsConfig.needLocation) {
            checkPermissionAndLaunchLocalizationUpdate()
        } else {
            launchLocatizationUpdate()
        }
    }

    fun stop() {
        firstLocalize = true
        force = true
        stopLocalizationUpdate()
        locationManager.removeUpdates(locationListener)
    }

    fun destroy() {
        neuroModel.close()
        stop()
        destroyHierarchy()
        coroutineContext.cancel()
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdate() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_INTERVAL_MS,
            MIN_DISTANCE_IN_METERS,
            locationListener
        )
    }

    fun localizeWithMockData(mockData: ResponseDto) {
        stopLocalizationUpdate()

        try {
            val (rotation, position) = mockData.toNewRotationAndPositionPair()
            localize(rotation, position)
        } catch (e: Exception) {
            vpsCallback?.onError(e)
        }
    }

    fun enableForceLocalization(enabled: Boolean) {
        vpsConfig.onlyForce = enabled
        if (!enabled) {
            force = true
        }
    }

    private fun checkPermissionAndLaunchLocalizationUpdate() {
        if (isGpsProviderEnabled()) {
            requestLocationUpdate()
            launchLocatizationUpdate()
        }
    }

    private fun launchLocatizationUpdate() {
        updateLocalization()

        timerJob = launch(Dispatchers.IO) {
            while (true) {
                delay(vpsConfig.timerInterval)
                if (isActive) {
                    updateLocalization()
                }
            }
        }

        isTimerRunning = true
    }

    private fun updateLocalization() {
        launch(Dispatchers.Main) {

            val camera = checkNotNull(arService).camera
            cameraStartPosition = camera.worldPosition
            cameraStartRotation = camera.worldRotation

            sendRequest()?.let { (rotation, position) ->
                localize(rotation, position)
            }
        }
    }

    private suspend fun sendRequest(): Pair<Quaternion, Vector3>? {
        return withContext(Dispatchers.Main) {
            photoTransform = checkNotNull(arService).camera.worldModelMatrix
            try {
                val responseDto = VpsApi.getApiService(vpsConfig.url).process(getRequestDto(), getMultipartBody())
                if (responseDto.responseData?.responseAttributes?.status == "done") {
                    onSuccessResponse(responseDto, responseDto.responseData.responseAttributes)
                } else {
                    onFailResponse()
                    null
                }
            } catch (e: NotYetAvailableException) {
                Logger.error(e)
                null
            } catch (e: CancellationException) {
                Logger.error(e)
                null
            } catch (e: Exception) {
                Logger.error(e.stackTraceToString())
                stop()
                vpsCallback?.onError(e)
                null
            }
        }
    }

    private fun getRequestDto(): RequestDto {
        return if (vpsConfig.needLocation && isGpsProviderEnabled()) {
            createRequestDto(lastLocation)
        } else {
            createRequestDto()
        }
    }

    private fun createRequestDto(location: Location? = null): RequestDto {
        val request = RequestDto(RequestDataDto())

        if (!firstLocalize) {
            if (failureCount >= 2) {
                force = true
            }
        }

        if (!force) {
            setLocalPosToRequestDto(request)
        }

        Logger.debug("force: $force")
        request.data.attributes.forcedLocalisation = force
        request.data.attributes.location.locationId = vpsConfig.locationID

        location?.run {
            request.data.attributes.location.gps = RequestGpsDto(
                accuracy.toDouble(),
                altitude,
                latitude,
                longitude,
                elapsedRealtimeNanos.toDouble()
            )
        }

        Logger.debug("request: $request")
        return request
    }

    private fun setLocalPosToRequestDto(request: RequestDto) {
        val lastCamera = Node()
        val newCamera = Node()
        val anchorParent = AnchorNode()

        lastCamera.worldPosition = successPhotoTransform?.toPositionVector()
        val camera = checkNotNull(arService).camera
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
            lastResponse?.x ?: 0f + correct.x,
            lastResponse?.y ?: 0f + correct.y,
            lastResponse?.z ?: 0f + correct.z
        )
        val newAngle = eulerNode.localRotation.toEulerAngles()

        request.data.attributes.location.localPos.run {
            x = newPos.x
            y = newPos.y
            z = newPos.z
            roll = newAngle.x
            pitch = newAngle.z
            yaw = newAngle.y
        }
    }

    private suspend fun getMultipartBody(): MultipartBody.Part {
        val image: Image = arService?.arFrame?.acquireCameraImage()
            ?: throw NotYetAvailableException("Failed to acquire camera image")

        val multipartBody =
            if (vpsConfig.isNeuro) image.toMultipartBodyNeuro(neuroModel) else image.toMultipartBodyServer()
        image.close()
        return multipartBody
    }

    private fun onSuccessResponse(
        responseDto: ResponseDto,
        responseAttributes: ResponseAttributesDto
    ): Pair<Quaternion, Vector3> {
        Logger.debug("done")
        if (!vpsConfig.onlyForce) {
            force = false
        }

        firstLocalize = false
        failureCount = 0
        vpsCallback?.onPositionVps(responseDto)
        lastResponse = responseAttributes.responseLocation?.responseRelative
        successPhotoTransform = photoTransform

        val anglesY = Quaternion(
            Vector3(
                ((lastResponse?.roll ?: 0f) * PI / 180f).toFloat(),
                ((lastResponse?.yaw ?: 0f) * PI / 180f).toFloat(),
                ((lastResponse?.pitch ?: 0f) * PI / 180f).toFloat()
            )
        ).toEulerAngles().y
        val cameraAngles = Quaternion(photoTransform?.toPositionVector()).toEulerAngles().y

        rotationAngle = anglesY + cameraAngles

        return responseDto.toNewRotationAndPositionPair()
    }

    private fun onFailResponse() {
        Logger.debug("fail")
        failureCount++
        Logger.debug("failureCount: $failureCount")
    }

    private fun localize(rotation: Quaternion, position: Vector3) {
        createNodeHierarchyIfNeed()

        cameraAlternativeNode?.worldRotation = getConvertedCameraStartRotation(cameraStartRotation)
        cameraAlternativeNode?.worldPosition = cameraStartPosition
        cameraAlternativeNode?.worldPosition?.z = 0f

        rotationNode?.localRotation = rotation
        anchorNode?.localPosition = position
    }

    private fun createNodeHierarchyIfNeed() {
        if (isModelCreated) return
        isModelCreated = true

        rotationNode = AnchorNode()
            .also { it.addChild(anchorNode) }
        cameraAlternativeNode = AnchorNode()
            .also { it.addChild(rotationNode) }

        val scene = checkNotNull(arService).scene
        scene.addChild(cameraAlternativeNode)
    }

    private fun isGpsProviderEnabled() =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    private fun stopLocalizationUpdate() {
        timerJob?.cancel()
        isTimerRunning = false
    }

    private fun destroyHierarchy() {
        cameraAlternativeNode?.setParent(null)
        rotationNode?.setParent(null)
        cameraAlternativeNode?.setParent(null)
        anchorNode?.renderable = null

        cameraAlternativeNode = null
        rotationNode = null

        isModelCreated = false
    }

}