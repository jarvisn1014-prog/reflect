package com.nish.reflect.engine

import com.nish.reflect.data.model.JournalEntry
import com.nish.reflect.data.model.MoodLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MoodAggregatorTest {

    private fun makeEntry(content: String, valence: Float, energy: Float, daysAgo: Int): JournalEntry {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo)
        return JournalEntry(
            content = content,
            wordCount = content.split(" ").size,
            localValence = valence,
            localEnergy = energy,
            createdAt = cal.timeInMillis
        )
    }

    @Test
    fun `aggregates weekly data correctly`() {
        val entries = listOf(
            makeEntry("Great day", 0.8f, 0.7f, 1),
            makeEntry("Feeling stressed", -0.5f, 0.6f, 2),
            makeEntry("Calm morning", 0.3f, 0.3f, 3),
            makeEntry("Overwhelmed at work", -0.6f, 0.7f, 4),
            makeEntry("Good walk", 0.5f, 0.6f, 5)
        )
        val (start, end) = MoodAggregator.weekBounds(System.currentTimeMillis())
        val stats = MoodAggregator.aggregate(entries, emptyList(), start, end)

        assertEquals(5, stats.entryCount)
        assertTrue("Avg valence should be in range", stats.avgValence in -1f..1f)
    }

    @Test
    fun `empty week returns zero-entry summary`() {
        val (start, end) = MoodAggregator.weekBounds(System.currentTimeMillis())
        val stats = MoodAggregator.aggregate(emptyList(), emptyList(), start, end)

        assertEquals(0, stats.entryCount)
    }

    @Test
    fun `declining valence trend detected`() {
        val entries = listOf(
            makeEntry("Great day", 0.8f, 0.7f, 1),
            makeEntry("Good day", 0.6f, 0.6f, 2),
            makeEntry("Okay day", 0.2f, 0.4f, 3),
            makeEntry("Low day", -0.3f, 0.3f, 4),
            makeEntry("Bad day", -0.7f, 0.2f, 5)
        )
        val (start, end) = MoodAggregator.weekBounds(System.currentTimeMillis())
        val stats = MoodAggregator.aggregate(entries, emptyList(), start, end)

        // First half should be higher than second half
        assertEquals("declining", stats.moodTrend)
    }

    @Test
    fun `theme extraction counts recurring words`() {
        val entries = listOf(
            JournalEntry(content = "Stressed about work deadline", wordCount = 5, localValence = -0.5f, localEnergy = 0.7f, aiThemes = "work stress,deadline"),
            JournalEntry(content = "Work was overwhelming", wordCount = 3, localValence = -0.6f, localEnergy = 0.7f, aiThemes = "work stress"),
            JournalEntry(content = "Finally relaxing after work", wordCount = 5, localValence = 0.3f, localEnergy = 0.3f, aiThemes = "work,relaxing")
        )
        val (start, end) = MoodAggregator.weekBounds(System.currentTimeMillis())
        val stats = MoodAggregator.aggregate(entries, emptyList(), start, end)

        assertTrue("Work should be a top theme", stats.topThemes.any { it.first == "work" })
    }
}