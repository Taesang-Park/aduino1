package com.example.aduino1.data.local.database

import androidx.room.*
import com.example.aduino1.data.local.entity.WaterRecord
import kotlinx.coroutines.flow.Flow

/**
 * Water Record DAO (Data Access Object)
 * 데이터베이스 접근을 위한 인터페이스
 */
@Dao
interface WaterDao {

    /**
     * 새로운 물 섭취 기록 추가
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WaterRecord): Long

    /**
     * 여러 개의 기록 추가
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<WaterRecord>)

    /**
     * 기록 업데이트
     */
    @Update
    suspend fun update(record: WaterRecord)

    /**
     * 기록 삭제
     */
    @Delete
    suspend fun delete(record: WaterRecord)

    /**
     * ID로 기록 삭제
     */
    @Query("DELETE FROM water_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 특정 날짜의 기록 삭제
     */
    @Query("DELETE FROM water_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    /**
     * 모든 기록 삭제
     */
    @Query("DELETE FROM water_records")
    suspend fun deleteAll()

    /**
     * ID로 기록 조회
     */
    @Query("SELECT * FROM water_records WHERE id = :id")
    suspend fun getById(id: Long): WaterRecord?

    /**
     * 특정 날짜의 모든 기록 조회 (Flow)
     */
    @Query("SELECT * FROM water_records WHERE date = :date ORDER BY timestamp DESC")
    fun getRecordsByDate(date: String): Flow<List<WaterRecord>>

    /**
     * 특정 날짜의 모든 기록 조회 (일회성)
     */
    @Query("SELECT * FROM water_records WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getRecordsByDateOnce(date: String): List<WaterRecord>

    /**
     * 특정 날짜의 총 섭취량 조회
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM water_records WHERE date = :date")
    suspend fun getTotalAmountByDate(date: String): Int

    /**
     * 특정 날짜의 총 섭취량 조회 (Flow)
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM water_records WHERE date = :date")
    fun getTotalAmountByDateFlow(date: String): Flow<Int>

    /**
     * 특정 날짜 범위의 기록 조회
     */
    @Query("SELECT * FROM water_records WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getRecordsByDateRange(startDate: String, endDate: String): Flow<List<WaterRecord>>

    /**
     * 특정 날짜 범위의 일별 총량 조회
     */
    @Query("SELECT date, COALESCE(SUM(amount), 0) as total FROM water_records WHERE date BETWEEN :startDate AND :endDate GROUP BY date ORDER BY date ASC")
    suspend fun getDailyTotalsByDateRange(startDate: String, endDate: String): List<DailyTotal>

    /**
     * 모든 기록 조회
     */
    @Query("SELECT * FROM water_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<WaterRecord>>

    /**
     * 전체 기록 개수
     */
    @Query("SELECT COUNT(*) FROM water_records")
    suspend fun getRecordCount(): Int

    /**
     * 특정 날짜의 기록 개수
     */
    @Query("SELECT COUNT(*) FROM water_records WHERE date = :date")
    suspend fun getRecordCountByDate(date: String): Int

    /**
     * 최근 N개의 기록 조회
     */
    @Query("SELECT * FROM water_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<WaterRecord>>

    /**
     * 전체 평균 섭취량
     */
    @Query("SELECT COALESCE(AVG(daily_total), 0) FROM (SELECT date, SUM(amount) as daily_total FROM water_records GROUP BY date)")
    suspend fun getAverageDailyIntake(): Float

    /**
     * 특정 시간 범위 내의 기록 조회 (타임스탬프 기준)
     */
    @Query("SELECT * FROM water_records WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp ORDER BY timestamp DESC")
    suspend fun getRecordsByTimestampRange(startTimestamp: Long, endTimestamp: Long): List<WaterRecord>

    /**
     * 특정 날짜의 시간별 섭취량 집계
     * @return 시간(0-23)과 해당 시간의 총 섭취량 맵
     */
    @Query("""
        SELECT
            CAST(strftime('%H', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER) as hour,
            COALESCE(SUM(amount), 0) as total
        FROM water_records
        WHERE date = :date
        GROUP BY hour
        ORDER BY hour ASC
    """)
    suspend fun getHourlyIntakeByDate(date: String): List<HourlyIntake>
}

/**
 * 일별 총량 데이터 클래스
 */
data class DailyTotal(
    val date: String,
    val total: Int
)

/**
 * 시간별 섭취량 데이터 클래스
 */
data class HourlyIntake(
    val hour: Int,      // 0-23
    val total: Int      // ml
)