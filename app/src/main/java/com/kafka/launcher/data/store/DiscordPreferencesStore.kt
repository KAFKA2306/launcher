package com.kafka.launcher.data.store

import android.content.Context
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.model.DiscordPreferences
import com.kafka.launcher.domain.model.DiscordRankingWeights
import com.kafka.launcher.domain.model.MutedDisplayNameEntry
import com.kafka.launcher.domain.model.SelfDisplayNameEntry
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DiscordPreferencesStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, DiscordConfig.storeDirectory)
    private val file = File(directory, "preferences.json")
    private val state = MutableStateFlow(read())
    val data: Flow<DiscordPreferences> = state.asStateFlow()

    suspend fun update(transform: (DiscordPreferences) -> DiscordPreferences) {
        val updated = withContext(ioDispatcher) {
            val result = transform(state.value)
            directory.mkdirs()
            file.writeText(encode(result))
            result
        }
        state.value = updated
    }

    suspend fun snapshot(): DiscordPreferences {
        return state.value
    }

    private fun read(): DiscordPreferences {
        if (!file.exists()) {
            return DiscordPreferences()
        }
        val content = file.readText()
        if (content.isBlank()) {
            return DiscordPreferences()
        }
        val node = JSONObject(content)
        val muted = node.optJSONArray("mutedNames")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val entry = array.getJSONObject(index)
                    add(
                        MutedDisplayNameEntry(
                            canonicalName = entry.optString("canonicalName"),
                            aliases = entry.optJSONArray("aliases")?.let { aliasArray ->
                                buildList {
                                    for (aliasIndex in 0 until aliasArray.length()) {
                                        add(aliasArray.getString(aliasIndex))
                                    }
                                }
                            } ?: emptyList()
                        )
                    )
                }
            }
        } ?: emptyList()
        val self = node.optJSONArray("selfNames")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val entry = array.getJSONObject(index)
                    add(
                        SelfDisplayNameEntry(
                            canonicalName = entry.optString("canonicalName"),
                            aliases = entry.optJSONArray("aliases")?.let { aliasArray ->
                                buildList {
                                    for (aliasIndex in 0 until aliasArray.length()) {
                                        add(aliasArray.getString(aliasIndex))
                                    }
                                }
                            } ?: emptyList()
                        )
                    )
                }
            }
        } ?: emptyList()
        val weightsNode = node.optJSONObject("rankingWeights")
        val weights = if (weightsNode != null) {
            DiscordRankingWeights(
                open = weightsNode.optInt("open", DiscordRankingWeights.Default.open),
                focus = weightsNode.optInt("focus", DiscordRankingWeights.Default.focus),
                post = weightsNode.optInt("post", DiscordRankingWeights.Default.post),
                notification = weightsNode.optInt("notification", DiscordRankingWeights.Default.notification)
            )
        } else {
            DiscordRankingWeights.Default
        }
        val dismissed = node.optBoolean("quickAccessGuideDismissed", false)
        return DiscordPreferences(
            mutedNames = muted,
            selfNames = self,
            rankingWeights = weights,
            quickAccessGuideDismissed = dismissed
        )
    }

    private fun encode(preferences: DiscordPreferences): String {
        val root = JSONObject()
        val muted = JSONArray()
        preferences.mutedNames.forEach { entry ->
            val node = JSONObject()
            node.put("canonicalName", entry.canonicalName)
            node.put("aliases", JSONArray(entry.aliases))
            muted.put(node)
        }
        val self = JSONArray()
        preferences.selfNames.forEach { entry ->
            val node = JSONObject()
            node.put("canonicalName", entry.canonicalName)
            node.put("aliases", JSONArray(entry.aliases))
            self.put(node)
        }
        val weights = JSONObject()
        weights.put("open", preferences.rankingWeights.open)
        weights.put("focus", preferences.rankingWeights.focus)
        weights.put("post", preferences.rankingWeights.post)
        weights.put("notification", preferences.rankingWeights.notification)
        root.put("mutedNames", muted)
        root.put("selfNames", self)
        root.put("rankingWeights", weights)
        root.put("quickAccessGuideDismissed", preferences.quickAccessGuideDismissed)
        return root.toString()
    }
}
