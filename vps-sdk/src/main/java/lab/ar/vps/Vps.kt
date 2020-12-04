package lab.ar.vps

import android.graphics.*
import android.media.Image
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lab.ar.network.RestApi
import lab.ar.network.dto.RequestDataDto
import lab.ar.network.dto.RequestDto
import lab.ar.network.dto.ResponseDto
import lab.ar.ui.VpsArFragment
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class Vps(
    private val coroutineScope: CoroutineScope,
    private val vpsArFragment: VpsArFragment,
    private val modelRenderable: ModelRenderable,
    private val url: String,
    private val locationID: String,
    private var onlyForce: Boolean = true
) {

    private var error = ""
    private var newDataForNodes = Pair(Quaternion(), Vector3())

    private var cameraStartRotation = Quaternion()
    private var cameraStartPosition = Vector3()
    private var isModelCreated = false

    private var politechParentParentNode: TransformableNode? = null
    private var politechParentNode: TransformableNode? = null
    private var politechTransformableNode: TransformableNode? = null

    private val engine = EngineInstance.getEngine().filamentEngine
    private val rm = engine.renderableManager

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

    private fun setAlpha() {
        politechTransformableNode?.renderableInstance?.filamentAsset?.let { asset ->
            for (entity in asset.entities) {
                val renderable = rm.getInstance(entity)
                if (renderable != 0) {
                    val r = 7f / 255
                    val g = 7f / 225
                    val b = 143f / 225
                    val materialInstance = rm.getMaterialInstanceAt(renderable, 0)
                    materialInstance.setParameter("baseColorFactor", r, g, b, 0.6f)
                }
            }
        }
    }

    private fun onSendPhoto() {
            cameraStartPosition = vpsArFragment.arSceneView.scene.camera.worldPosition
            cameraStartRotation = vpsArFragment.arSceneView.scene.camera.worldRotation
            takePhotoAndSendRequestToServer(vpsArFragment.arSceneView)
    }

    private fun onGetResponse() {
        localize(newDataForNodes.first, newDataForNodes.second)
    }

    private fun localize(newRotation: Quaternion, newPosition: Vector3) {
        if (!isModelCreated) {
            createNodeHierarchy()
            setAlpha()
            isModelCreated = true
        }

        politechParentParentNode?.worldRotation = convert(cameraStartRotation)
        politechParentParentNode?.worldPosition = cameraStartPosition

        politechParentNode?.localRotation = newRotation

        politechTransformableNode?.localPosition = newPosition
    }

    private fun convert(cameraRotation: Quaternion): Quaternion {
        val dir = Quaternion.rotateVector(cameraRotation, Vector3(0f, 0f, 1f))
        dir.y = 0f
        return Quaternion.rotationBetweenVectors(Vector3(0f, 0f, 1f), dir)
    }

    private fun takePhotoAndSendRequestToServer(view: ArSceneView) {
        coroutineScope.launch {
            val imageName = "sceneform_photo"
            var image: Image? = null
            try {
                image = view.arFrame?.acquireCameraImage() ?: return@launch
                val resizedBitmap = getResizedBitmap(image)
                val imageMultipart = getImageMultipart(resizedBitmap, imageName)

                sendRequestToServer(imageMultipart, imageName)
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                image?.close()
            }
        }
    }

    private suspend fun getResizedBitmap(image: Image): Bitmap {
        return withContext(Dispatchers.Default) {
            val bytes = image.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toBlackAndWhiteBitmap()
            Bitmap.createScaledBitmap(bitmap, 960, 540, false)
        }
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
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        return out.toByteArray()
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

    private fun getImageMultipart(bitmap: Bitmap, filePath: String): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        bitmap.recycle()

        val requestBody = byteArray.toRequestBody("*/*".toMediaTypeOrNull(), 0, byteArray.size)
        return MultipartBody.Part.createFormData("image", filePath, requestBody)
    }

    private fun sendRequestToServer(imageMultipart: MultipartBody.Part, filePath: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val json = RequestDto(RequestDataDto(jobId = filePath.hashCode().toString()))
                val deferredResponse = RestApi.retrofitService.process(json, imageMultipart)
                extractResponseData(deferredResponse.await())
            } catch (e: Exception) {
                error = e.toString()
            }
        }
    }

    private fun extractResponseData(response: ResponseDto) {
        val coordinateData =
            response.responseData?.responseAttributes?.responseLocation?.responseRelative ?: return

        val yaw = coordinateData.yaw ?: 0f
        val x = 0 - (coordinateData.x ?: 0f)
        val y = 0 - (coordinateData.y ?: 0f)
        val z = 0 - (coordinateData.z ?: 0f)

        val newPosition = Vector3(x, y, z)

        val newRotation = if (yaw > 0) {
            Quaternion(Vector3(0f, 180f - yaw, 0f)).inverted()
        } else {
            Quaternion(Vector3(0f, yaw, 0f)).inverted()
        }

        newDataForNodes = Pair(newRotation, newPosition)
    }


    fun start() {

    }

    fun stop() {

    }

    fun enableForceLocalization(enabled: Boolean) {

    }

    fun localizeWithMockData(mockData: ResponseDto) {

    }
}