package com.example.aduino1.presentation.home

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduino1.data.local.database.WaterDatabase
import com.example.aduino1.data.repository.WaterRepository
import com.example.aduino1.domain.model.BluetoothConnectionState
import com.example.aduino1.domain.model.DailyWaterIntake
import com.example.aduino1.presentation.bluetooth.BluetoothManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Home 화면 ViewModel
 * 블루투스 연결, 센서 데이터, 물 섭취량 관리
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothManager(application)
    private val repository: WaterRepository

    init {
        val database = WaterDatabase.getDatabase(application)
        repository = WaterRepository(database.waterDao())
    }

    // 블루투스 연결 상태
    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothManager.connectionState

    // 현재 무게
    val currentWeight: StateFlow<Float> = bluetoothManager.currentWeight

    // 연결된 디바이스 이름
    val connectedDeviceName: StateFlow<String?> = bluetoothManager.connectedDeviceName

    // 목표 섭취량 (나중에 DataStore로 관리 가능)
    private val _goalAmount = MutableStateFlow(2000) // 기본 2L
    val goalAmount: StateFlow<Int> = _goalAmount.asStateFlow()

    // 오늘의 섭취량
    val todayIntake: StateFlow<DailyWaterIntake> = repository.getTodayIntake(_goalAmount.value)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyWaterIntake.empty(
                WaterRepository.getCurrentDate(),
                _goalAmount.value
            )
        )

    // UI 이벤트
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        // 마신 양 감지 시 자동으로 기록 추가
        viewModelScope.launch {
            bluetoothManager.drinkAmount.collect { amount ->
                if (amount != null && amount > 0) {
                    addWaterRecord(amount.toInt())
                }
            }
        }
    }

    /**
     * 블루투스 활성화 여부
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothManager.isBluetoothEnabled()
    }

    /**
     * 블루투스 권한 확인
     */
    fun hasBluetoothPermission(): Boolean {
        return bluetoothManager.hasBluetoothPermission()
    }

    /**
     * 페어링된 디바이스 목록
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothManager.getPairedDevices()
    }

    /**
     * 디바이스에 연결
     */
    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            try {
                bluetoothManager.connect(device)
                _uiEvent.emit(UiEvent.ShowMessage("${device.name}에 연결되었습니다"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("연결 실패: ${e.message}"))
            }
        }
    }

    /**
     * 연결 해제
     */
    fun disconnect() {
        bluetoothManager.disconnect()
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowMessage("연결이 해제되었습니다"))
        }
    }

    /**
     * 영점 조정
     */
    fun tare() {
        viewModelScope.launch {
            try {
                bluetoothManager.sendTareCommand()
                _uiEvent.emit(UiEvent.ShowMessage("영점 조정 중..."))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("영점 조정 실패: ${e.message}"))
            }
        }
    }

    /**
     * 수동으로 물 섭취 기록 추가
     */
    fun addWaterRecord(amount: Int) {
        viewModelScope.launch {
            try {
                repository.addWaterRecord(amount)
                _uiEvent.emit(UiEvent.RecordAdded(amount))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("기록 추가 실패: ${e.message}"))
            }
        }
    }

    /**
     * 목표량 설정
     */
    fun setGoalAmount(amount: Int) {
        _goalAmount.value = amount
    }

    /**
     * 리소스 정리
     */
    override fun onCleared() {
        super.onCleared()
        bluetoothManager.cleanup()
    }

    /**
     * UI 이벤트
     */
    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        data class ShowError(val error: String) : UiEvent()
        data class RecordAdded(val amount: Int) : UiEvent()
    }
}