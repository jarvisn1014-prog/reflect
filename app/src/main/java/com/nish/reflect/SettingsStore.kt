package com.nish.reflect

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// PM Insight: DataStore for settings — API key, AI toggle, onboarding flag.
// The AI toggle is a trust feature: users can disable AI and use Reflect as a pure journal.

private val Context.dataStore by preferencesDataStore(name = "reflect_settings")

object SettingsStore {
    private val API_KEY = stringPreferencesKey("api_key")
    private val AI_ENABLED = booleanPreferencesKey("ai_enabled")
    private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    private val THEME_MODE = stringPreferencesKey("theme_mode")

    fun getApiKey(context: Context): Flow<String> =
        context.dataStore.data.map { it[API_KEY] ?: "" }

    suspend fun setApiKey(context: Context, key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    fun getAiEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[AI_ENABLED] ?: true }

    suspend fun setAiEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[AI_ENABLED] = enabled }
    }

    fun getHasSeenOnboarding(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[HAS_SEEN_ONBOARDING] ?: false }

    suspend fun setHasSeenOnboarding(context: Context, seen: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_ONBOARDING] = seen }
    }

    fun getThemeMode(context: Context): Flow<String> =
        context.dataStore.data.map { it[THEME_MODE] ?: "system" }

    suspend fun setThemeMode(context: Context, mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }
}