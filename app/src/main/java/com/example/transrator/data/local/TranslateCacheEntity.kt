package com.example.transrator.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "translation_cache",
    indices = [Index(value = ["profileId", "sourceText"], unique = true)]
)
data class TranslationCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Int,
    val sourceText: String,
    val translatedText: String,
    val cachedAt: Long = System.currentTimeMillis()
)