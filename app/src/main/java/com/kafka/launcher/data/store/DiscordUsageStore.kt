package com.kafka.launcher.data.store

import android.content.Context
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.model.ChannelUsageStats
import com.kafka.launcher.domain.model.DiscordChannelKey
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DiscordUsageStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, DiscordConfig.storeDirectory)
    private val file = File(directory, DiscordConfig.usageStoreFileName)
    private val state = MutableStateFlow(read())
    val data: Flow<List<ChannelUsageStats>> = state.asStateFlow()

    suspend fun update(transform: (List<ChannelUsageStats>) -> List<ChannelUsageStats>) {
        val updated = withContext(ioDispatcher) {
            val result = transform(state.value)
            directory.mkdirs()
            file.writeText(encode(result))
            result
        }
        state.value = updated
    }

    suspend fun snapshot(): List<ChannelUsageStats> {
        return state.value
    }

    private fun read(): List<ChannelUsageStats> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        val array = JSONArray(text)
        return buildList {
            for (index in 0 until array.length()) {
                val node = array.getJSONObject(index)
                val key = DiscordChannelKey(
                    guildId = node.optString("guildId"),
                    channelId = node.optString("channelId"),
                    threadId = node.optString("threadId").ifBlank { null }
                )
                add(
                    ChannelUsageStats(
                        channelKey = key,
                        channelId = node.optString("channelIdValue", key.stableId),
                        openCount = node.optLong("openCount"),
                        totalFocusSeconds = node.optLong("totalFocusSeconds"),
                        postCount = node.optLong("postCount"),
                        notificationCount = node.optLong("notificationCount"),
                        lastActiveAt = node.optLong("lastActiveAt")
                    )
                )
            }
        }
    }

    private fun encode(stats: List<ChannelUsageStats>): String {
        val array = JSONArray()
        stats.forEach { entry ->
            val node = JSONObject()
            node.put("guildId", entry.channelKey.guildId)
            node.put("channelId", entry.channelKey.channelId)
            node.put("threadId", entry.channelKey.threadId ?: "")
            node.put("channelIdValue", entry.channelId)
            node.put("openCount", entry.openCount)
            node.put("totalFocusSeconds", entry.totalFocusSeconds)
            node.put("postCount", entry.postCount)
            node.put("notificationCount", entry.notificationCount)
            node.put("lastActiveAt", entry.lastActiveAt)
            array.put(node)
        }
        return array.toString()
    }
}
