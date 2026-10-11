package com.example.transrator

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TranslationCacheEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun translationCacheDao(): TranslationCacheDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transrator.db"   // 실제 생성되는 DB 파일 이름
                ).build().also { INSTANCE = it }
            }
        }
    }
}