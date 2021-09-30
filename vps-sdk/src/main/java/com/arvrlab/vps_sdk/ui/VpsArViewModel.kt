package com.arvrlab.vps_sdk.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arvrlab.vps_sdk.data.VpsConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class VpsArViewModel(
    private val context: Context,
    private val vpsService: VpsService
) : ViewModel(), VpsService by vpsService, LifecycleObserver {

    private companion object {
        const val ACCESS_FINE_LOCATION_REQUEST_CODE = 1000
        const val CAMERA_PERMISSION_REQUEST_CODE = 1010
    }

    private val _requestPermissions: MutableSharedFlow<Pair<Array<String>, Int>> =
        MutableSharedFlow()
    val requestPermissions: SharedFlow<Pair<Array<String>, Int>> =
        _requestPermissions.asSharedFlow()

    private val _locationPermissionDialog: MutableSharedFlow<Unit> = MutableSharedFlow()
    val locationPermissionDialog: SharedFlow<Unit> = _locationPermissionDialog.asSharedFlow()

    private val _cameraPermissionDialog: MutableSharedFlow<Unit> = MutableSharedFlow()
    val cameraPermissionDialog: SharedFlow<Unit> = _cameraPermissionDialog.asSharedFlow()

    private var useGps: Boolean = false

    override fun setVpsConfig(vpsConfig: VpsConfig) {
        vpsService.setVpsConfig(vpsConfig)
        useGps = vpsConfig.useGps
    }

    override fun startVpsService() {
        if (useGps && !checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            viewModelScope.launch {
                _requestPermissions.emit(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) to ACCESS_FINE_LOCATION_REQUEST_CODE
                )
            }
        } else {
            vpsService.startVpsService()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE) {
            checkResultLocation()
        }
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            checkResultCamera()
        }
    }

    private fun checkResultLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            vpsService.startVpsService()
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
        ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
}