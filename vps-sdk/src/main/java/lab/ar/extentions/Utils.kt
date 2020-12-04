package lab.ar.extentions

import android.graphics.*
import android.media.Image
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lab.ar.network.dto.ResponseDto
import java.io.ByteArrayOutputStream
import java.lang.Exception

fun Image.toByteArray(): ByteArray {
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

fun Bitmap.toBlackAndWhiteBitmap(): Bitmap {
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

suspend fun getResizedBitmap(image: Image): Bitmap {
    return withContext(Dispatchers.Default) {
        val bytes = image.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toBlackAndWhiteBitmap()
        Bitmap.createScaledBitmap(bitmap, 960, 540, false)
    }
}

fun getConvertedCameraStartRotation(cameraRotation: Quaternion): Quaternion {
    val dir = Quaternion.rotateVector(cameraRotation, Vector3(0f, 0f, 1f))
    dir.y = 0f
    return Quaternion.rotationBetweenVectors(Vector3(0f, 0f, 1f), dir)
}

fun TransformableNode.setAlpha() {
    val engine = EngineInstance.getEngine().filamentEngine
    val rm = engine.renderableManager

    renderableInstance?.filamentAsset?.let { asset ->
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

fun ResponseDto.toNewPositionAndLocationPair(): Pair<Quaternion, Vector3> {
    val coordinateData =
        responseData?.responseAttributes?.responseLocation?.responseRelative ?: throw Exception("Fail")

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

    return Pair(newRotation, newPosition)
}