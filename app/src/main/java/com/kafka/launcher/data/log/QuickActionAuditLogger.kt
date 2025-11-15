package com.kafka.launcher.data.log

import android.content.Context
import com.kafka.launcher.domain.model.QuickAction
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class QuickActionAuditLogger(context: Context) {
    private val logDir = File(context.getExternalFilesDir(null), "logs").apply { mkdirs() }
    private val snapshotFile = File(logDir, "quickactions_snapshot.txt")
    private val eventsFile = File(logDir, "quickactions_events.txt")
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun writeSnapshot(actions: List<QuickAction>) {
        val content = buildString {
            append("snapshot=")
            append(timestamp())
            append('\n')
            actions.forEach { action ->
                append(formatAction(action))
                append('\n')
            }
        }
        snapshotFile.writeText(content)
    }

    fun logExecution(action: QuickAction, query: String) {
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
        eventsFile.appendText(line + "\n")
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
