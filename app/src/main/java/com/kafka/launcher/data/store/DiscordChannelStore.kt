package com.kafka.launcher.data.store

import android.content.Context
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.model.DiscordChannel
import com.kafka.launcher.domain.model.DiscordChannelKey
import com.kafka.launcher.domain.model.DiscordChannelType
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DiscordChannelStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, DiscordConfig.storeDirectory)
    private val file = File(directory, DiscordConfig.channelStoreFileName)
    private val state = MutableStateFlow(read())
    val data: Flow<List<DiscordChannel>> = state.asStateFlow()

    suspend fun update(transform: (List<DiscordChannel>) -> List<DiscordChannel>) {
        val updated = withContext(ioDispatcher) {
            val result = transform(state.value)
            directory.mkdirs()
            file.writeText(encode(result))
            result
        }
        state.value = updated
    }

    suspend fun snapshot(): List<DiscordChannel> {
        return state.value
    }

    private fun read(): List<DiscordChannel> {
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
                val typeValue = node.optString("type", DiscordChannelType.CHANNEL.name)
                val type = if (typeValue == DiscordChannelType.THREAD.name) DiscordChannelType.THREAD else DiscordChannelType.CHANNEL
                add(
                    DiscordChannel(
                        key = key,
                        id = node.optString("id", key.stableId),
                        type = type,
                        url = node.optString("url"),
                        label = node.optString("label"),
                        serverName = node.optString("serverName"),
                        categoryName = node.optString("categoryName").ifBlank { null },
                        tags = node.optJSONArray("tags")?.let { tags ->
                            buildList {
                                for (tagIndex in 0 until tags.length()) {
                                    add(tags.getString(tagIndex))
                                }
                            }
                        } ?: emptyList(),
                        favorite = node.optBoolean("favorite", false)
                    )
                )
            }
        }
    }

    private fun encode(channels: List<DiscordChannel>): String {
        val array = JSONArray()
        channels.forEach { channel ->
            val node = JSONObject()
            node.put("id", channel.id)
            node.put("guildId", channel.key.guildId)
            node.put("channelId", channel.key.channelId)
            node.put("threadId", channel.key.threadId ?: "")
            node.put("type", channel.type.name)
            node.put("url", channel.url)
            node.put("label", channel.label)
            node.put("serverName", channel.serverName)
            node.put("categoryName", channel.categoryName ?: "")
            node.put("tags", JSONArray(channel.tags))
            node.put("favorite", channel.favorite)
            array.put(node)
        }
        return array.toString()
    }
}
