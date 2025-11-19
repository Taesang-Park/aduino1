package com.example.aduino1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aduino1.domain.model.HydrationSettings

/**
 * 수분 섭취 설정 Entity
 *
 * Room Database에 저장되는 설정 데이터입니다.
 */
@Entity(tableName = "hydration_settings")
data class HydrationSettingsEntity(
    @PrimaryKey
    val id: Long = 1,                   // 항상 1개만 존재
    val dailyGoal: Int,                 // 하루 목표량 (ml)
    val intervalHours: Float,           // 시간 간격 (시간)
    val wakingHours: Int,               // 깨어있는 시간 (시간)
    val startTimeHour: Int              // 하루 시작 시간 (0-23시)
) {
    /**
     * Entity를 Domain 모델로 변환
     * @return HydrationSettings 도메인 모델
     */
    fun toDomain(): HydrationSettings {
        return HydrationSettings(
            id = id,
            dailyGoal = dailyGoal,
            intervalHours = intervalHours,
            wakingHours = wakingHours,
            startTimeHour = startTimeHour
        )
    }

    companion object {
        /**
         * Domain 모델로부터 Entity 생성
         * @param settings 도메인 모델
         * @return Entity
         */
        fun fromDomain(settings: HydrationSettings): HydrationSettingsEntity {
            return HydrationSettingsEntity(
                id = settings.id,
                dailyGoal = settings.dailyGoal,
                intervalHours = settings.intervalHours,
                wakingHours = settings.wakingHours,
                startTimeHour = settings.startTimeHour
            )
        }

        /**
         * 기본 설정 Entity
         * @return 기본값으로 초기화된 Entity
         */
        fun default(): HydrationSettingsEntity {
            return fromDomain(HydrationSettings.default())
        }
    }
}