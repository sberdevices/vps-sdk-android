package com.arvrlab.vps_sdk.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.arvrlab.vps_sdk.data.VpsConfig
import com.arvrlab.vps_sdk.domain.interactor.IVpsInteractor
import com.arvrlab.vps_sdk.domain.model.GpsLocationModel
import com.arvrlab.vps_sdk.domain.model.NodePositionModel
import com.arvrlab.vps_sdk.util.Logger
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class VpsArViewModel(
    private val application: Application,
    private val vpsInteractor: IVpsInteractor,
    private val arManager: ArManager
) : ViewModel(), VpsService, LifecycleObserver {

    private companion object {
        const val ACCESS_FINE_LOCATION_REQUEST_CODE = 1000
        const val CAMERA_PERMISSION_REQUEST_CODE = 1010

        const val MIN_INTERVAL_MS = 1000L
        const val MIN_DISTANCE_IN_METERS = 1f
    }

    override val worldNode: Node
        get() = arManager.worldNode

    private val _requestPermissions: MutableSharedFlow<Pair<Array<String>, Int>> = MutableSharedFlow()
    val requestPermissions: SharedFlow<Pair<Array<String>, Int>> = _requestPermissions.asSharedFlow()

    private val _locationPermissionDialog: MutableSharedFlow<Unit> = MutableSharedFlow()
    val locationPermissionDialog: SharedFlow<Unit> = _locationPermissionDialog.asSharedFlow()

    private val _cameraPermissionDialog: MutableSharedFlow<Unit> = MutableSharedFlow()
    val cameraPermissionDialog: SharedFlow<Unit> = _cameraPermissionDialog.asSharedFlow()

    private var vpsJob: Job? = null
    private val vpsHandlerException: CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            Logger.error(throwable)
            viewModelScope.launch(Dispatchers.Main) {
                stopVpsService()
                vpsCallback?.onError(throwable)
            }
        }

    private var lastNodePosition: NodePositionModel = NodePositionModel()

    private lateinit var vpsConfig: VpsConfig
    private var vpsCallback: VpsCallback? = null

    private var gpsLocation: GpsLocationModel? = null
    private val locationListener: LocationListener by lazy {
        LocationListener { location ->
            gpsLocation = GpsLocationModel.from(location)
        }
    }
    private val locationManager: LocationManager by lazy {
        ContextCompat.getSystemService(application, LocationManager::class.java) as LocationManager
    }

    fun bindArSceneView(arSceneView: ArSceneView) {
        arManager.bindArSceneView(arSceneView)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        stopVpsService()
        arManager.destroy()
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
        this.vpsConfig = vpsConfig
    }

    override fun setVpsCallback(vpsCallback: VpsCallback) {
        this.vpsCallback = vpsCallback
    }

    override fun startVpsService() {
        if (vpsJob != null) return

        if (vpsConfig.needLocation && !checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            viewModelScope.launch {
                _requestPermissions.emit(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) to ACCESS_FINE_LOCATION_REQUEST_CODE
                )
            }
        } else {
            internalStartVpsService()
        }
    }

    override fun stopVpsService() {
        vpsJob?.cancel()
        vpsJob = null
        locationManager.removeUpdates(locationListener)
    }

    private fun internalStartVpsService() {
        viewModelScope.launch(vpsHandlerException) {
            gpsLocation = null
            requestLocationIfNeed()

            var firstLocalize = true
            var failureCount = 0
            var force = vpsConfig.onlyForce
            vpsJob = launch(Dispatchers.Default) {
                while (isActive) {
                    if (!firstLocalize && failureCount >= 2 && !force) {
                        force = true
                    }

                    arManager.savePositions()

                    val image: Image
                    val currentNodePosition: NodePositionModel
                    withContext(Dispatchers.Main) {
                        image = arManager.acquireCameraImage()
                        currentNodePosition = arManager.getCurrentNodePosition(lastNodePosition)
                    }

                    vpsInteractor.calculateNodePosition(
                        url = vpsConfig.url,
                        locationID = vpsConfig.locationID,
                        image = image,
                        isNeuro = vpsConfig.isNeuro,
                        nodePosition = currentNodePosition,
                        force = force,
                        gpsLocation = gpsLocation
                    )?.let {
                        firstLocalize = false
                        lastNodePosition = it
                        withContext(Dispatchers.Main) {
                            arManager.updatePositions(lastNodePosition)
                        }
                        vpsCallback?.onSuccess()
                    } ?: run {
                        failureCount++
                        Logger.debug("localization fail: $failureCount")
                    }
                    delay(vpsConfig.timerInterval)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationIfNeed() {
        if (vpsConfig.needLocation && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_INTERVAL_MS,
                MIN_DISTANCE_IN_METERS,
                locationListener
            )
        }
    }

    private fun checkResultLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            internalStartVpsService()
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