package com.kafka.launcher.data.store

import android.content.Context
import com.kafka.launcher.config.GeminiConfig
import com.kafka.launcher.domain.model.GeminiRecommendationJson
import com.kafka.launcher.domain.model.GeminiRecommendations
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GeminiRecommendationStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, GeminiConfig.recommendationDirectory)
    private val file = File(directory, GeminiConfig.recommendationFileName)
    private val store = MutableStateFlow(read())
    val data: Flow<GeminiRecommendations?> = store.asStateFlow()

    suspend fun update(recommendations: GeminiRecommendations) {
        directory.mkdirs()
        val json = GeminiRecommendationJson.encode(recommendations)
        file.writeText(json)
        store.value = recommendations
    }

    suspend fun snapshot(): GeminiRecommendations? {
        return store.value
    }

    private fun read(): GeminiRecommendations? {
        if (!file.exists()) {
            return null
        }
        val json = file.readText()
        if (json.isBlank()) {
            return null
        }
        return GeminiRecommendationJson.decode(json)
    }
}
