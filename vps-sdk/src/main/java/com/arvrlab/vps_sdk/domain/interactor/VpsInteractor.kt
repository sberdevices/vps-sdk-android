package com.arvrlab.vps_sdk.domain.interactor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import androidx.core.content.ContextCompat
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.data.repository.IVpsRepository
import com.arvrlab.vps_sdk.domain.model.LocalPositionModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel
import com.arvrlab.vps_sdk.ui.VpsCallback
import com.arvrlab.vps_sdk.util.Logger
import com.arvrlab.vps_sdk.util.toNewRotationAndPositionPair
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

internal class VpsInteractor(
    private val context: Context,
    private val arInteractor: IArInteractor,
    private val vpsRepository: IVpsRepository,
    private val neuroInteractor: INeuroInteractor
) : IVpsInteractor {

    private companion object {
        const val MIN_INTERVAL_MS = 1000L
        const val MIN_DISTANCE_IN_METERS = 1f

        const val BITMAP_WIDTH = 960
        const val BITMAP_HEIGHT = 540

        const val QUALITY = 90
    }

    override lateinit var vpsConfig: VpsConfig
        private set

    private var timerJob: Job? = null

    private var lastLocalPosition: LocalPositionModel = LocalPositionModel()

    private var failureCount = 0
    private var force = true
    private var firstLocalize = true

    private var lastLocation: Location? = null
    private val locationListener: LocationListener by lazy {
        LocationListener { location -> lastLocation = location }
    }
    private val locationManager: LocationManager by lazy {
        ContextCompat.getSystemService(context, LocationManager::class.java) as LocationManager
    }

    private var vpsCallback: VpsCallback? = null

    override fun setVpsConfig(vpsConfig: VpsConfig) {
        this.vpsConfig = vpsConfig
    }

    override fun setVpsCallback(vpsCallback: VpsCallback) {
        this.vpsCallback = vpsCallback
    }

    override fun enableForceLocalization(enabled: Boolean) {
        vpsConfig.onlyForce = enabled
        if (!enabled) {
            force = true
        }
    }

    override suspend fun startLocatization() {
        if (timerJob != null) return

        if (vpsConfig.needLocation) {
            if (isGpsProviderEnabled()) {
                requestLocationUpdates()
                launchLocatizationUpdate()
            }
        } else {
            launchLocatizationUpdate()
        }
    }

    override fun stopLocatization() {
        firstLocalize = true
        force = true
        timerJob?.cancel()
        timerJob = null
        locationManager.removeUpdates(locationListener)
    }

    override fun destroy() {
        stopLocatization()
        arInteractor.destroyHierarchy()
        neuroInteractor.close()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_INTERVAL_MS,
            MIN_DISTANCE_IN_METERS,
            locationListener
        )
    }

    private fun isGpsProviderEnabled(): Boolean =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    private suspend fun launchLocatizationUpdate() {
        coroutineScope {
            timerJob = launch(Dispatchers.Default) {
                while (isActive) {
                    updateLocalization()
                    delay(vpsConfig.timerInterval)
                }
            }
        }
    }

    private suspend fun updateLocalization() {
        arInteractor.updateLocalization()

        try {
            val image: Image = arInteractor.acquireCameraImage()
            if (!firstLocalize) {
                if (failureCount >= 2) {
                    force = true
                }
            }

            val byteArray = withContext(Dispatchers.Default) {
                if (vpsConfig.isNeuro) {
                    val imageInByteArray = image.toByteArrayNeuroVersion()
                    val bitmap = BitmapFactory.decodeByteArray(imageInByteArray, 0, imageInByteArray.size)

                    neuroInteractor.codingBitmap(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT)
                } else {
                    val bitmap = image.toBitmap()
                    ByteArrayOutputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
                        stream.toByteArray()
                    }
                }
            }
            image.close()

            val vpsLocationModel = VpsLocationModel(
                locationID = vpsConfig.locationID,
                location = if (vpsConfig.needLocation && isGpsProviderEnabled()) lastLocation else null,
                localPosition = withContext(Dispatchers.Main) { arInteractor.getLocalPosition(lastLocalPosition) },
                force = force,
                isNeuro = vpsConfig.isNeuro,
                byteArray = byteArray
            )

            val responseLocalPosition = vpsRepository.getLocation(vpsConfig.url, vpsLocationModel)
            if (responseLocalPosition != null) {
                val (rotation, position) = onSuccessResponse(responseLocalPosition)
                withContext(Dispatchers.Main) {
                    arInteractor.localize(rotation, position)
                }
            } else {
                failureCount++
                Logger.debug("fail count: $failureCount")
            }
        } catch (e: NotYetAvailableException) {
            Logger.error(e)
        } catch (e: CancellationException) {
            Logger.error(e)
        } catch (e: Exception) {
            Logger.error(e.stackTraceToString())
            withContext(Dispatchers.Main) {
                vpsCallback?.onError(e)
                stopLocatization()
            }
        }
    }

    private fun onSuccessResponse(localPosition: LocalPositionModel): Pair<Quaternion, Vector3> {
        if (!vpsConfig.onlyForce) {
            force = false
        }

        firstLocalize = false
        failureCount = 0
        vpsCallback?.onPositionVps()
        lastLocalPosition = localPosition

        arInteractor.updateRotationAngle(lastLocalPosition)

        return localPosition.toNewRotationAndPositionPair()
    }

    private fun Image.toByteArrayNeuroVersion(): ByteArray =
        ByteArrayOutputStream().use { out ->
            val yBuffer = this.planes[0].buffer
            val ySize = yBuffer.remaining()
            val nv21 = ByteArray(ySize)

            yBuffer.get(nv21, 0, ySize)

            val yuv = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
            yuv.compressToJpeg(Rect(0, 0, this.width, this.height), QUALITY, out)
            out.toByteArray()
        }

    private fun Image.toBitmap(): Bitmap {
        val byteArray = toByteArrayServerVersion()

        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size).toBlackAndWhiteBitmap()
        return Bitmap.createScaledBitmap(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT, false)
    }

    private fun Image.toByteArrayServerVersion(): ByteArray {
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

    private fun Bitmap.toBlackAndWhiteBitmap(): Bitmap {
        val blackAndWhiteBitmap = Bitmap.createBitmap(
            this.width, this.height, this.config
        )
        for (x in 0 until this.width) {
            for (y in 0 until this.height) {
                val pixelColor = this.getPixel(x, y)
                val pixelAlpha: Int = Color.alpha(pixelColor)
                val pixelRed: Int = Color.red(pixelColor)
                val pixelGreen: Int = Color.green(pixelColor)
                val pixelBlue: Int = Color.blue(pixelColor)
                val pixelBW = (pixelRed + pixelGreen + pixelBlue) / 3
                val newPixel: Int = Color.argb(pixelAlpha, pixelBW, pixelBW, pixelBW)
                blackAndWhiteBitmap.setPixel(x, y, newPixel)
            }
        }
        return blackAndWhiteBitmap
    }

}