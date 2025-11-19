package com.example.aduino1.data.repository

import com.example.aduino1.data.local.dao.SettingsDao
import com.example.aduino1.data.local.entity.HydrationSettingsEntity
import com.example.aduino1.domain.model.HydrationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 설정 Repository
 *
 * 수분 섭취 설정에 대한 데이터 접근을 추상화합니다.
 */
class SettingsRepository(private val settingsDao: SettingsDao) {

    /**
     * 설정 조회 (Flow)
     * @return 설정 Flow
     */
    fun getSettings(): Flow<HydrationSettings> {
        return settingsDao.getSettings().map { entity ->
            entity?.toDomain() ?: HydrationSettings.default()
        }
    }

    /**
     * 설정 조회 (일회성)
     * @return 설정
     */
    suspend fun getSettingsOnce(): HydrationSettings {
        val entity = settingsDao.getSettingsOnce()
        return entity?.toDomain() ?: HydrationSettings.default()
    }

    /**
     * 설정 저장
     * @param settings 저장할 설정
     */
    suspend fun saveSettings(settings: HydrationSettings) {
        val entity = HydrationSettingsEntity.fromDomain(settings)
        settingsDao.insertOrUpdate(entity)
    }

    /**
     * 하루 목표량만 업데이트
     * @param dailyGoal 새 목표량 (ml)
     */
    suspend fun updateDailyGoal(dailyGoal: Int) {
        settingsDao.updateDailyGoal(dailyGoal)
    }

    /**
     * 시간 간격만 업데이트
     * @param intervalHours 새 시간 간격 (시간)
     */
    suspend fun updateIntervalHours(intervalHours: Float) {
        settingsDao.updateIntervalHours(intervalHours)
    }

    /**
     * 설정 초기화 (기본값으로)
     */
    suspend fun resetToDefault() {
        saveSettings(HydrationSettings.default())
    }

    /**
     * 설정이 존재하는지 확인
     * @return 존재하면 true
     */
    suspend fun hasSettings(): Boolean {
        return settingsDao.getSettingsOnce() != null
    }

    /**
     * 초기 설정 생성 (앱 최초 실행 시)
     */
    suspend fun initializeIfNeeded() {
        if (!hasSettings()) {
            saveSettings(HydrationSettings.default())
        }
    }
}
