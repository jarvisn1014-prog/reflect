package com.nish.reflect.engine

import com.nish.reflect.data.model.JournalEntry
import com.nish.reflect.data.model.MoodLog
import java.util.Calendar

// PM Insight: This is "temporal pattern aggregation" — compressing structured data
// into stats that an LLM can reason about. Instead of sending raw entries to Gemini,
// we aggregate into: avgValence, emotionDistribution, topThemes, moodTrend.
// This is the data-to-text pattern: structured data in, narrative insight out.

data class WeeklyStats(
    val entryCount: Int,
    val avgValence: Float,
    val avgEnergy: Float,
    val emotionDistribution: Map<String, Int>,
    val topThemes: List<Pair<String, Int>>,
    val moodTrend: String,  // "improving", "declining", "stable"
    val dayLabels: List<String>,
    val dayValences: List<Float?>,  // per-day valence, null if no entry
    val weeklyStatsJson: String
)

object MoodAggregator {

    /// Get the start (Monday 00:00) and end (Sunday 23:59:59) timestamps for the week containing `date`
    fun weekBounds(date: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Move to Monday (first day of week)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysToMonday)
        val start = cal.timeInMillis

        // End = Sunday 23:59:59
        cal.add(Calendar.DAY_OF_MONTH, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return start to end
    }

    fun aggregate(
        entries: List<JournalEntry>,
        moodLogs: List<MoodLog>,
        weekStart: Long,
        weekEnd: Long
    ): WeeklyStats {
        val weekEntries = entries.filter { it.createdAt in weekStart..weekEnd }
        val weekMoods = moodLogs.filter { it.createdAt in weekStart..weekEnd }

        if (weekEntries.isEmpty() && weekMoods.isEmpty()) {
            return WeeklyStats(
                entryCount = 0,
                avgValence = 0f,
                avgEnergy = 0f,
                emotionDistribution = emptyMap(),
                topThemes = emptyList(),
                moodTrend = "stable",
                dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                dayValences = List(7) { null },
                weeklyStatsJson = """{"entryCount":0}"""
            )
        }

        // Average valence from entries (use AI valence if available, else local)
        val valences = weekEntries.map { it.aiValence ?: it.localValence }
        val avgValence = if (valences.isNotEmpty()) valences.average().toFloat() else 0f

        // Average energy
        val energies = weekEntries.map { it.localEnergy }
        val avgEnergy = if (energies.isNotEmpty()) energies.average().toFloat() else 0f

        // Emotion distribution from AI fields
        val emotionDist = weekEntries
            .mapNotNull { it.aiEmotion }
            .groupingBy { it }
            .eachCount()
            .mapValues { it.value }

        // Theme extraction from AI themes (CSV)
        val themeCount = mutableMapOf<String, Int>()
        weekEntries.forEach { entry ->
            entry.aiThemes?.split(",")?.forEach { theme ->
                val cleaned = theme.trim().lowercase()
                if (cleaned.isNotBlank()) {
                    themeCount[cleaned] = (themeCount[cleaned] ?: 0) + 1
                }
            }
        }
        val topThemes = themeCount.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        // Per-day valence for chart
        val dayValences = List(7) { dayIndex ->
            val dayStart = weekStart + dayIndex * 24 * 60 * 60 * 1000L
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L - 1
            val dayEntries = weekEntries.filter { it.createdAt in dayStart..dayEnd }
            val dayMoods = weekMoods.filter { it.createdAt in dayStart..dayEnd }

            val allValences = dayEntries.map { it.aiValence ?: it.localValence } + dayMoods.map { it.valence }
            if (allValences.isNotEmpty()) allValences.average().toFloat() else null
        }

        // Mood trend: compare first half vs second half
        val firstHalf = dayValences.take(4).filterNotNull()
        val secondHalf = dayValences.drop(4).filterNotNull()
        val trend = if (firstHalf.isNotEmpty() && secondHalf.isNotEmpty()) {
            val diff = secondHalf.average() - firstHalf.average()
            when {
                diff > 0.15f -> "improving"
                diff < -0.15f -> "declining"
                else -> "stable"
            }
        } else "stable"

        // Build JSON for AI insight auditability
        val json = buildString {
            append("{")
            append("\"entryCount\":${weekEntries.size},")
            append("\"avgValence\":${"%.2f".format(avgValence)},")
            append("\"avgEnergy\":${"%.2f".format(avgEnergy)},")
            append("\"moodTrend\":\"$trend\",")
            append("\"emotionDistribution\":${emotionDist.map { "\"${it.key}\":${it.value}" }.joinToString(",", "{", "}") },")
            append("\"topThemes\":[${topThemes.joinToString(",") { "\"${it.first}\"" }}]")
            append("}")
        }

        return WeeklyStats(
            entryCount = weekEntries.size,
            avgValence = avgValence,
            avgEnergy = avgEnergy,
            emotionDistribution = emotionDist,
            topThemes = topThemes,
            moodTrend = trend,
            dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            dayValences = dayValences,
            weeklyStatsJson = json
        )
    }
}