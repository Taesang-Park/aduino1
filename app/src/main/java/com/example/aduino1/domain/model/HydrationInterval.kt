package com.example.aduino1.domain.model

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 수분 섭취 시간 구간
 *
 * 하루를 여러 구간으로 나누어 각 구간별 섭취량을 추적합니다.
 */
data class HydrationInterval(
    val intervalNumber: Int,            // 구간 번호 (1부터 시작)
    val startTime: Long,                // 구간 시작 시간 (타임스탬프)
    val endTime: Long,                  // 구간 종료 시간 (타임스탬프)
    val goalAmount: Int,                // 구간 목표량 (ml)
    val currentAmount: Int = 0,         // 현재 섭취량 (ml)
    val recordCount: Int = 0            // 기록 횟수
) {
    /**
     * 달성률 계산 (0.0 ~ 1.0+)
     * @return 달성률
     */
    val achievementRate: Float
        get() = if (goalAmount > 0) currentAmount.toFloat() / goalAmount else 0f

    /**
     * 달성률 (퍼센트, 0 ~ 100+)
     * @return 퍼센트
     */
    val achievementPercent: Float
        get() = achievementRate * 100

    /**
     * 목표 달성 여부
     * @return 달성했으면 true
     */
    val isGoalAchieved: Boolean
        get() = currentAmount >= goalAmount

    /**
     * 남은 목표량
     * @return 남은 양 (ml), 최소 0
     */
    val remainingAmount: Int
        get() = (goalAmount - currentAmount).coerceAtLeast(0)

    /**
     * 구간 상태
     * @return IntervalStatus
     */
    val status: IntervalStatus
        get() {
            val now = System.currentTimeMillis()
            return when {
                now < startTime -> IntervalStatus.UPCOMING
                now > endTime -> IntervalStatus.COMPLETED
                else -> IntervalStatus.ACTIVE
            }
        }

    /**
     * LED 색상 명령
     * @return 달성률에 따른 LED 색상
     */
    val ledColor: LedColorCommand
        get() = LedColorCommand.fromAchievementPercentage(achievementPercent)

    /**
     * 시간 형식 포매터
     */
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * 구간 시간 문자열
     * @return "08:00-10:00" 형식
     */
    fun getTimeRangeString(): String {
        return "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
    }

    /**
     * 남은 시간 (밀리초)
     * @return 구간 종료까지 남은 시간
     */
    fun getRemainingTimeMillis(): Long {
        val now = System.currentTimeMillis()
        return if (now < endTime) endTime - now else 0
    }

    /**
     * 남은 시간 문자열
     * @return "1시간 30분" 형식
     */
    fun getRemainingTimeString(): String {
        val remainingMillis = getRemainingTimeMillis()
        if (remainingMillis <= 0) return "완료"

        val hours = remainingMillis / (1000 * 60 * 60)
        val minutes = (remainingMillis % (1000 * 60 * 60)) / (1000 * 60)

        return buildString {
            if (hours > 0) append("${hours}시간 ")
            if (minutes > 0) append("${minutes}분")
            if (hours == 0L && minutes == 0L) append("1분 미만")
        }.trim()
    }

    /**
     * 경과 시간 (밀리초)
     * @return 구간 시작부터 경과한 시간
     */
    fun getElapsedTimeMillis(): Long {
        val now = System.currentTimeMillis()
        return if (now > startTime) now - startTime else 0
    }

    /**
     * 구간 진행률 (0.0 ~ 1.0)
     * @return 시간 기준 진행률
     */
    val timeProgressRate: Float
        get() {
            val total = (endTime - startTime).toFloat()
            val elapsed = getElapsedTimeMillis().toFloat()
            return if (total > 0) (elapsed / total).coerceIn(0f, 1f) else 0f
        }

    companion object {
        /**
         * 빈 구간 생성 (초기값용)
         * @return 기본값으로 채워진 구간
         */
        fun empty(intervalNumber: Int = 1, goalAmount: Int = 250): HydrationInterval {
            val now = System.currentTimeMillis()
            return HydrationInterval(
                intervalNumber = intervalNumber,
                startTime = now,
                endTime = now + 2 * 3600 * 1000, // 2시간 후
                goalAmount = goalAmount
            )
        }
    }
}