package com.kafka.launcher.domain.usecase

import com.kafka.launcher.config.GeminiConfig
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.ActionLog
import com.kafka.launcher.domain.model.ActionStats
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class GeminiPayloadBuilder {
    fun build(events: List<ActionLog>, stats: List<ActionStats>): String {
        val ordered = events.sortedBy { it.timestamp }.map { TimedLog(it) }
        val windows = GeminiConfig.timeWindows
        val actionFallback = stats
            .filterNot { it.actionId.startsWith(LauncherConfig.appUsagePrefix) }
            .map { it.actionId to it.count.toInt() }
            .take(GeminiConfig.payloadActionLimit)
        val appFallback = stats
            .filter { it.actionId.startsWith(LauncherConfig.appUsagePrefix) }
            .map { it.actionId.removePrefix(LauncherConfig.appUsagePrefix) to it.count.toInt() }
            .take(GeminiConfig.payloadAppLimit)
        val payload = buildString {
            append("{")
            append("\"timeWindowStats\":[")
            windows.forEachIndexed { index, definition ->
                val matched = ordered.filter { fits(definition, it) }
                append(windowJson(definition.id, matched, actionFallback, appFallback))
                if (index < windows.lastIndex) append(",")
            }
            append("],")
            append("\"recentAnomalies\":[]")
            append("}")
        }
        return payload
    }

    private fun windowJson(
        id: String,
        logs: List<TimedLog>,
        fallbackActions: List<Pair<String, Int>>,
        fallbackApps: List<Pair<String, Int>>
    ): String {
        val actions = topActions(logs).ifEmpty { fallbackActions }
        val apps = topApps(logs).ifEmpty { fallbackApps }
        val sequence = recentSequence(logs)
        return buildString {
            append("{")
            append("\"windowId\":\"")
            append(id)
            append("\",")
            append("\"topActions\":[")
            actions.forEachIndexed { index, entry ->
                append("{\"id\":\"")
                append(entry.first)
                append("\",\"count\":")
                append(entry.second)
                append(",\"successRate\":1.0}")
                if (index < actions.lastIndex) append(",")
            }
            append("],")
            append("\"topApps\":[")
            apps.forEachIndexed { index, entry ->
                append("{\"packageName\":\"")
                append(entry.first)
                append("\",\"count\":")
                append(entry.second)
                append("}")
                if (index < apps.lastIndex) append(",")
            }
            append("],")
            append("\"recentActionSequence\":[")
            sequence.forEachIndexed { index, action ->
                append("\"")
                append(action)
                append("\"")
                if (index < sequence.lastIndex) append(",")
            }
            append("]")
            append("}")
        }
    }

    private fun topActions(logs: List<TimedLog>): List<Pair<String, Int>> {
        return logs
            .filterNot { it.actionId.startsWith(LauncherConfig.appUsagePrefix) }
            .groupingBy { it.actionId }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(GeminiConfig.payloadActionLimit)
            .map { it.key to it.value }
    }

    private fun topApps(logs: List<TimedLog>): List<Pair<String, Int>> {
        return logs
            .filter { it.actionId.startsWith(LauncherConfig.appUsagePrefix) }
            .groupingBy { it.actionId.removePrefix(LauncherConfig.appUsagePrefix) }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(GeminiConfig.payloadAppLimit)
            .map { it.key to it.value }
    }

    private fun recentSequence(logs: List<TimedLog>): List<String> {
        return logs
            .asReversed()
            .filterNot { it.actionId.startsWith(LauncherConfig.appUsagePrefix) }
            .map { it.actionId }
            .distinct()
            .take(GeminiConfig.payloadSequenceLimit)
            .reversed()
    }

    private fun fits(definition: GeminiConfig.TimeWindowDefinition, log: TimedLog): Boolean {
        if (log.isWeekend && !definition.appliesToWeekends) return false
        if (!log.isWeekend && !definition.appliesToWeekdays) return false
        val start = definition.startHour * 60 + definition.startMinute
        val end = definition.endHour * 60 + definition.endMinute
        val minute = log.minuteOfDay
        return if (start <= end) {
            minute in start until end
        } else {
            minute >= start || minute < end
        }
    }

    private class TimedLog(action: ActionLog) {
        private val timestamp: ZonedDateTime = Instant.ofEpochMilli(action.timestamp).atZone(ZoneOffset.UTC)
        val minuteOfDay: Int = timestamp.hour * 60 + timestamp.minute
        val isWeekend: Boolean = timestamp.dayOfWeek.value == 6 || timestamp.dayOfWeek.value == 7
        val actionId: String = action.actionId
    }
}
