package com.kafka.launcher.data.quickaction

import android.content.Context
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.GeminiGeneratedAction
import java.io.File
import java.time.Instant
import java.util.Locale
import kotlin.jvm.Volatile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuickActionCatalogStore(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, LauncherConfig.quickActionCatalogDirectory)
    private val file = File(directory, LauncherConfig.quickActionCatalogFileName)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val state = sharedState
    val data: Flow<QuickActionCatalog> = state.asStateFlow()

    init {
        if (!initialized) {
            synchronized(fileLock) {
                if (!initialized) {
                    state.value = read()
                    initialized = true
                }
            }
        }
    }

    fun snapshot(): QuickActionCatalog = state.value

    fun mergeFromGemini(actions: List<GeminiGeneratedAction>, generatedAt: String) {
        if (actions.isEmpty()) return
        synchronized(fileLock) {
            val current = state.value
            val timestamp = resolvedTimestamp(generatedAt)
            val map = current.entries.associateBy { it.id }.toMutableMap()
            actions.forEach { action ->
                val mappedType = mapActionType(action.actionType) ?: return@forEach
                val existing = map[action.id]
                val entry = if (existing == null) {
                    QuickActionCatalogEntry(
                        id = action.id,
                        label = action.label,
                        actionType = mappedType.name,
                        data = action.data,
                        packageName = action.packageName,
                        createdAt = timestamp,
                        updatedAt = timestamp,
                        timeWindows = action.timeWindows
                    )
                } else {
                    existing.copy(
                        label = action.label,
                        actionType = mappedType.name,
                        data = action.data,
                        packageName = action.packageName,
                        updatedAt = timestamp,
                        timeWindows = action.timeWindows
                    )
                }
                map[action.id] = entry
            }
            val updated = QuickActionCatalog(
                updatedAt = timestamp,
                entries = map.values.sortedBy { it.id }
            )
            write(updated)
        }
    }

    fun incrementUsage(id: String) {
        updateEntry(id) { entry -> entry.copy(usageCount = entry.usageCount + 1) }
    }

    fun incrementAccepted(id: String) {
        updateEntry(id) { entry -> entry.copy(acceptedCount = entry.acceptedCount + 1) }
    }

    fun incrementDismissed(id: String) {
        updateEntry(id) { entry -> entry.copy(dismissedCount = entry.dismissedCount + 1) }
    }

    fun clearDismissed(id: String) {
        updateEntry(id) { entry -> entry.copy(dismissedCount = 0) }
    }

    private fun updateEntry(id: String, transform: (QuickActionCatalogEntry) -> QuickActionCatalogEntry) {
        synchronized(fileLock) {
            val current = state.value
            val base = current.entries.firstOrNull { it.id == id } ?: return
            val updatedEntries = current.entries.map { entry ->
                if (entry.id == id) transform(base) else entry
            }
            val updated = current.copy(entries = updatedEntries)
            write(updated)
        }
    }

    private fun write(catalog: QuickActionCatalog) {
        directory.mkdirs()
        val payload = json.encodeToString(catalog)
        file.writeText(payload)
        state.value = catalog
    }

    private fun read(): QuickActionCatalog {
        if (!file.exists()) return QuickActionCatalog()
        val text = file.readText()
        if (text.isBlank()) return QuickActionCatalog()
        return json.decodeFromString<QuickActionCatalog>(text)
    }

    private fun mapActionType(value: String): ActionType? {
        if (value.isBlank()) return null
        return when (value.uppercase(Locale.US)) {
            "APP_DEEP_LINK" -> ActionType.BROWSER_URL
            "URL" -> ActionType.BROWSER_URL
            "APP_MAIN" -> ActionType.OPEN_APP
            "MAPS_NAVIGATION" -> ActionType.MAP_NAVIGATION
            "MAPS_VIEW" -> ActionType.MAP_VIEW
            "EMAIL_COMPOSE" -> ActionType.EMAIL_COMPOSE
            "EMAIL_INBOX" -> ActionType.EMAIL_INBOX
            "CALENDAR_VIEW" -> ActionType.CALENDAR_VIEW
            "CALENDAR_INSERT" -> ActionType.CALENDAR_INSERT
            "BROWSER_URL" -> ActionType.BROWSER_URL
            "DISCORD_OPEN" -> ActionType.DISCORD_OPEN
            else -> null
        }
    }

    private fun resolvedTimestamp(value: String): String {
        if (value.isBlank()) return Instant.now().toString()
        return value
    }
}

private val sharedState = MutableStateFlow(QuickActionCatalog())
@Volatile
private var initialized = false
private val fileLock = Any()
