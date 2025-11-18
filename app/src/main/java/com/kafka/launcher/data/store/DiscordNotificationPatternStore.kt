package com.kafka.launcher.data.store

import android.content.Context
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.model.NotificationPatternRule
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DiscordNotificationPatternStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, DiscordConfig.storeDirectory)
    private val file = File(directory, DiscordConfig.patternStoreFileName)
    private val state = MutableStateFlow(read())
    val data: Flow<List<NotificationPatternRule>> = state.asStateFlow()

    suspend fun update(transform: (List<NotificationPatternRule>) -> List<NotificationPatternRule>) {
        val updated = withContext(ioDispatcher) {
            val result = transform(state.value)
            directory.mkdirs()
            file.writeText(encode(result))
            result
        }
        state.value = updated
    }

    suspend fun snapshot(): List<NotificationPatternRule> {
        return state.value
    }

    private fun read(): List<NotificationPatternRule> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        val array = JSONArray(text)
        return buildList {
            for (index in 0 until array.length()) {
                val node = array.getJSONObject(index)
                add(
                    NotificationPatternRule(
                        id = node.optString("id"),
                        titleRegex = node.optString("titleRegex").ifBlank { null },
                        subTextRegex = node.optString("subTextRegex").ifBlank { null },
                        targetChannelId = node.optString("targetChannelId")
                    )
                )
            }
        }
    }

    private fun encode(rules: List<NotificationPatternRule>): String {
        val array = JSONArray()
        rules.forEach { rule ->
            val node = JSONObject()
            node.put("id", rule.id)
            node.put("titleRegex", rule.titleRegex ?: "")
            node.put("subTextRegex", rule.subTextRegex ?: "")
            node.put("targetChannelId", rule.targetChannelId)
            array.put(node)
        }
        return array.toString()
    }
}
