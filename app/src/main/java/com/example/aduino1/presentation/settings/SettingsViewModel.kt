package com.example.aduino1.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduino1.data.local.database.WaterDatabase
import com.example.aduino1.data.repository.SettingsRepository
import com.example.aduino1.domain.model.HydrationSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Settings 화면 ViewModel
 * 수분 섭취 목표 및 시간 간격 설정 관리
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository

    init {
        val database = WaterDatabase.getDatabase(application)
        settingsRepository = SettingsRepository(database.settingsDao())
    }

    // 현재 설정
    val settings: StateFlow<HydrationSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HydrationSettings.default()
        )

    // UI 상태
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // UI 이벤트
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        // 현재 설정으로 UI 상태 초기화
        viewModelScope.launch {
            settings.collect { currentSettings ->
                _uiState.value = _uiState.value.copy(
                    dailyGoal = currentSettings.dailyGoal,
                    intervalHours = currentSettings.intervalHours,
                    wakingHours = currentSettings.wakingHours,
                    startTimeHour = currentSettings.startTimeHour
                )
            }
        }
    }

    /**
     * 일일 목표량 업데이트
     */
    fun updateDailyGoal(goal: Int) {
        _uiState.value = _uiState.value.copy(dailyGoal = goal)
    }

    /**
     * 시간 간격 업데이트
     */
    fun updateIntervalHours(hours: Float) {
        _uiState.value = _uiState.value.copy(intervalHours = hours)
    }

    /**
     * 활동 시간 업데이트
     */
    fun updateWakingHours(hours: Int) {
        _uiState.value = _uiState.value.copy(wakingHours = hours)
    }

    /**
     * 시작 시간 업데이트
     */
    fun updateStartTimeHour(hour: Int) {
        _uiState.value = _uiState.value.copy(startTimeHour = hour)
    }

    /**
     * 설정 저장
     */
    fun saveSettings() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val newSettings = HydrationSettings(
                    id = 1,
                    dailyGoal = currentState.dailyGoal,
                    intervalHours = currentState.intervalHours,
                    wakingHours = currentState.wakingHours,
                    startTimeHour = currentState.startTimeHour
                )

                settingsRepository.saveSettings(newSettings)
                _uiEvent.emit(UiEvent.SettingsSaved)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("설정 저장 실패: ${e.message}"))
            }
        }
    }

    /**
     * 설정 초기화
     */
    fun resetToDefault() {
        viewModelScope.launch {
            try {
                settingsRepository.resetToDefault()
                _uiEvent.emit(UiEvent.SettingsReset)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("초기화 실패: ${e.message}"))
            }
        }
    }

    /**
     * 유효성 검사
     */
    fun validateSettings(): Boolean {
        val state = _uiState.value
        return when {
            state.dailyGoal < 500 || state.dailyGoal > 5000 -> {
                viewModelScope.launch {
                    _uiEvent.emit(UiEvent.ShowError("일일 목표는 500ml ~ 5000ml 사이여야 합니다"))
                }
                false
            }
            state.intervalHours < 0.5f || state.intervalHours > 8f -> {
                viewModelScope.launch {
                    _uiEvent.emit(UiEvent.ShowError("시간 간격은 0.5시간 ~ 8시간 사이여야 합니다"))
                }
                false
            }
            state.wakingHours < 8 || state.wakingHours > 20 -> {
                viewModelScope.launch {
                    _uiEvent.emit(UiEvent.ShowError("활동 시간은 8시간 ~ 20시간 사이여야 합니다"))
                }
                false
            }
            state.startTimeHour < 0 || state.startTimeHour > 23 -> {
                viewModelScope.launch {
                    _uiEvent.emit(UiEvent.ShowError("시작 시간은 0시 ~ 23시 사이여야 합니다"))
                }
                false
            }
            else -> true
        }
    }

    /**
     * UI 상태
     */
    data class SettingsUiState(
        val dailyGoal: Int = 2000,
        val intervalHours: Float = 2f,
        val wakingHours: Int = 16,
        val startTimeHour: Int = 8
    ) {
        // 계산된 값들
        val timesPerDay: Int
            get() = (wakingHours / intervalHours).toInt()

        val goalPerInterval: Int
            get() = if (timesPerDay > 0) dailyGoal / timesPerDay else dailyGoal

        val intervalMinutes: Int
            get() = (intervalHours * 60).toInt()
    }

    /**
     * UI 이벤트
     */
    sealed class UiEvent {
        object SettingsSaved : UiEvent()
        object SettingsReset : UiEvent()
        data class ShowError(val message: String) : UiEvent()
    }
}
