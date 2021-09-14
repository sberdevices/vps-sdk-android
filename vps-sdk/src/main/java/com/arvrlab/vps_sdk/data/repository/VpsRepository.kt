package com.arvrlab.vps_sdk.data.repository

import android.content.Context
import android.graphics.*
import android.media.Image
import com.arvrlab.vps_sdk.BuildConfig
import com.arvrlab.vps_sdk.data.api.VpsApi
import com.arvrlab.vps_sdk.data.model.request.*
import com.arvrlab.vps_sdk.domain.model.LocalPositionModel
import com.arvrlab.vps_sdk.domain.model.VpsLocationModel
import com.arvrlab.vps_sdk.domain.neuro.NeuroHelper
import com.arvrlab.vps_sdk.domain.neuro.NeuroModel
import com.arvrlab.vps_sdk.util.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayOutputStream

internal class VpsRepository(
    private val context: Context
) : IVpsRepository {

    private companion object {
        const val STATUS_DONE = "done"
        const val STATUS_FAIL = "fail"

        const val BITMAP_WIDTH = 960
        const val BITMAP_HEIGHT = 540

        const val QUALITY = 90
    }

    private val cachedVpsServices = mutableMapOf<String, VpsApi>()

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val neuroModel: NeuroModel by lazy { NeuroModel(context) }

    override suspend fun getLocation(requestLocationModel: VpsLocationModel): LocalPositionModel? {
        val requestVpsModel = createRequestVpsModel(requestLocationModel)
        val body = getMultipartBody(requestLocationModel.image, requestLocationModel.isNeuro)

        val response = getVpsService(requestLocationModel.url).requestLocation(requestVpsModel, body)

        return if (response.data?.attributes?.status == STATUS_DONE) {
            val relative = response.data.attributes.location?.relative
            LocalPositionModel(
                x = relative?.x ?: 0f,
                y = relative?.y ?: 0f,
                z = relative?.z ?: 0f,
                roll = relative?.roll ?: 0f,
                pitch = relative?.pitch ?: 0f,
                yaw = relative?.yaw ?: 0f,
            )
        } else {
            null
        }
    }

    private fun createRequestVpsModel(vpsLocationModel: VpsLocationModel): RequestVpsModel {
        val localPos = if (!vpsLocationModel.force) {
            vpsLocationModel.localPosition.toRequestLocalPosDto()
        } else {
            RequestLocalPosModel()
        }

        val gpsModel: RequestGpsModel? = vpsLocationModel.location?.run {
            RequestGpsModel(
                accuracy.toDouble(),
                altitude,
                latitude,
                longitude,
                elapsedRealtimeNanos.toDouble()
            )
        }

        return RequestVpsModel(
            data = RequestDataModel(
                attributes = RequestAttributesModel(
                    forcedLocalisation = vpsLocationModel.force,
                    location = RequestLocationModel(
                        locationId = vpsLocationModel.locationID,
                        localPos = localPos,
                        gps = gpsModel
                    )
                )
            )
        )
    }

    private suspend fun getMultipartBody(image: Image, isNeuro: Boolean): MultipartBody.Part {
        val multipartBody = if (isNeuro) image.toMultipartBodyNeuro(neuroModel) else image.toMultipartBodyServer()
        image.close()
        neuroModel.close()
        return multipartBody
    }

    private fun getVpsService(url: String): VpsApi =
        cachedVpsServices[url] ?: getClient(url).let { client ->
            val vpsApiService = client.create(VpsApi::class.java)
            cachedVpsServices[url] = vpsApiService
            vpsApiService
        }

    private fun getClient(baseUrl: String): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private fun LocalPositionModel.toRequestLocalPosDto(): RequestLocalPosModel =
        RequestLocalPosModel(
            x = this.x,
            y = this.y,
            z = this.z,
            roll = this.roll,
            pitch = this.pitch,
            yaw = this.yaw,
        )

    private suspend fun Image.toMultipartBodyServer(): MultipartBody.Part {
        return withContext(Dispatchers.IO) {
            val resizedBitmap = getResizedBitmap(this@toMultipartBodyServer)

            val byteArray = ByteArrayOutputStream().use { stream ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
                stream.toByteArray()
            }

            resizedBitmap.recycle()

            val requestBody = byteArray.toRequestBody("*/*".toMediaTypeOrNull(), 0, byteArray.size)
            MultipartBody.Part.createFormData("image", "sceneform_photo", requestBody)
        }
    }

    private suspend fun Image.toMultipartBodyNeuro(neuro: NeuroModel): MultipartBody.Part {
        return withContext(Dispatchers.IO) {

            val imageInByteArray = toByteArrayNeuroVersion()
            val bitmap = BitmapFactory.decodeByteArray(imageInByteArray, 0, imageInByteArray.size)

            val neuroResult = neuro.getFeatures(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT)
            val byteArray = NeuroHelper.getFileAsByteArray(neuroResult)

            bitmap.recycle()

            val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull(), 0, byteArray.size)
            MultipartBody.Part.createFormData("embedding", "embedding.embd", requestBody)
        }
    }

    private fun Image.toByteArrayNeuroVersion(): ByteArray {
        return ByteArrayOutputStream().use { out ->
            val yBuffer = this.planes[0].buffer
            val ySize = yBuffer.remaining()
            val nv21 = ByteArray(ySize)

            yBuffer.get(nv21, 0, ySize)

            val yuv = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
            yuv.compressToJpeg(Rect(0, 0, this.width, this.height), QUALITY, out)
            out.toByteArray()
        }
    }

    private suspend fun getResizedBitmap(image: Image): Bitmap {
        return withContext(Dispatchers.IO) {
            val bytes = image.toByteArrayServerVersion()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toBlackAndWhiteBitmap()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, BITMAP_WIDTH, BITMAP_HEIGHT, false)

            bitmap.recycle()

            scaledBitmap
        }
    }

    private fun Bitmap.toBlackAndWhiteBitmap(): Bitmap {
        val blackAndWhieBitmap = Bitmap.createBitmap(
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
                blackAndWhieBitmap.setPixel(x, y, newPixel)
            }
        }
        return blackAndWhieBitmap
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
}