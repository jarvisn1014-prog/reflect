package com.nish.reflect.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nish.reflect.data.model.JournalEntry
import com.nish.reflect.data.model.MoodLog
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry): Long

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    fun getAll(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt ASC")
    suspend fun getByDateRange(start: Long, end: Long): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: Long): JournalEntry?

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): JournalEntry?

    @Query("UPDATE journal_entries SET userCorrectedEmotion = :emotion WHERE id = :id")
    suspend fun correctEmotion(id: Long, emotion: String)

    @Query("UPDATE journal_entries SET aiEmotion = :emotion, aiValence = :valence, aiArousal = :arousal, aiConfidence = :confidence, aiThemes = :themes, aiReflectionPrompt = :prompt, aiDataInput = :dataInput WHERE id = :id")
    suspend fun updateAiFields(id: Long, emotion: String, valence: Float, arousal: Float, confidence: Float, themes: String, prompt: String, dataInput: String)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun count(): Int
}

@Dao
interface MoodLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MoodLog): Long

    @Query("SELECT * FROM mood_logs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MoodLog>>

    @Query("SELECT * FROM mood_logs WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt ASC")
    suspend fun getByDateRange(start: Long, end: Long): List<MoodLog>

    @Query("DELETE FROM mood_logs")
    suspend fun deleteAll()
}