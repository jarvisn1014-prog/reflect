package com.nish.reflect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nish.reflect.data.local.ReflectDatabase
import com.nish.reflect.data.model.JournalEntry
import com.nish.reflect.data.model.MoodLog
import com.nish.reflect.data.remote.ReflectAIService
import com.nish.reflect.engine.LocalSentimentAnalyzer
import com.nish.reflect.engine.MoodAggregator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReflectViewModel(app: Application) : AndroidViewModel(app) {

    private val db = ReflectDatabase.getInstance(app)
    private val entryDao = db.journalEntryDao()
    private val moodDao = db.moodLogDao()
    private val aiService = ReflectAIService()

    val entries: StateFlow<List<JournalEntry>> = entryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moodLogs: StateFlow<List<MoodLog>> = moodDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiKey: StateFlow<String> = SettingsStore.getApiKey(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val aiEnabled: StateFlow<Boolean> = SettingsStore.getAiEnabled(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hasSeenOnboarding: StateFlow<Boolean> = SettingsStore.getHasSeenOnboarding(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isGeneratingDigest = MutableStateFlow(false)
    val isGeneratingDigest: StateFlow<Boolean> = _isGeneratingDigest.asStateFlow()

    private val _digestHeadline = MutableStateFlow<String?>(null)
    val digestHeadline: StateFlow<String?> = _digestHeadline.asStateFlow()

    private val _weeklyStats = MutableStateFlow<MoodAggregator.WeeklyStats?>(null)
    val weeklyStats: StateFlow<MoodAggregator.WeeklyStats?> = _weeklyStats.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun saveEntry(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val wordCount = content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val localSentiment = LocalSentimentAnalyzer.analyze(content)

            val entry = JournalEntry(
                content = content,
                wordCount = wordCount,
                localValence = localSentiment.valence,
                localEnergy = localSentiment.energy
            )
            val entryId = entryDao.insert(entry)

            // If AI enabled and API key set, call Gemini for analysis
            if (aiEnabled.value && apiKey.value.isNotBlank()) {
                _isAnalyzing.value = true
                try {
                    val result = aiService.analyzeEntry(apiKey.value, content)
                    if (result != null) {
                        entryDao.updateAiFields(
                            id = entryId,
                            emotion = result.emotion,
                            valence = result.valence,
                            arousal = result.arousal,
                            confidence = result.confidence,
                            themes = result.themes.joinToString(","),
                            prompt = result.reflectionPrompt,
                            dataInput = result.dataInputJson
                        )
                    }
                } catch (e: Exception) {
                    _error.value = "AI analysis failed: ${e.message}"
                } finally {
                    _isAnalyzing.value = false
                }
            }
        }
    }

    fun logMood(mood: String, valence: Float, energy: Float, tags: String? = null, entryId: Long? = null) {
        viewModelScope.launch {
            moodDao.insert(MoodLog(
                mood = mood,
                valence = valence,
                energy = energy,
                tags = tags,
                entryId = entryId
            ))
        }
    }

    fun correctEmotion(entryId: Long, emotion: String) {
        viewModelScope.launch {
            entryDao.correctEmotion(entryId, emotion)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { entryDao.delete(id) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            entryDao.deleteAll()
            moodDao.deleteAll()
        }
    }

    fun generateWeeklyDigest() {
        if (apiKey.value.isBlank()) {
            _error.value = "Please set your Gemini API key in Settings first."
            return
        }
        viewModelScope.launch {
            _isGeneratingDigest.value = true
            _error.value = null
            try {
                val (weekStart, weekEnd) = MoodAggregator.weekBounds(System.currentTimeMillis())
                val weekEntries = entryDao.getByDateRange(weekStart, weekEnd)
                val weekMoods = moodDao.getByDateRange(weekStart, weekEnd)

                val stats = MoodAggregator.aggregate(weekEntries, weekMoods, weekStart, weekEnd)
                _weeklyStats.value = stats

                if (stats.entryCount >= 3) {
                    val digest = aiService.generateDigest(apiKey.value, stats.weeklyStatsJson)
                    _digestHeadline.value = digest?.headline
                        ?: "Not enough entries this week to reflect on."
                } else {
                    _digestHeadline.value = null
                }
            } catch (e: Exception) {
                _error.value = "Failed to generate digest: ${e.message}"
            } finally {
                _isGeneratingDigest.value = false
            }
        }
    }

    fun exportJournal(): String {
        val sb = StringBuilder()
        sb.appendLine("# Reflect — Journal Export")
        sb.appendLine("Exported: ${java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}")
        sb.appendLine()
        entries.value.forEach { entry ->
            sb.appendLine("---")
            sb.appendLine("Date: ${java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date(entry.createdAt))}")
            sb.appendLine("Words: ${entry.wordCount}")
            if (entry.aiEmotion != null) sb.appendLine("AI Emotion: ${entry.userCorrectedEmotion ?: entry.aiEmotion}")
            if (entry.aiThemes != null) sb.appendLine("Themes: ${entry.aiThemes}")
            if (entry.aiReflectionPrompt != null) sb.appendLine("Reflection: ${entry.aiReflectionPrompt}")
            sb.appendLine()
            sb.appendLine(entry.content)
            sb.appendLine()
        }
        moodLogs.value.forEach { log ->
            sb.appendLine("---")
            sb.appendLine("Mood: ${log.mood} (${java.text.SimpleDateFormat("MMM d, 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date(log.createdAt))})")
            if (log.tags != null) sb.appendLine("Tags: ${log.tags}")
            sb.appendLine()
        }
        return sb.toString()
    }

    fun setApiKey(key: String) {
        viewModelScope.launch { SettingsStore.setApiKey(getApplication(), key) }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch { SettingsStore.setAiEnabled(getApplication(), enabled) }
    }

    fun setHasSeenOnboarding() {
        viewModelScope.launch { SettingsStore.setHasSeenOnboarding(getApplication(), true) }
    }

    fun clearError() { _error.value = null }
}