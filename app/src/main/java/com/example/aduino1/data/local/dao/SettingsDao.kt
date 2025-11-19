package com.example.aduino1.data.local.dao

import androidx.room.*
import com.example.aduino1.data.local.entity.HydrationSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * 설정 DAO (Data Access Object)
 *
 * 수분 섭취 설정에 대한 데이터베이스 쿼리를 정의합니다.
 */
@Dao
interface SettingsDao {

    /**
     * 설정 조회 (Flow)
     * @return 설정 Flow (항상 1개)
     */
    @Query("SELECT * FROM hydration_settings WHERE id = 1")
    fun getSettings(): Flow<HydrationSettingsEntity?>

    /**
     * 설정 조회 (일회성)
     * @return 설정, 없으면 null
     */
    @Query("SELECT * FROM hydration_settings WHERE id = 1")
    suspend fun getSettingsOnce(): HydrationSettingsEntity?

    /**
     * 설정 저장 (Insert or Update)
     * @param settings 저장할 설정
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: HydrationSettingsEntity)

    /**
     * 설정 업데이트
     * @param settings 업데이트할 설정
     */
    @Update
    suspend fun update(settings: HydrationSettingsEntity)

    /**
     * 설정 삭제 (테스트용)
     * @param settings 삭제할 설정
     */
    @Delete
    suspend fun delete(settings: HydrationSettingsEntity)

    /**
     * 모든 설정 삭제 (리셋용)
     */
    @Query("DELETE FROM hydration_settings")
    suspend fun deleteAll()

    /**
     * 하루 목표량만 업데이트
     * @param dailyGoal 새 목표량
     */
    @Query("UPDATE hydration_settings SET dailyGoal = :dailyGoal WHERE id = 1")
    suspend fun updateDailyGoal(dailyGoal: Int)

    /**
     * 시간 간격만 업데이트
     * @param intervalHours 새 시간 간격
     */
    @Query("UPDATE hydration_settings SET intervalHours = :intervalHours WHERE id = 1")
    suspend fun updateIntervalHours(intervalHours: Float)
}