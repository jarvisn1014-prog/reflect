package com.nish.reflect.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LocalSentimentAnalyzerTest {

    @Test
    fun `positive words produce positive valence`() {
        val score = LocalSentimentAnalyzer.analyze("I had a great wonderful day feeling happy")
        assertTrue("Valence should be positive", score.valence > 0.3f)
    }

    @Test
    fun `negative words produce negative valence`() {
        val score = LocalSentimentAnalyzer.analyze("I feel terrible exhausted and sad")
        assertTrue("Valence should be negative", score.valence < -0.3f)
    }

    @Test
    fun `neutral text produces near-zero valence`() {
        val score = LocalSentimentAnalyzer.analyze("I went to the store")
        assertTrue("Valence should be near zero", abs(score.valence) < 0.1f)
    }

    @Test
    fun `mixed sentiment averages correctly`() {
        val score = LocalSentimentAnalyzer.analyze("Great day but feel exhausted")
        assertTrue("Mixed sentiment should be near zero", abs(score.valence) < 0.3f)
    }

    @Test
    fun `energy words map correctly`() {
        val score = LocalSentimentAnalyzer.analyze("I feel energized and motivated")
        assertTrue("Energy should be high", score.energy > 0.5f)
    }

    @Test
    fun `empty text returns neutral`() {
        val score = LocalSentimentAnalyzer.analyze("")
        assertEquals(0f, score.valence, 0.01f)
        assertEquals(0f, score.energy, 0.01f)
    }
}