package com.kafka.launcher.data.remote

import com.kafka.launcher.config.GeminiConfig
import com.kafka.launcher.domain.model.GeminiRecommendationJson
import com.kafka.launcher.domain.model.GeminiRecommendations
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GeminiApiClient(private val client: OkHttpClient = OkHttpClient()) {
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchRecommendations(payload: String, apiKey: String): GeminiRecommendations? {
        if (apiKey.isBlank()) return null
        val requestBody = buildBody(payload)
        val request = Request.Builder()
            .url("${GeminiConfig.endpoint}?key=$apiKey")
            .post(requestBody.toRequestBody(mediaType))
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val content = response.body?.string().orEmpty()
                parse(content)
            }
        }
    }

    private fun buildBody(payload: String): String {
        val escapedPayload = JSONObject.quote(payload)
        return buildString {
            append("{")
            append("\"model\":\"")
            append(GeminiConfig.model)
            append("\",")
            append("\"generationConfig\":")
            append(GeminiConfig.generationConfig)
            append(",")
            append("\"contents\":[")
            append("{\"role\":\"user\",\"parts\":[{\"text\":\"KafkaLauncher action log payload\"}]},")
            append("{\"role\":\"user\",\"parts\":[{\"text\":")
            append(escapedPayload)
            append("}]}]")
            append("}")
        }
    }

    private fun parse(response: String): GeminiRecommendations? {
        val root = JSONObject(response)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        val payload = parts.getJSONObject(0).optString("text")
        if (payload.isBlank()) return null
        val snapshot = GeminiRecommendationJson.decode(payload)
        val generated = snapshot.generatedAt.ifBlank { Instant.now().toString() }
        return snapshot.copy(generatedAt = generated)
    }
}
