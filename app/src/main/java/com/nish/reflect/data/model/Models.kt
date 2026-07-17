package com.nish.reflect.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// PM Insight: JournalEntry carries BOTH local (instant, zero cost) and AI (Gemini) sentiment.
// This makes the hybrid AI pattern visible in the data model.
// AI fields are nullable — set after Gemini call on save.
// userCorrectedEmotion allows the user to override AI classification.

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val wordCount: Int,
    // On-device sentiment (instant, zero API cost)
    val localValence: Float,        // -1.0 (very negative) to 1.0 (very positive)
    val localEnergy: Float,          // 0.0 (depleted) to 1.0 (energized)
    // AI analysis (set after Gemini call on save, null if AI off or not yet analyzed)
    val aiEmotion: String? = null,
    val aiValence: Float? = null,
    val aiArousal: Float? = null,
    val aiConfidence: Float? = null,
    val aiThemes: String? = null,    // CSV: "work stress,sleep,family"
    val aiReflectionPrompt: String? = null,
    val aiDataInput: String? = null, // JSON snapshot for auditability
    val userCorrectedEmotion: String? = null, // if user overrides AI
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mood_logs")
data class MoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mood: String,               // "rough", "low", "okay", "good", "great"
    val valence: Float,             // -1.0, -0.5, 0.0, 0.5, 1.0
    val energy: Float,              // 0.0 to 1.0
    val tags: String? = null,       // CSV: "anxious,calm,grateful"
    val entryId: Long? = null,      // link to JournalEntry if from editor
    val createdAt: Long = System.currentTimeMillis()
)