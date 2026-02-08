package com.lbs.dr.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lbs.dr.data.repository.PositionRepository
import com.lbs.dr.domain.algorithm.fusion.IndoorPositioningEngine
import com.lbs.dr.domain.model.*
import com.lbs.dr.sensor.barometer.BarometerCollector
import com.lbs.dr.sensor.ble.BleBeaconCollector
import com.lbs.dr.sensor.imu.ImuCollector
import com.lbs.dr.sensor.wifi.WifiCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val positioningEngine: IndoorPositioningEngine,
    private val imuCollector: ImuCollector,
    private val barometerCollector: BarometerCollector,
    private val wifiCollector: WifiCollector,
    private val bleCollector: BleBeaconCollector,
    private val repository: PositionRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Position trail for map display
    private val _positionTrail = MutableStateFlow<List<Position>>(emptyList())
    val positionTrail: StateFlow<List<Position>> = _positionTrail.asStateFlow()

    private var trackingJob: Job? = null
    private var saveCounter = 0

    fun startTracking() {
        if (trackingJob?.isActive == true) return

        repository.startNewSession()

        _uiState.update { it.copy(isTracking = true) }

        trackingJob = viewModelScope.launch {
            // Collect IMU data (highest rate)
            launch {
                imuCollector.readings().collect { imu ->
                    val position = positioningEngine.processImu(imu)
                    val motionState = positioningEngine.getMotionState()

                    _uiState.update {
                        it.copy(
                            currentPosition = position,
                            motionState = motionState,
                            stepCount = it.stepCount + if (!motionState.isStationary) 1 else 0
                        )
                    }

                    // Add to trail (throttled)
                    saveCounter++
                    if (saveCounter % 50 == 0) { // ~1 Hz at 50 Hz IMU
                        _positionTrail.update { trail ->
                            (trail + position).takeLast(500)
                        }
                    }

                    // Save to DB periodically
                    if (saveCounter % 100 == 0) {
                        repository.savePosition(
                            position = position,
                            motionState = motionState,
                            floorInfo = positioningEngine.getFloorInfo(),
                            accel = floatArrayOf(imu.ax, imu.ay, imu.az)
                        )
                    }
                }
            }

            // Collect barometer data
            launch {
                barometerCollector.readings().collect { pressure ->
                    val floorInfo = positioningEngine.processPressure(pressure)
                    _uiState.update {
                        it.copy(
                            floorInfo = floorInfo,
                            pressure = pressure.pressureHPa,
                            temperature = pressure.temperatureC
                        )
                    }
                }
            }

            // Collect WiFi scans
            launch {
                wifiCollector.readings().collect { wifiScan ->
                    // TODO: load AP positions from configuration/map
                    val apPositions = emptyMap<String, Pair<Double, Double>>()
                    positioningEngine.processWifi(wifiScan, apPositions)

                    _uiState.update {
                        it.copy(
                            wifiApCount = wifiScan.accessPoints.size,
                            rttApCount = wifiScan.accessPoints.count { ap -> ap.rttSupported }
                        )
                    }
                }
            }

            // Collect BLE beacon scans
            launch {
                bleCollector.readings().collect { bleScan ->
                    // TODO: load beacon positions from configuration/map
                    val beaconPositions = emptyMap<String, Pair<Double, Double>>()
                    positioningEngine.processBle(bleScan, beaconPositions)

                    _uiState.update {
                        it.copy(beaconCount = bleScan.beacons.size)
                    }
                }
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        saveCounter = 0
        _uiState.update { it.copy(isTracking = false) }
    }

    fun initializePosition(latitude: Double, longitude: Double, altitude: Double, floor: Int) {
        IndoorPositioningEngine.setReferencePoint(latitude, longitude)
        positioningEngine.initialize(latitude, longitude, altitude, floor)
        _uiState.update {
            it.copy(
                currentPosition = Position(
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                    floor = floor,
                    accuracy = 0f,
                    source = PositionSource.FUSED
                )
            )
        }
    }

    fun setReferenceFloor(floor: Int) {
        val pressure = _uiState.value.pressure ?: return
        positioningEngine.setReferenceFloor(floor, pressure)
    }

    fun resetEngine() {
        positioningEngine.reset()
        _positionTrail.value = emptyList()
        _uiState.update { MainUiState() }
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}

data class MainUiState(
    val isTracking: Boolean = false,
    val currentPosition: Position? = null,
    val motionState: MotionState? = null,
    val floorInfo: FloorInfo? = null,
    val pressure: Float? = null,
    val temperature: Float? = null,
    val stepCount: Int = 0,
    val wifiApCount: Int = 0,
    val rttApCount: Int = 0,
    val beaconCount: Int = 0
)
