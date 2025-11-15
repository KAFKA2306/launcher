package com.kafka.launcher.data.log

import android.content.Context
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.QuickAction
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickActionAuditLogger(context: Context) {
    private val directory = LogDirectoryWriter(context)
    private val snapshotFile = directory.resolve(LauncherConfig.quickActionSnapshotFileName)
    private val eventsFile = directory.resolve(LauncherConfig.quickActionEventsFileName)
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun writeSnapshot(actions: List<QuickAction>) {
        scope.launch {
            val content = buildString {
                append("snapshot=")
                append(timestamp())
                append('\n')
                actions.forEach { action ->
                    append(formatAction(action))
                    append('\n')
                }
            }
            directory.write {
                snapshotFile.writeText(content)
            }
        }
    }

    fun logExecution(action: QuickAction, query: String) {
        scope.launch {
            val line = buildString {
                append(timestamp())
                append(" | EXECUTE | id=")
                append(action.id)
                append(" | provider=")
                append(action.providerId)
                append(" | label=")
                append(action.label)
                append(" | type=")
                append(action.actionType.name)
                append(" | query=")
                append(query)
            }
            directory.write {
                eventsFile.appendText(line + "\n")
            }
        }
    }

    private fun formatAction(action: QuickAction): String = buildString {
        append("id=")
        append(action.id)
        append(", provider=")
        append(action.providerId)
        append(", label=")
        append(action.label)
        append(", type=")
        append(action.actionType.name)
        append(", data=")
        append(action.data.orEmpty())
        append(", package=")
        append(action.packageName.orEmpty())
        append(", priority=")
        append(action.priority)
    }

    private fun timestamp(): String = formatter.format(ZonedDateTime.now())
}
