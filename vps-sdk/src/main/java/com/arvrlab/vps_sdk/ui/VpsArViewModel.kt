package com.arvrlab.vps_sdk.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.*
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.domain.interactor.IArInteractor
import com.arvrlab.vps_sdk.domain.interactor.IVpsInteractor
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.Renderable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class VpsArViewModel(
    private val application: Application,
    private val vpsInteractor: IVpsInteractor,
    private val arInteractor: IArInteractor
) : ViewModel(), VpsService, LifecycleObserver {

    private companion object {
        const val ACCESS_FINE_LOCATION_REQUEST_CODE = 1000
        const val CAMERA_PERMISSION_REQUEST_CODE = 1010
    }

    override val modelNode: Node
        get() = arInteractor.modelNode

    private val _requestPermissions: MutableSharedFlow<Pair<Array<String>, Int>> = MutableSharedFlow()
    val requestPermissions: SharedFlow<Pair<Array<String>, Int>> = _requestPermissions.asSharedFlow()

    private val _locationPermissionDialog: MutableSharedFlow<Unit> = MutableSharedFlow()
    val locationPermissionDialog: SharedFlow<Unit> = _locationPermissionDialog.asSharedFlow()

    private val _cameraPermissionDialog: MutableSharedFlow<Unit> = MutableSharedFlow()
    val cameraPermissionDialog: SharedFlow<Unit> = _cameraPermissionDialog.asSharedFlow()

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        vpsInteractor.destroy()
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        if (requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE) {
            checkResultLocation()
        }
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            checkResultCamera()
        }
    }

    override fun setVpsConfig(vpsConfig: VpsConfig) {
        vpsInteractor.setVpsConfig(vpsConfig)
    }

    override fun setVpsCallback(vpsCallback: VpsCallback) {
        vpsInteractor.setVpsCallback(vpsCallback)
    }

    override fun startVpsService() {
        if (vpsInteractor.vpsConfig.needLocation) {
            checkPermission()
        } else {
            vpsInteractor.startLocatization()
        }
    }

    override fun stopVpsService() {
        vpsInteractor.stopLocatization()
    }

    override fun enableForceLocalization(enabled: Boolean) =
        vpsInteractor.enableForceLocalization(enabled)

    override fun setRenderable(renderable: Renderable) {
        arInteractor.modelNode.renderable = renderable
    }

    private fun checkPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            vpsInteractor.startLocatization()
        } else {
            viewModelScope.launch {
                _requestPermissions.emit(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) to ACCESS_FINE_LOCATION_REQUEST_CODE
                )
            }
        }
    }

    private fun checkResultLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            startVpsService()
            return
        }

        viewModelScope.launch {
            _locationPermissionDialog.emit(Unit)
        }
    }

    private fun checkResultCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA)) {
            return
        }
        viewModelScope.launch {
            _cameraPermissionDialog.emit(Unit)
        }
    }

    private fun checkSelfPermission(permission: String): Boolean =
        ActivityCompat.checkSelfPermission(application, permission) == PackageManager.PERMISSION_GRANTED

}