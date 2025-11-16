package com.kafka.launcher.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kafka.launcher.config.GeminiConfig
import com.kafka.launcher.domain.model.GeminiRecommendationJson
import com.kafka.launcher.domain.model.GeminiRecommendations
import java.io.File
import okio.Path.Companion.toPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class GeminiRecommendationStore(context: Context) {
    private val appContext = context.applicationContext
    private val payloadKey = stringPreferencesKey(GeminiConfig.recommendationStoreKey)
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val directory = File(appContext.filesDir, GeminiConfig.recommendationDirectory)
            directory.mkdirs()
            directory.resolve(GeminiConfig.recommendationFileName).absolutePath.toPath()
        }
    )

    val data: Flow<GeminiRecommendations?> = dataStore.data.map { prefs ->
        prefs[payloadKey]?.let { GeminiRecommendationJson.decode(it) }
    }

    suspend fun update(recommendations: GeminiRecommendations) {
        val json = GeminiRecommendationJson.encode(recommendations)
        dataStore.edit { prefs ->
            prefs[payloadKey] = json
        }
    }

    suspend fun snapshot(): GeminiRecommendations? {
        return data.firstOrNull()
    }
}
