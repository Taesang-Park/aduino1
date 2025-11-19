package com.example.aduino1.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.aduino1.data.local.entity.WaterRecord

/**
 * Water Monitor Database
 * Room 데이터베이스 클래스
 */
@Database(
    entities = [WaterRecord::class],
    version = 1,
    exportSchema = false
)
abstract class WaterDatabase : RoomDatabase() {

    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile
        private var INSTANCE: WaterDatabase? = null

        /**
         * 데이터베이스 인스턴스 가져오기 (싱글톤)
         */
        fun getDatabase(context: Context): WaterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WaterDatabase::class.java,
                    "water_database"
                )
                    .fallbackToDestructiveMigration() // 마이그레이션 실패 시 데이터베이스 재생성
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 테스트용 인메모리 데이터베이스 생성
         */
        fun getInMemoryDatabase(context: Context): WaterDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                WaterDatabase::class.java
            ).build()
        }
    }
}