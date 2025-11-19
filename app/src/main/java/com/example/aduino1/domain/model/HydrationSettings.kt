package com.example.aduino1.domain.model

/**
 * 수분 섭취 설정
 *
 * 사용자의 하루 목표 및 시간 간격 설정을 관리합니다.
 */
data class HydrationSettings(
    val id: Long = 1,                       // 설정 ID (항상 1개만 존재)
    val dailyGoal: Int = 2000,              // 하루 목표량 (ml)
    val intervalHours: Float = 2f,          // 시간 간격 (시간)
    val wakingHours: Int = 16,              // 깨어있는 시간 (시간)
    val startTimeHour: Int = 8              // 하루 시작 시간 (0-23시)
) {
    /**
     * 하루에 몇 번 마셔야 하는지 계산
     * @return 회차 수
     */
    val timesPerDay: Int
        get() = (wakingHours / intervalHours).toInt()

    /**
     * 한 번(한 구간)에 마셔야 할 목표량
     * @return 구간당 목표량 (ml)
     */
    val goalPerInterval: Int
        get() = if (timesPerDay > 0) dailyGoal / timesPerDay else dailyGoal

    /**
     * 시간 간격 (밀리초 단위)
     * @return 밀리초
     */
    val intervalMillis: Long
        get() = (intervalHours * 3600 * 1000).toLong()

    /**
     * 설정이 유효한지 검증
     * @return 유효하면 true
     */
    fun isValid(): Boolean {
        return dailyGoal > 0 &&
                intervalHours > 0 &&
                wakingHours > 0 &&
                startTimeHour in 0..23 &&
                intervalHours <= wakingHours
    }

    /**
     * 설정 요약 문자열
     * @return "2000ml / 2시간마다 / 8회"
     */
    fun getSummary(): String {
        return "${dailyGoal}ml / ${intervalHours}시간마다 / ${timesPerDay}회"
    }

    companion object {
        /**
         * 기본 설정 생성
         * @return 기본값으로 초기화된 설정
         */
        fun default() = HydrationSettings()

        /**
         * 사전 정의된 간격 옵션
         */
        val INTERVAL_OPTIONS = listOf(1f, 2f, 3f, 4f)
    }
}