package com.example.aduino1.presentation.home

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduino1.data.local.database.WaterDatabase
import com.example.aduino1.data.repository.SettingsRepository
import com.example.aduino1.data.repository.WaterRepository
import com.example.aduino1.domain.calculator.IntervalCalculator
import com.example.aduino1.domain.model.BluetoothConnectionState
import com.example.aduino1.domain.model.DailyWaterIntake
import com.example.aduino1.domain.model.HydrationInterval
import com.example.aduino1.domain.model.HydrationSettings
import com.example.aduino1.domain.model.LedColorCommand
import com.example.aduino1.presentation.bluetooth.BluetoothManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Home 화면 ViewModel
 * 블루투스 연결, 센서 데이터, 물 섭취량 관리
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothManager(application)
    private val repository: WaterRepository
    private val settingsRepository: SettingsRepository

    init {
        val database = WaterDatabase.getDatabase(application)
        repository = WaterRepository(database.waterDao())
        settingsRepository = SettingsRepository(database.settingsDao())

        // 설정 초기화
        viewModelScope.launch {
            settingsRepository.initializeIfNeeded()
        }
    }

    // 블루투스 연결 상태
    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothManager.connectionState

    // 현재 무게
    val currentWeight: StateFlow<Float> = bluetoothManager.currentWeight

    // 연결된 디바이스 이름
    val connectedDeviceName: StateFlow<String?> = bluetoothManager.connectedDeviceName

    // 수분 섭취 설정
    val settings: StateFlow<HydrationSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HydrationSettings.default()
        )

    // 오늘의 섭취량 (설정 기반)
    val todayIntake: StateFlow<DailyWaterIntake> = settings.flatMapLatest { currentSettings ->
        repository.getTodayIntake(currentSettings.dailyGoal)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyWaterIntake.empty(
            WaterRepository.getCurrentDate(),
            2000
        )
    )

    // 오늘의 구간 정보
    private val _todayIntervals = MutableStateFlow<List<HydrationInterval>>(emptyList())
    val todayIntervals: StateFlow<List<HydrationInterval>> = _todayIntervals.asStateFlow()

    // 현재 구간 정보
    private val _currentInterval = MutableStateFlow<HydrationInterval?>(null)
    val currentInterval: StateFlow<HydrationInterval?> = _currentInterval.asStateFlow()

    // 마지막 LED 색상 (중복 전송 방지)
    private var lastSentLedColor: LedColorCommand? = null

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

        // 설정 및 섭취량 변경 시 구간 정보 업데이트
        viewModelScope.launch {
            combine(settings, todayIntake) { currentSettings, _ ->
                currentSettings
            }.collect { currentSettings ->
                updateIntervals(currentSettings)
            }
        }

        // 1분마다 현재 구간 체크 및 LED 업데이트
        viewModelScope.launch {
            while (true) {
                delay(60_000) // 1분
                updateCurrentInterval()
                updateLedColor()
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
     * 구간 정보 업데이트
     */
    private suspend fun updateIntervals(currentSettings: HydrationSettings) {
        val today = WaterRepository.getCurrentDate()

        // 오늘의 구간별 타임스탬프 생성
        val intervals = IntervalCalculator.calculateTodayIntervals(currentSettings)

        // 구간별 섭취량 데이터 로드
        val intervalTimeRanges = intervals.map { interval ->
            Pair(interval.startTime, interval.endTime)
        }
        val intakeByInterval = repository.getIntakeByInterval(today, intervalTimeRanges)

        // 구간별 데이터 업데이트
        val updatedIntervals = intervals.map { interval ->
            val (amount, recordCount) = intakeByInterval[interval.intervalNumber] ?: Pair(0, 0)
            interval.copy(
                currentAmount = amount,
                recordCount = recordCount
            )
        }

        _todayIntervals.value = updatedIntervals
        updateCurrentInterval()
        updateLedColor()
    }

    /**
     * 현재 구간 업데이트
     */
    private fun updateCurrentInterval() {
        val intervals = _todayIntervals.value
        _currentInterval.value = IntervalCalculator.getCurrentInterval(intervals)
    }

    /**
     * LED 색상 업데이트 (블루투스 연결 시)
     */
    private fun updateLedColor() {
        viewModelScope.launch {
            val interval = _currentInterval.value ?: return@launch
            val newColor = interval.ledColor

            // 연결 상태 확인 및 중복 전송 방지
            if (connectionState.value == BluetoothConnectionState.CONNECTED &&
                newColor != lastSentLedColor) {
                try {
                    bluetoothManager.sendColorCommand(newColor)
                    lastSentLedColor = newColor
                } catch (e: Exception) {
                    // LED 업데이트 실패는 로그만 남기고 무시 (중요하지 않은 기능)
                    android.util.Log.w("HomeViewModel", "Failed to update LED color", e)
                }
            }
        }
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