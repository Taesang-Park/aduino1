package com.example.aduino1.domain.model

/**
 * 시간 구간 상태
 *
 * 각 구간이 현재 어떤 상태인지를 나타냅니다.
 */
enum class IntervalStatus(val displayName: String) {
    UPCOMING("예정"),       // 아직 시작 안 됨
    ACTIVE("진행 중"),      // 현재 진행 중
    COMPLETED("완료");      // 종료됨

    /**
     * 활성 상태인지 확인
     * @return 진행 중이면 true
     */
    fun isActive(): Boolean = this == ACTIVE

    /**
     * 완료된 상태인지 확인
     * @return 완료됐으면 true
     */
    fun isCompleted(): Boolean = this == COMPLETED

    /**
     * 예정된 상태인지 확인
     * @return 예정이면 true
     */
    fun isUpcoming(): Boolean = this == UPCOMING
}