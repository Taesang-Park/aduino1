package com.example.aduino1.data.repository

import com.example.aduino1.data.local.database.HourlyIntake
import com.example.aduino1.data.local.database.WaterDao
import com.example.aduino1.data.local.entity.WaterRecord
import com.example.aduino1.domain.model.DailyWaterIntake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

/**
 * Water Repository
 * 데이터 계층과 비즈니스 로직을 분리하는 중간 계층
 */
class WaterRepository(private val waterDao: WaterDao) {

    /**
     * 새로운 물 섭취 기록 추가
     */
    suspend fun addWaterRecord(amount: Int): Long {
        val record = WaterRecord(amount = amount)
        return waterDao.insert(record)
    }

    /**
     * 수동으로 기록 추가 (타임스탬프 지정)
     */
    suspend fun addManualRecord(amount: Int, timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val date = formatDate(calendar.time)

        val record = WaterRecord(
            amount = amount,
            timestamp = timestamp,
            date = date
        )
        return waterDao.insert(record)
    }

    /**
     * 기록 삭제
     */
    suspend fun deleteRecord(record: WaterRecord) {
        waterDao.delete(record)
    }

    /**
     * ID로 기록 삭제
     */
    suspend fun deleteRecordById(id: Long) {
        waterDao.deleteById(id)
    }

    /**
     * 특정 날짜의 기록 조회
     */
    fun getRecordsByDate(date: String): Flow<List<WaterRecord>> {
        return waterDao.getRecordsByDate(date)
    }

    /**
     * 특정 날짜의 일일 섭취량 조회 (Flow)
     */
    fun getDailyIntake(date: String, goalAmount: Int = 2000): Flow<DailyWaterIntake> {
        return waterDao.getRecordsByDate(date).map { records ->
            DailyWaterIntake(
                date = date,
                totalAmount = records.sumOf { it.amount },
                goalAmount = goalAmount,
                recordCount = records.size
            )
        }
    }

    /**
     * 오늘의 섭취량 조회 (Flow)
     */
    fun getTodayIntake(goalAmount: Int = 2000): Flow<DailyWaterIntake> {
        val today = getCurrentDate()
        return getDailyIntake(today, goalAmount)
    }

    /**
     * 특정 날짜 범위의 기록 조회
     */
    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<WaterRecord>> {
        return waterDao.getRecordsByDateRange(startDate, endDate)
    }

    /**
     * 주간 통계 조회 (최근 7일)
     */
    suspend fun getWeeklyStats(goalAmount: Int = 2000): List<DailyWaterIntake> {
        val calendar = Calendar.getInstance()
        val endDate = formatDate(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = formatDate(calendar.time)

        val dailyTotals = waterDao.getDailyTotalsByDateRange(startDate, endDate)
        val dailyTotalsMap = dailyTotals.associate { it.date to it.total }

        // 7일 간의 데이터 생성 (기록이 없는 날은 0으로)
        val result = mutableListOf<DailyWaterIntake>()
        calendar.time = parseDate(startDate)

        repeat(7) {
            val date = formatDate(calendar.time)
            val total = dailyTotalsMap[date] ?: 0

            result.add(
                DailyWaterIntake(
                    date = date,
                    totalAmount = total,
                    goalAmount = goalAmount,
                    recordCount = 0
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return result
    }

    /**
     * 최근 기록 조회
     */
    fun getRecentRecords(limit: Int = 10): Flow<List<WaterRecord>> {
        return waterDao.getRecentRecords(limit)
    }

    /**
     * 전체 평균 일일 섭취량 조회
     */
    suspend fun getAverageDailyIntake(): Float {
        return waterDao.getAverageDailyIntake()
    }

    /**
     * 모든 기록 삭제
     */
    suspend fun deleteAllRecords() {
        waterDao.deleteAll()
    }

    /**
     * 전체 기록 수 조회
     */
    suspend fun getRecordCount(): Int {
        return waterDao.getRecordCount()
    }

    /**
     * 특정 시간 구간의 섭취량 조회 (intervalNumber별)
     * @param date 날짜 (yyyy-MM-dd)
     * @param intervals 구간 리스트 (startTime, endTime 포함)
     * @return 구간 번호 -> (섭취량, 기록 수) 맵
     */
    suspend fun getIntakeByInterval(
        date: String,
        intervals: List<Pair<Long, Long>>
    ): Map<Int, Pair<Int, Int>> {
        val result = mutableMapOf<Int, Pair<Int, Int>>()

        intervals.forEachIndexed { index, (startTime, endTime) ->
            val records = waterDao.getRecordsByTimestampRange(startTime, endTime)
            val totalAmount = records.sumOf { it.amount }
            val recordCount = records.size
            result[index + 1] = Pair(totalAmount, recordCount)
        }

        return result
    }

    /**
     * 특정 날짜의 시간별 섭취량 조회
     * @param date 날짜 (yyyy-MM-dd)
     * @return 시간(0-23) -> 섭취량(ml) 맵 (24시간 전체, 없는 시간은 0)
     */
    suspend fun getHourlyIntake(date: String): Map<Int, Int> {
        val hourlyData = waterDao.getHourlyIntakeByDate(date)
        val hourlyMap = hourlyData.associate { it.hour to it.total }

        // 24시간 전체를 0으로 초기화하고 데이터가 있는 시간만 업데이트
        return (0..23).associateWith { hour ->
            hourlyMap[hour] ?: 0
        }
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun getCurrentDate(): String {
            return dateFormat.format(Date())
        }

        fun formatDate(date: Date): String {
            return dateFormat.format(date)
        }

        fun parseDate(dateString: String): Date {
            return dateFormat.parse(dateString) ?: Date()
        }

        /**
         * N일 전 날짜 가져오기
         */
        fun getDateDaysAgo(days: Int): String {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            return formatDate(calendar.time)
        }
    }
}