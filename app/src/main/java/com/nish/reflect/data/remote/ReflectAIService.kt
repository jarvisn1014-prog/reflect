package com.nish.reflect.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// PM Insight: Combined AI service — one call for extraction + reflection,
// one call for weekly digest. Minimal API usage, maximum value per call.

class ReflectAIService {

    private val api: GeminiApi = Retrofit.Builder()
        .baseUrl(ReflectPrompt.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class)

    private val gson = Gson()

    data class ExtractionOutput(
        val emotion: String,
        val valence: Float,
        val arousal: Float,
        val confidence: Float,
        val themes: List<String>,
        val reflectionPrompt: String,
        val dataInputJson: String
    )

    data class DigestOutput(
        val headline: String,
        val dataInputJson: String
    )

    suspend fun analyzeEntry(apiKey: String, entryText: String): ExtractionOutput? =
        withContext(Dispatchers.IO) {
            try {
                val prompt = ReflectPrompt.buildExtractionPrompt(entryText)
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(prompt))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(temperature = 0.3)
                )

                val response = api.generateContent(
                    ReflectPrompt.buildEndpointUrl(),
                    apiKey,
                    request
                )

                val rawText = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?: return@withContext null

                // Defensive JSON extraction — strip markdown fences if present
                val cleanJson = extractJson(rawText) ?: return@withContext null

                val result = gson.fromJson(cleanJson, ExtractionResult::class.java)

                ExtractionOutput(
                    emotion = result.emotion,
                    valence = result.valence,
                    arousal = result.arousal,
                    confidence = result.confidence,
                    themes = result.themes,
                    reflectionPrompt = result.reflectionPrompt,
                    dataInputJson = cleanJson
                )
            } catch (e: Exception) {
                null
            }
        }

    suspend fun generateDigest(apiKey: String, statsJson: String): DigestOutput? =
        withContext(Dispatchers.IO) {
            try {
                val prompt = ReflectPrompt.buildDigestPrompt(statsJson)
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(prompt))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.4,
                        responseMimeType = "text/plain"
                    )
                )

                val response = api.generateContent(
                    ReflectPrompt.buildEndpointUrl(),
                    apiKey,
                    request
                )

                val headline = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?: return@withContext null

                DigestOutput(headline = headline.trim(), dataInputJson = statsJson)
            } catch (e: Exception) {
                null
            }
        }

    /// Strip markdown code fences and extract JSON object
    private fun extractJson(text: String): String? {
        var cleaned = text.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            cleaned.substring(start, end + 1)
        } else null
    }
}