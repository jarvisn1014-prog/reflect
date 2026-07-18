package com.nish.reflect.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

// PM Insight: One combined Gemini call on save returns:
// emotion, valence, arousal, confidence, themes, AND a reflection prompt.
// This halves API usage vs separate "Deep Analyze" + "Reflect" buttons.
// Zero-shot for classification (temp 0.2), few-shot examples for the reflection prompt.

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig
)

data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.3,
    val maxOutputTokens: Int = 2048,
    val responseMimeType: String = "application/json"
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

// Combined extraction + reflection response
data class ExtractionResult(
    @SerializedName("emotion") val emotion: String,
    @SerializedName("valence") val valence: Float,
    @SerializedName("arousal") val arousal: Float,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("themes") val themes: List<String>,
    @SerializedName("reflectionPrompt") val reflectionPrompt: String
)

interface GeminiApi {
    @POST
    suspend fun generateContent(
        @Url url: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object ReflectPrompt {
    // Combined: structured extraction + few-shot reflection prompt
    private val SYSTEM_INSTRUCTION = """
You are a reflective journaling assistant. Analyze the journal entry and return ONLY a JSON object with these fields:
- "emotion": primary emotion (one of: joyful, calm, content, neutral, anxious, sad, angry, frustrated, excited, grateful, lonely, overwhelmed, hopeful, tired)
- "valence": float from -1.0 (very negative) to 1.0 (very positive)
- "arousal": float from 0.0 (calm) to 1.0 (intense)
- "confidence": float from 0.0 to 1.0 (how confident you are in this classification)
- "themes": array of 1-3 short theme tags (1-3 words each, e.g. "work stress", "sleep", "family")
- "reflectionPrompt": ONE thoughtful follow-up question that helps the user think deeper about their entry. Specific to the content, not generic. Warm, curious, non-judgmental. 1-2 sentences max.

Example 1:
Entry: "Felt overwhelmed at work today. Couldn't focus on anything."
Response: {"emotion":"overwhelmed","valence":-0.5,"arousal":0.7,"confidence":0.85,"themes":["work stress","focus"],"reflectionPrompt":"What specifically felt overwhelming? Was it the volume of work, or the type of tasks?"}

Example 2:
Entry: "Had a great walk outside. Felt clear-headed for the first time in weeks."
Response: {"emotion":"calm","valence":0.6,"arousal":0.3,"confidence":0.8,"themes":["exercise","clarity"],"reflectionPrompt":"What about the walk made it different? Can you recreate those conditions?"}

Now analyze this entry:
""".trimIndent()

    fun buildExtractionPrompt(entryText: String): String {
        return "$SYSTEM_INSTRUCTION\n$entryText"
    }

    // Weekly digest prompt — takes aggregated stats JSON
    private val DIGEST_INSTRUCTION = """
You are a mood pattern analyst. You receive a week's worth of structured mood and journal data as JSON.
Identify patterns, correlations, and trends. Write a 2-4 sentence insight that is:
- Specific (cite days or themes when possible)
- Non-judgmental (observe, don't prescribe)
- Grounded in the data provided
- Warm and personal (second person, like a letter from the user's past self)
Do NOT diagnose or give medical advice. Do NOT use generic affirmations.
If the data is too sparse for a meaningful insight, respond with: "Not enough entries this week to reflect on. Come back when you've written a few more."
""".trimIndent()

    fun buildDigestPrompt(statsJson: String): String {
        return "$DIGEST_INSTRUCTION\n\nWeekly data:\n$statsJson"
    }

    const val MODEL = "gemini-2.0-flash"
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    fun buildEndpointUrl(model: String = MODEL): String {
        return "$BASE_URL$model:generateContent"
    }
}