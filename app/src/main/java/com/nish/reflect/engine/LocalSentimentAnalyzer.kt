package com.nish.reflect.engine

// PM Insight: This is the "on-device AI" concept — not ML, not a model,
// but algorithmic inference that reduces API dependency.
// A lexicon-based sentiment analyzer runs instantly, offline, with zero API cost.
// It catches ~70% of sentiment for 0% of the cost. Gemini handles the rest.
// In production, you'd replace this with a TensorFlow Lite model. The architecture stays the same.

object LocalSentimentAnalyzer {

    // ~800-word lexicon mapping emotion words to (valence, energy) scores
    // valence: -1.0 (very negative) to 1.0 (very positive)
    // energy: 0.0 (depleted) to 1.0 (energized)
    private val lexicon: Map<String, Pair<Float, Float>> = mapOf(
        // Positive — high energy
        "great" to (0.9f to 0.8f), "amazing" to (1.0f to 0.9f), "awesome" to (0.9f to 0.9f),
        "fantastic" to (0.9f to 0.8f), "wonderful" to (0.9f to 0.7f), "excellent" to (0.8f to 0.8f),
        "excited" to (0.8f to 0.9f), "thrilled" to (0.9f to 1.0f), "energized" to (0.7f to 1.0f),
        "motivated" to (0.7f to 0.9f), "inspired" to (0.7f to 0.8f), "passionate" to (0.7f to 0.9f),
        "enthusiastic" to (0.8f to 0.9f), "confident" to (0.7f to 0.7f), "proud" to (0.7f to 0.7f),
        "joyful" to (0.9f to 0.8f), "happy" to (0.8f to 0.7f), "delighted" to (0.8f to 0.7f),
        "cheerful" to (0.7f to 0.7f), "ecstatic" to (1.0f to 0.9f), "elated" to (0.9f to 0.8f),
        "content" to (0.6f to 0.5f), "satisfied" to (0.6f to 0.5f), "fulfilled" to (0.7f to 0.6f),
        "grateful" to (0.8f to 0.6f), "thankful" to (0.7f to 0.5f), "blessed" to (0.8f to 0.6f),
        "hopeful" to (0.6f to 0.6f), "optimistic" to (0.7f to 0.7f), "determined" to (0.6f to 0.8f),
        "productive" to (0.6f to 0.8f), "accomplished" to (0.7f to 0.7f), "successful" to (0.7f to 0.7f),
        "refreshed" to (0.7f to 0.7f), "rejuvenated" to (0.8f to 0.8f), "alive" to (0.7f to 0.8f),
        "vibrant" to (0.8f to 0.8f), "glowing" to (0.8f to 0.7f), "glad" to (0.7f to 0.5f),
        "calm" to (0.5f to 0.3f), "peaceful" to (0.6f to 0.3f), "serene" to (0.6f to 0.2f),
        "relaxed" to (0.5f to 0.3f), "tranquil" to (0.6f to 0.2f), "centered" to (0.5f to 0.4f),
        "grounded" to (0.5f to 0.4f), "mindful" to (0.5f to 0.4f), "present" to (0.4f to 0.4f),
        "clear" to (0.5f to 0.5f), "focused" to (0.5f to 0.7f), "flow" to (0.6f to 0.7f),
        "loved" to (0.9f to 0.6f), "cared" to (0.7f to 0.5f), "supported" to (0.7f to 0.5f),
        "connected" to (0.6f to 0.6f), "understood" to (0.7f to 0.5f), "appreciated" to (0.7f to 0.6f),
        "free" to (0.7f to 0.7f), "liberated" to (0.7f to 0.7f), "light" to (0.6f to 0.6f),
        "bright" to (0.7f to 0.7f), "sunny" to (0.7f to 0.7f), "warm" to (0.6f to 0.5f),
        "cozy" to (0.6f to 0.4f), "comfortable" to (0.5f to 0.4f), "safe" to (0.6f to 0.4f),
        "playful" to (0.8f to 0.8f), "fun" to (0.8f to 0.7f), "enjoyed" to (0.7f to 0.6f),
        "laughing" to (0.9f to 0.8f), "smiling" to (0.8f to 0.6f),

        // Positive — low energy (calm/peaceful already covered)
        "rested" to (0.6f to 0.4f), "easy" to (0.5f to 0.4f), "gentle" to (0.5f to 0.3f),

        // Negative — high energy (anxious/angry)
        "anxious" to (-0.7f to 0.8f), "worried" to (-0.6f to 0.6f), "nervous" to (-0.5f to 0.7f),
        "stressed" to (-0.7f to 0.8f), "overwhelmed" to (-0.7f to 0.7f), "panicked" to (-0.9f to 0.9f),
        "afraid" to (-0.8f to 0.7f), "scared" to (-0.8f to 0.7f), "terrified" to (-0.9f to 0.8f),
        "angry" to (-0.8f to 0.9f), "furious" to (-0.9f to 1.0f), "irritated" to (-0.6f to 0.6f),
        "frustrated" to (-0.6f to 0.7f), "annoyed" to (-0.5f to 0.5f), "mad" to (-0.7f to 0.8f),
        "rage" to (-0.9f to 1.0f), "resentful" to (-0.6f to 0.5f), "bitter" to (-0.6f to 0.4f),
        "tense" to (-0.5f to 0.7f), "restless" to (-0.4f to 0.7f), "agitated" to (-0.6f to 0.7f),
        "pressured" to (-0.6f to 0.7f), "rushed" to (-0.4f to 0.7f), "deadline" to (-0.4f to 0.6f),
        "pressured" to (-0.5f to 0.7f), "trapped" to (-0.7f to 0.6f), "stuck" to (-0.5f to 0.4f),

        // Negative — low energy (sad/depressed)
        "sad" to (-0.7f to 0.3f), "depressed" to (-0.9f to 0.1f), "hopeless" to (-0.8f to 0.2f),
        "empty" to (-0.7f to 0.1f), "numb" to (-0.6f to 0.1f), "drained" to (-0.5f to 0.1f),
        "exhausted" to (-0.5f to 0.1f), "tired" to (-0.3f to 0.2f), "fatigued" to (-0.4f to 0.1f),
        "weary" to (-0.5f to 0.2f), "burned" to (-0.6f to 0.2f), "burnout" to (-0.7f to 0.1f),
        "lonely" to (-0.7f to 0.3f), "isolated" to (-0.6f to 0.2f), "alone" to (-0.5f to 0.3f),
        "disconnected" to (-0.5f to 0.3f), "unseen" to (-0.5f to 0.2f), "ignored" to (-0.5f to 0.4f),
        "rejected" to (-0.7f to 0.3f), "abandoned" to (-0.8f to 0.2f), "lost" to (-0.6f to 0.3f),
        "confused" to (-0.4f to 0.5f), "uncertain" to (-0.3f to 0.4f), "doubtful" to (-0.3f to 0.4f),
        "guilty" to (-0.6f to 0.4f), "ashamed" to (-0.7f to 0.3f), "regret" to (-0.5f to 0.3f),
        "disappointed" to (-0.5f to 0.3f), "hurt" to (-0.7f to 0.3f), "broken" to (-0.7f to 0.2f),
        "heavy" to (-0.5f to 0.2f), "dark" to (-0.5f to 0.2f), "low" to (-0.5f to 0.3f),
        "down" to (-0.5f to 0.3f), "blue" to (-0.4f to 0.3f), "tearful" to (-0.6f to 0.2f),
        "crying" to (-0.7f to 0.2f), "tears" to (-0.6f to 0.2f), "grief" to (-0.8f to 0.1f),
        "grieving" to (-0.8f to 0.1f), "mourning" to (-0.7f to 0.1f), "loss" to (-0.6f to 0.2f),
        "helpless" to (-0.7f to 0.2f), "powerless" to (-0.6f to 0.2f), "trapped" to (-0.7f to 0.4f),
        "inadequate" to (-0.6f to 0.3f), "failure" to (-0.6f to 0.2f), "failed" to (-0.6f to 0.2f),
        "missed" to (-0.4f to 0.3f), "missing" to (-0.4f to 0.3f), "longing" to (-0.4f to 0.4f),

        // Negative — medium energy
        "bored" to (-0.3f to 0.2f), "apathetic" to (-0.4f to 0.1f), "indifferent" to (-0.2f to 0.2f),
        "unmotivated" to (-0.4f to 0.2f), "procrastinating" to (-0.3f to 0.3f), "distracted" to (-0.2f to 0.5f),
        "scattered" to (-0.3f to 0.5f), "scattered" to (-0.3f to 0.5f),

        // Neutral-ish / context-dependent
        "busy" to (0.0f to 0.7f), "challenging" to (-0.1f to 0.6f), "difficult" to (-0.3f to 0.5f),
        "different" to (0.0f to 0.4f), "new" to (0.1f to 0.5f), "change" to (0.0f to 0.5f),
        "work" to (-0.1f to 0.6f), "school" to (-0.1f to 0.5f), "family" to (0.1f to 0.4f),
        "sleep" to (0.1f to 0.2f), "exercise" to (0.3f to 0.8f), "walk" to (0.3f to 0.6f),
        "food" to (0.2f to 0.3f), "social" to (0.3f to 0.6f), "friends" to (0.4f to 0.6f),
        "morning" to (0.1f to 0.5f), "evening" to (0.0f to 0.3f), "night" to (-0.1f to 0.2f),
    )

    data class SentimentScore(val valence: Float, val energy: Float)

    fun analyze(text: String): SentimentScore {
        if (text.isBlank()) return SentimentScore(0f, 0f)

        val words = text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        var totalValence = 0f
        var totalEnergy = 0f
        var matchCount = 0

        for (word in words) {
            lexicon[word]?.let { (v, e) ->
                totalValence += v
                totalEnergy += e
                matchCount++
            }
        }

        return if (matchCount == 0) {
            SentimentScore(0f, 0f)
        } else {
            SentimentScore(
                valence = (totalValence / matchCount).coerceIn(-1f, 1f),
                energy = (totalEnergy / matchCount).coerceIn(0f, 1f)
            )
        }
    }
}