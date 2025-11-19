package com.example.aduino1.domain.model

/**
 * 일별 물 섭취량 모델
 */
data class DailyWaterIntake(
    val date: String,
    val totalAmount: Int,      // 총 섭취량 (ml)
    val goalAmount: Int = 2000, // 목표량 (ml) - 기본 2L
    val recordCount: Int = 0    // 기록 횟수
) {
    /**
     * 목표 달성률 (0.0 ~ 1.0)
     */
    val achievementRate: Float
        get() = if (goalAmount > 0) (totalAmount.toFloat() / goalAmount).coerceIn(0f, 1f) else 0f

    /**
     * 목표 달성률 (퍼센트)
     */
    val achievementPercent: Int
        get() = (achievementRate * 100).toInt()

    /**
     * 목표 달성 여부
     */
    val isGoalAchieved: Boolean
        get() = totalAmount >= goalAmount

    /**
     * 남은 양 (ml)
     */
    val remainingAmount: Int
        get() = (goalAmount - totalAmount).coerceAtLeast(0)

    companion object {
        /**
         * 빈 데이터 생성
         */
        fun empty(date: String, goalAmount: Int = 2000) = DailyWaterIntake(
            date = date,
            totalAmount = 0,
            goalAmount = goalAmount,
            recordCount = 0
        )
    }
}

/**
 * 주간 물 섭취 통계
 */
data class WeeklyWaterStats(
    val startDate: String,
    val endDate: String,
    val dailyIntakes: List<DailyWaterIntake>,
    val averageAmount: Int,
    val totalAmount: Int,
    val achievedDays: Int,
    val totalDays: Int
) {
    /**
     * 평균 달성률
     */
    val averageAchievementRate: Float
        get() {
            if (dailyIntakes.isEmpty()) return 0f
            return dailyIntakes.map { it.achievementRate }.average().toFloat()
        }
}

/**
 * 블루투스 연결 상태
 */
enum class BluetoothConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * 센서 데이터
 */
data class SensorData(
    val currentWeight: Float,   // 현재 무게 (g)
    val drinkAmount: Float,     // 마신 양 (ml)
    val timestamp: Long = System.currentTimeMillis()
)