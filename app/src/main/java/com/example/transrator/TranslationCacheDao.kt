package com.example.transrator

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TranslationCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TranslationCacheEntity)

    @Query("""
        SELECT * FROM translation_cache
        WHERE profileId = :profileId AND sourceText = :sourceText
        LIMIT 1
    """)
    suspend fun findBySourceText(profileId: Int, sourceText: String): TranslationCacheEntity?
}