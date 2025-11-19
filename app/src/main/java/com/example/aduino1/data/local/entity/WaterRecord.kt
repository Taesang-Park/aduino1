package com.example.aduino1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

/**
 * 물 섭취 기록 Entity
 * Room Database에 저장되는 데이터 모델
 */
@Entity(tableName = "water_records")
data class WaterRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * 마신 양 (ml)
     */
    val amount: Int,

    /**
     * 타임스탬프 (밀리초)
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * 날짜 (yyyy-MM-dd 형식)
     */
    val date: String = getCurrentDate()
) {
    companion object {
        private fun getCurrentDate(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return dateFormat.format(Date())
        }

        /**
         * 타임스탬프를 시간 문자열로 변환 (HH:mm)
         */
        fun formatTime(timestamp: Long): String {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            return timeFormat.format(Date(timestamp))
        }

        /**
         * 타임스탬프를 전체 날짜시간 문자열로 변환
         */
        fun formatDateTime(timestamp: Long): String {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return dateTimeFormat.format(Date(timestamp))
        }
    }
}