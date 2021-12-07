package com.arvrlab.vps_sdk.common

import com.arvrlab.vps_sdk.domain.model.GpsPoseModel
import com.arvrlab.vps_sdk.domain.model.NodePoseModel
import com.arvrlab.vps_sdk.domain.model.PoseModel
import com.arvrlab.vps_sdk.util.getEulerAngles
import com.arvrlab.vps_sdk.util.toRadians
import com.google.ar.sceneform.math.Vector3
import org.koin.core.context.GlobalContext
import kotlin.math.cos
import kotlin.math.sin

class CoordinateConverter internal constructor() {

    companion object {
        private const val TWO_PI_DEGREES = 360

        private const val MERIDIAN_ONE_DEGREES_DISTANCE = 40008.548 * 1000.0 / TWO_PI_DEGREES
        private const val EQUATOR_ONE_DEGREES_DISTANCE = 40075.0 * 1000.0 / TWO_PI_DEGREES

        private const val PI_DIV_180 = Math.PI / 180.0

        fun instance(): CoordinateConverter =
            GlobalContext.get().get()
    }

    private var prevPoseModel: PoseModel = PoseModel.EMPTY

    private var angleDifference: Float = 0f

    internal fun updatePoseModel(poseModel: PoseModel) {
        prevPoseModel = poseModel
        angleDifference = calculateAngleDifference(poseModel)
    }

    fun convertToGlobalCoordinate(nodePoseModel: NodePoseModel): GpsPoseModel {
        if (prevPoseModel == PoseModel.EMPTY) return GpsPoseModel.EMPTY

        val nodePoseHeading = nodePoseModel.getRotation().getEulerAngles().y

        val prevCoordinate =
            rotatedCoordinate(angleDifference, prevPoseModel.nodePoseModel.getPosition())
        val currentCoordinate = rotatedCoordinate(angleDifference, nodePoseModel.getPosition())
        val coordinate =
            calculateGpsCoordinate(prevCoordinate, currentCoordinate, prevPoseModel.gpsPoseModel)

        var heading = nodePoseHeading - angleDifference

        if (heading < 0) heading += TWO_PI_DEGREES
        else if (heading > TWO_PI_DEGREES) heading -= TWO_PI_DEGREES

        return GpsPoseModel(0f, coordinate.latitude, coordinate.longitude, heading)
    }

    fun convertToLocalCoordinate(gpsPoseModel: GpsPoseModel): NodePoseModel {
        if (prevPoseModel == PoseModel.EMPTY) return NodePoseModel.EMPTY

        val position = calculateArCoreCoordinate(
            prevPoseModel.gpsPoseModel,
            gpsPoseModel,
            prevPoseModel.nodePoseModel.getPosition(),
            -angleDifference
        )
        val angleY = -gpsPoseModel.heading - angleDifference
        return NodePoseModel(position.x, position.y, position.z, 0f, angleY, 0f)
    }

    private fun calculateArCoreCoordinate(
        prevGpsPoseModel: GpsPoseModel,
        gpsPoseModel: GpsPoseModel,
        prevNodePosition: Vector3,
        angleDifference: Float,
    ): Vector3 {
        val prevLatitude = prevGpsPoseModel.latitude
        val prevLongitude = prevGpsPoseModel.longitude
        val latitude = gpsPoseModel.latitude
        val longitude = gpsPoseModel.longitude

        val oneDegreesLongitude = cos(prevLatitude * PI_DIV_180) * EQUATOR_ONE_DEGREES_DISTANCE
        val dx = (longitude - prevLongitude) * oneDegreesLongitude
        val dz = (latitude - prevLatitude) * MERIDIAN_ONE_DEGREES_DISTANCE

        val coordinateDifference = rotatedCoordinate(
            angleDifference,
            Vector3(dx.toFloat(), prevNodePosition.y, dz.toFloat())
        )
        return Vector3(
            prevNodePosition.x + coordinateDifference.x,
            prevNodePosition.y,
            prevNodePosition.z + coordinateDifference.z
        )
    }

    private fun rotatedCoordinate(angle: Float, point: Vector3): Vector3 {
        val rad = angle.toRadians()
        val newX = point.x * cos(rad) + point.z * sin(rad)
        val newZ = point.x * sin(rad) - point.z * cos(rad)
        return Vector3(newX, point.y, newZ)
    }

    private fun calculateAngleDifference(poseModel: PoseModel): Float {
        val angle = poseModel.nodePoseModel.getRotation().getEulerAngles().y
        return -(poseModel.gpsPoseModel.heading - angle)
    }

    private fun calculateGpsCoordinate(
        prevCoordinate: Vector3,
        currentCoordinate: Vector3,
        gpsPoseModel: GpsPoseModel
    ): GpsPoseModel {
        val dx = currentCoordinate.x - prevCoordinate.x
        val dz = currentCoordinate.z - prevCoordinate.z

        val oneDegreesLongitude =
            cos(gpsPoseModel.latitude * PI_DIV_180) * EQUATOR_ONE_DEGREES_DISTANCE

        val latitude = gpsPoseModel.latitude - dz / MERIDIAN_ONE_DEGREES_DISTANCE
        val longitude = gpsPoseModel.longitude + dx / oneDegreesLongitude

        return GpsPoseModel(latitude = latitude.toFloat(), longitude = longitude.toFloat())
    }
}