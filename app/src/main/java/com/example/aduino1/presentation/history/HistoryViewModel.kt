package com.example.aduino1.presentation.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aduino1.data.local.database.WaterDatabase
import com.example.aduino1.data.local.entity.WaterRecord
import com.example.aduino1.data.repository.WaterRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * History 화면 ViewModel
 * 물 섭취 기록 조회 및 관리
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WaterRepository

    init {
        val database = WaterDatabase.getDatabase(application)
        repository = WaterRepository(database.waterDao())
    }

    // 선택된 날짜
    private val _selectedDate = MutableStateFlow(WaterRepository.getCurrentDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // 선택된 날짜의 기록들
    val records: StateFlow<List<WaterRecord>> = _selectedDate.flatMapLatest { date ->
        repository.getRecordsByDate(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 일별 총량
    val dailyTotal: StateFlow<Int> = records.map { recordList ->
        recordList.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // UI 이벤트
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

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
        calendar.time = parseDate(_selectedDate.value)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        _selectedDate.value = formatDate(calendar.time)
    }

    /**
     * 다음 날짜로 이동
     */
    fun selectNextDay() {
        val calendar = Calendar.getInstance()
        calendar.time = parseDate(_selectedDate.value)
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        // 미래 날짜는 선택 불가
        val today = Calendar.getInstance()
        if (calendar.before(today) || calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            _selectedDate.value = formatDate(calendar.time)
        }
    }

    /**
     * 기록 삭제
     */
    fun deleteRecord(record: WaterRecord) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(record)
                _uiEvent.emit(UiEvent.RecordDeleted)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("삭제 실패: ${e.message}"))
            }
        }
    }

    /**
     * ID로 기록 삭제
     */
    fun deleteRecordById(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteRecordById(id)
                _uiEvent.emit(UiEvent.RecordDeleted)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("삭제 실패: ${e.message}"))
            }
        }
    }

    /**
     * 수동 기록 추가 (타임스탬프 지정)
     */
    fun addManualRecord(amount: Int, timestamp: Long) {
        viewModelScope.launch {
            try {
                repository.addManualRecord(amount, timestamp)
                _uiEvent.emit(UiEvent.RecordAdded)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("기록 추가 실패: ${e.message}"))
            }
        }
    }

    /**
     * 날짜 포맷팅 헬퍼
     */
    private fun formatDate(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }

    private fun parseDate(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.parse(dateString) ?: Date()
    }

    /**
     * UI 이벤트
     */
    sealed class UiEvent {
        object RecordDeleted : UiEvent()
        object RecordAdded : UiEvent()
        data class ShowError(val message: String) : UiEvent()
    }
}
