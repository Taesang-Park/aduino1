package com.example.aduino1.domain.calculator

import com.example.aduino1.domain.model.HydrationInterval
import com.example.aduino1.domain.model.HydrationSettings
import java.util.Calendar

/**
 * 시간 구간 계산기
 *
 * 설정값을 기반으로 하루의 시간 구간을 계산하고 관리합니다.
 */
object IntervalCalculator {

    /**
     * 오늘의 모든 구간 생성
     * @param settings 수분 섭취 설정
     * @param intakeByInterval 구간별 섭취량 맵 (intervalNumber -> amount)
     * @return 오늘의 모든 구간 리스트
     */
    fun calculateTodayIntervals(
        settings: HydrationSettings,
        intakeByInterval: Map<Int, Pair<Int, Int>> = emptyMap() // intervalNumber -> (amount, count)
    ): List<HydrationInterval> {
        val startOfDay = getStartOfDayTimestamp(settings.startTimeHour)
        val intervals = mutableListOf<HydrationInterval>()

        for (i in 0 until settings.timesPerDay) {
            val intervalNumber = i + 1
            val intervalStart = startOfDay + (i * settings.intervalMillis)
            val intervalEnd = intervalStart + settings.intervalMillis

            val (amount, count) = intakeByInterval[intervalNumber] ?: Pair(0, 0)

            intervals.add(
                HydrationInterval(
                    intervalNumber = intervalNumber,
                    startTime = intervalStart,
                    endTime = intervalEnd,
                    goalAmount = settings.goalPerInterval,
                    currentAmount = amount,
                    recordCount = count
                )
            )
        }

        return intervals
    }

    /**
     * 현재 활성 구간 찾기
     * @param intervals 전체 구간 리스트
     * @return 현재 활성 구간, 없으면 null
     */
    fun getCurrentInterval(intervals: List<HydrationInterval>): HydrationInterval? {
        val now = System.currentTimeMillis()
        return intervals.find { now in it.startTime..it.endTime }
    }

    /**
     * 특정 타임스탬프가 속한 구간 번호 계산
     * @param timestamp 타임스탬프
     * @param settings 설정
     * @return 구간 번호 (1부터 시작), 범위 밖이면 null
     */
    fun getIntervalNumberForTimestamp(
        timestamp: Long,
        settings: HydrationSettings
    ): Int? {
        val startOfDay = getStartOfDayTimestamp(settings.startTimeHour)
        val endOfDay = startOfDay + (settings.wakingHours * 3600 * 1000)

        if (timestamp < startOfDay || timestamp >= endOfDay) {
            return null
        }

        val elapsedMillis = timestamp - startOfDay
        val intervalNumber = (elapsedMillis / settings.intervalMillis).toInt() + 1

        return if (intervalNumber in 1..settings.timesPerDay) {
            intervalNumber
        } else {
            null
        }
    }

    /**
     * 다음 구간 시작 시간까지 남은 시간 (밀리초)
     * @param currentInterval 현재 구간
     * @return 남은 시간 (밀리초)
     */
    fun getTimeUntilNextInterval(currentInterval: HydrationInterval?): Long {
        if (currentInterval == null) return 0
        val now = System.currentTimeMillis()
        return if (currentInterval.endTime > now) {
            currentInterval.endTime - now
        } else {
            0
        }
    }

    /**
     * 하루 시작 시간의 타임스탬프 계산
     * @param startHour 시작 시간 (0-23)
     * @return 오늘 시작 시간의 타임스탬프
     */
    fun getStartOfDayTimestamp(startHour: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * 하루 종료 시간의 타임스탬프 계산
     * @param settings 설정
     * @return 오늘 종료 시간의 타임스탬프
     */
    fun getEndOfDayTimestamp(settings: HydrationSettings): Long {
        return getStartOfDayTimestamp(settings.startTimeHour) +
                (settings.wakingHours * 3600 * 1000)
    }

    /**
     * 날짜 변경 감지 (자정이 지났는지)
     * @param lastCheckTimestamp 마지막 체크 시간
     * @return 날짜가 바뀌었으면 true
     */
    fun hasDateChanged(lastCheckTimestamp: Long): Boolean {
        val lastDate = Calendar.getInstance().apply {
            timeInMillis = lastCheckTimestamp
        }
        val currentDate = Calendar.getInstance()

        return lastDate.get(Calendar.DAY_OF_YEAR) != currentDate.get(Calendar.DAY_OF_YEAR) ||
                lastDate.get(Calendar.YEAR) != currentDate.get(Calendar.YEAR)
    }

    /**
     * 구간별 통계 계산
     * @param intervals 구간 리스트
     * @return 통계 정보
     */
    fun calculateIntervalStatistics(intervals: List<HydrationInterval>): IntervalStatistics {
        val completed = intervals.count { it.isGoalAchieved }
        val total = intervals.size
        val totalAmount = intervals.sumOf { it.currentAmount }
        val totalGoal = intervals.sumOf { it.goalAmount }

        return IntervalStatistics(
            totalIntervals = total,
            completedIntervals = completed,
            completionRate = if (total > 0) completed.toFloat() / total else 0f,
            totalAmount = totalAmount,
            totalGoal = totalGoal,
            overallAchievementRate = if (totalGoal > 0) totalAmount.toFloat() / totalGoal else 0f
        )
    }
}

/**
 * 구간별 통계
 */
data class IntervalStatistics(
    val totalIntervals: Int,            // 전체 구간 수
    val completedIntervals: Int,        // 달성한 구간 수
    val completionRate: Float,          // 완료율
    val totalAmount: Int,               // 총 섭취량
    val totalGoal: Int,                 // 총 목표량
    val overallAchievementRate: Float   // 전체 달성률
)