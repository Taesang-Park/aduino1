package com.example.aduino1.presentation.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduino1.data.local.database.WaterDatabase
import com.example.aduino1.data.repository.SettingsRepository
import com.example.aduino1.data.repository.WaterRepository
import com.example.aduino1.domain.calculator.IntervalCalculator
import com.example.aduino1.domain.model.HydrationInterval
import com.example.aduino1.domain.model.HydrationSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * Statistics 화면 ViewModel
 * 시간별 섭취량 통계 및 구간별 달성률 표시
 */
class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WaterRepository
    private val settingsRepository: SettingsRepository

    init {
        val database = WaterDatabase.getDatabase(application)
        repository = WaterRepository(database.waterDao())
        settingsRepository = SettingsRepository(database.settingsDao())
    }

    // 설정
    val settings: StateFlow<HydrationSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HydrationSettings.default()
        )

    // 선택된 날짜
    private val _selectedDate = MutableStateFlow(WaterRepository.getCurrentDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // 시간별 섭취량
    private val _hourlyIntake = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val hourlyIntake: StateFlow<Map<Int, Int>> = _hourlyIntake.asStateFlow()

    // 구간별 정보
    private val _intervalStats = MutableStateFlow<List<HydrationInterval>>(emptyList())
    val intervalStats: StateFlow<List<HydrationInterval>> = _intervalStats.asStateFlow()

    // 총 섭취량
    val totalIntake: StateFlow<Int> = _hourlyIntake.map { hourlyMap ->
        hourlyMap.values.sum()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        // 날짜 및 설정 변경 시 데이터 로드
        viewModelScope.launch {
            combine(_selectedDate, settings) { date, currentSettings ->
                Pair(date, currentSettings)
            }.collect { (date, currentSettings) ->
                loadHourlyIntake(date)
                loadIntervalStats(date, currentSettings)
            }
        }
    }

    /**
     * 날짜 변경
     */
    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    /**
     * 오늘 날짜로 이동
     */
    fun selectToday() {
        _selectedDate.value = WaterRepository.getCurrentDate()
    }

    /**
     * 이전 날짜로 이동
     */
    fun selectPreviousDay() {
        val calendar = Calendar.getInstance()
        calendar.time = WaterRepository.parseDate(_selectedDate.value)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        _selectedDate.value = WaterRepository.formatDate(calendar.time)
    }

    /**
     * 다음 날짜로 이동
     */
    fun selectNextDay() {
        val calendar = Calendar.getInstance()
        calendar.time = WaterRepository.parseDate(_selectedDate.value)
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        // 미래 날짜는 선택 불가
        val today = Calendar.getInstance()
        if (calendar.before(today) || calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            _selectedDate.value = WaterRepository.formatDate(calendar.time)
        }
    }

    /**
     * 시간별 섭취량 로드
     */
    private suspend fun loadHourlyIntake(date: String) {
        val hourlyData = repository.getHourlyIntake(date)
        _hourlyIntake.value = hourlyData
    }

    /**
     * 구간별 통계 로드
     */
    private suspend fun loadIntervalStats(date: String, currentSettings: HydrationSettings) {
        // 구간 계산
        val intervals = IntervalCalculator.calculateTodayIntervals(currentSettings)

        // 구간별 섭취량 데이터 로드
        val intervalTimeRanges = intervals.map { interval ->
            Pair(interval.startTime, interval.endTime)
        }
        val intakeByInterval = repository.getIntakeByInterval(date, intervalTimeRanges)

        // 구간별 데이터 업데이트
        val updatedIntervals = intervals.map { interval ->
            val (amount, recordCount) = intakeByInterval[interval.intervalNumber] ?: Pair(0, 0)
            interval.copy(
                currentAmount = amount,
                recordCount = recordCount
            )
        }

        _intervalStats.value = updatedIntervals
    }

    /**
     * 전체 달성률 계산
     */
    fun getOverallAchievementRate(): Float {
        val intervals = _intervalStats.value
        if (intervals.isEmpty()) return 0f

        val totalGoal = intervals.sumOf { it.goalAmount }
        val totalCurrent = intervals.sumOf { it.currentAmount }

        return if (totalGoal > 0) {
            (totalCurrent.toFloat() / totalGoal.toFloat()) * 100
        } else {
            0f
        }
    }

    /**
     * 최대 시간별 섭취량 (차트 스케일용)
     */
    fun getMaxHourlyIntake(): Int {
        val max = _hourlyIntake.value.values.maxOrNull() ?: 0
        return if (max > 0) ((max / 100) + 1) * 100 else 500 // 100 단위로 올림
    }
}
