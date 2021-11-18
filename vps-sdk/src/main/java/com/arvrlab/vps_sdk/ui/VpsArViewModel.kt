package com.arvrlab.vps_sdk.ui

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.content.Context
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import androidx.lifecycle.*
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.util.hasSelfPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class VpsArViewModel(
    private val context: Context,
    private val vpsService: VpsService,
    private val locationManager: LocationManager
) : ViewModel(), VpsService by vpsService, LifecycleObserver {

    private companion object {
        const val PERMISSIONS_REQUEST_CODE = 100
        const val LOCATION_SETTINGS_REQUEST_CODE = 101

        // request code from BaseArFragment
        const val RC_PERMISSIONS = 1010

        const val READY_DELAY = 100L
    }

    private val _requestPermissions: MutableSharedFlow<Pair<Array<String>, Int>> =
        MutableSharedFlow()
    val requestPermissions: SharedFlow<Pair<Array<String>, Int>> =
        _requestPermissions.asSharedFlow()

    private val _showDialog: MutableSharedFlow<Pair<Dialog, Int>> = MutableSharedFlow()
    val showDialog: SharedFlow<Pair<Dialog, Int>> = _showDialog.asSharedFlow()

    private val _locationSettings: MutableSharedFlow<Int> = MutableSharedFlow()
    val locationSettings: SharedFlow<Int> = _locationSettings.asSharedFlow()

    private var useGps: Boolean = false

    private var vpsStart: Boolean = false

    private var isReady: Boolean = false

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        isReady = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onPause() {
        isReady = false
    }

    override fun setVpsConfig(vpsConfig: VpsConfig) {
        vpsService.setVpsConfig(vpsConfig)
        useGps = vpsConfig.useGps
    }

    override fun startVpsService() {
        viewModelScope.launch {
            while (!isReady) delay(READY_DELAY)

            vpsStart = true
            when {
                !checkUsedPermissions() -> return@launch
                useGps && !locationManager.isGpsEnable() -> {
                    viewModelScope.launch {
                        _showDialog.emit(Dialog.LOCATION_ENABLE to LOCATION_SETTINGS_REQUEST_CODE)
                    }
                }
                else -> vpsService.startVpsService()
            }
        }
    }

    override fun stopVpsService() {
        vpsStart = false
        vpsService.stopVpsService()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            if (!locationManager.isGpsEnable()) {
                viewModelScope.launch {
                    _showDialog.emit(Dialog.LOCATION_ENABLE to requestCode)
                }
                return
            }
            startVpsService()
        }
    }

    fun onRequestPermissionsResult(requestCode: Int) {
        when (requestCode) {
            RC_PERMISSIONS, PERMISSIONS_REQUEST_CODE -> {
                if (!context.hasSelfPermission(CAMERA)) {
                    viewModelScope.launch {
                        _showDialog.emit(Dialog.CAMERA_PERMISSION to requestCode)
                    }
                    return
                }
                if (requestCode == RC_PERMISSIONS) return

                if (useGps && !context.hasSelfPermission(ACCESS_FINE_LOCATION)) {
                    viewModelScope.launch {
                        _showDialog.emit(Dialog.LOCATION_PERMISSION to requestCode)
                    }
                    return
                }

                if (vpsStart) {
                    startVpsService()
                }
            }
        }
    }

    fun requestCameraPermission(requestCode: Int) {
        viewModelScope.launch {
            _requestPermissions.emit(arrayOf(CAMERA) to requestCode)
        }
    }

    fun requestLocationPermission() {
        viewModelScope.launch {
            _requestPermissions.emit(arrayOf(ACCESS_FINE_LOCATION) to PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun checkUsedPermissions(): Boolean {
        val permissions = arrayListOf<String>()
        if (!context.hasSelfPermission(CAMERA)) {
            permissions.add(CAMERA)
        }
        if (useGps && !context.hasSelfPermission(ACCESS_FINE_LOCATION)) {
            permissions.add(ACCESS_FINE_LOCATION)
        }
        if (permissions.isNotEmpty()) {
            viewModelScope.launch {
                _requestPermissions.emit(permissions.toTypedArray() to PERMISSIONS_REQUEST_CODE)
            }
            return false
        }
        return true
    }

    private fun LocationManager.isGpsEnable(): Boolean =
        this.isProviderEnabled(GPS_PROVIDER)

    enum class Dialog {
        CAMERA_PERMISSION,
        LOCATION_PERMISSION,
        LOCATION_ENABLE,
    }

}