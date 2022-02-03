package com.arvrlab.vps_sdk.common

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.arvrlab.vps_sdk.domain.model.CompassModel
import com.arvrlab.vps_sdk.util.TimestampUtil
import com.arvrlab.vps_sdk.util.toDegrees

internal class CompassManager(private val sensorManager: SensorManager) : SensorEventListener {

    private var isStarted: Boolean = false

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var timestamp: Double = 0.0

    fun start() {
        if (isStarted) return

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?.let { accelerometer ->
                sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            ?.let { magneticField ->
                sensorManager.registerListener(
                    this,
                    magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }

        isStarted = true
    }

    fun stop() {
        if (!isStarted) return

        sensorManager.unregisterListener(this)

        isStarted = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        timestamp = TimestampUtil.getTimestampInSec()
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
    }

    fun getCompassModel(): CompassModel =
        CompassModel(
            accuracy = 0f,
            heading = getHeading(),
            timestamp = timestamp
        )

    private fun getHeading(): Float {
        var heading = 0f
        val isSuccess = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        if (isSuccess) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            heading = orientationAngles[0].toDegrees()
        }
        return heading
    }

}