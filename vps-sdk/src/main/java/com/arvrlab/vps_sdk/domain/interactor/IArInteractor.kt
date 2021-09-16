package com.arvrlab.vps_sdk.domain.interactor

import android.media.Image
import com.arvrlab.vps_sdk.domain.model.LocalPositionModel
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

internal interface IArInteractor {

    val modelNode: Node

    fun updateLocalization()
    fun updateRotationAngle(lastLocalPosition: LocalPositionModel?)
    fun localize(rotation: Quaternion, position: Vector3)
    fun destroyHierarchy()
    fun getLocalPosition(lastLocalPosition: LocalPositionModel): LocalPositionModel

    @Throws(exceptionClasses = [NotYetAvailableException::class])
    fun acquireCameraImage(): Image
}