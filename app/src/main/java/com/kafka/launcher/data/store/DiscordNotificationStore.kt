package com.kafka.launcher.data.store

import android.content.Context
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.model.DiscordNotificationRecord
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DiscordNotificationStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, DiscordConfig.storeDirectory)
    private val file = File(directory, DiscordConfig.notificationStoreFileName)
    private val state = MutableStateFlow(read())
    val data: Flow<List<DiscordNotificationRecord>> = state.asStateFlow()

    suspend fun update(transform: (List<DiscordNotificationRecord>) -> List<DiscordNotificationRecord>) {
        val updated = withContext(ioDispatcher) {
            val result = transform(state.value)
            directory.mkdirs()
            file.writeText(encode(result))
            result
        }
        state.value = updated
    }

    suspend fun snapshot(): List<DiscordNotificationRecord> {
        return state.value
    }

    private fun read(): List<DiscordNotificationRecord> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        val array = JSONArray(text)
        return buildList {
            for (index in 0 until array.length()) {
                val node = array.getJSONObject(index)
                add(
                    DiscordNotificationRecord(
                        id = node.optLong("id"),
                        timestamp = node.optLong("timestamp"),
                        title = node.optString("title"),
                        subText = node.optString("subText").ifBlank { null },
                        text = node.optString("text").ifBlank { null },
                        mappedChannelId = node.optString("mappedChannelId").ifBlank { null }
                    )
                )
            }
        }
    }

    private fun encode(records: List<DiscordNotificationRecord>): String {
        val array = JSONArray()
        records.forEach { record ->
            val node = JSONObject()
            node.put("id", record.id)
            node.put("timestamp", record.timestamp)
            node.put("title", record.title)
            node.put("subText", record.subText ?: "")
            node.put("text", record.text ?: "")
            node.put("mappedChannelId", record.mappedChannelId ?: "")
            array.put(node)
        }
        return array.toString()
    }
}
