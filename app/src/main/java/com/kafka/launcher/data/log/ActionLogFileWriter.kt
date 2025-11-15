package com.kafka.launcher.data.log

import android.content.Context
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.ActionLog
import com.kafka.launcher.domain.model.ActionStats
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ActionLogFileWriter(context: Context) {
    private val directory = LogDirectoryWriter(context)
    private val eventsFile = directory.resolve(LauncherConfig.actionLogEventsFileName)
    private val recentFile = directory.resolve(LauncherConfig.actionLogRecentFileName)
    private val statsFile = directory.resolve(LauncherConfig.actionLogStatsFileName)
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    suspend fun append(log: ActionLog) {
        directory.write {
            val content = buildString {
                append("{\"generated\":\"")
                append(timestamp())
                append("\",\"actionId\":\"")
                append(log.actionId)
                append("\",\"eventTimestamp\":\"")
                append(formatTimestamp(log.timestamp))
                append("\"}")
            }
            eventsFile.appendText(content + "\n")
        }
    }

    suspend fun writeRecent(logs: List<ActionLog>) {
        directory.write {
            val content = buildString {
                append("{\"generated\":\"")
                append(timestamp())
                append("\",\"entries\":[")
                logs.forEachIndexed { index, log ->
                    append("{\"actionId\":\"")
                    append(log.actionId)
                    append("\",\"timestamp\":\"")
                    append(formatTimestamp(log.timestamp))
                    append("\"}")
                    if (index < logs.lastIndex) append(",")
                }
                append("]}")
            }
            recentFile.writeText(content)
        }
    }

    suspend fun writeStats(stats: List<ActionStats>) {
        directory.write {
            val content = buildString {
                append("{\"generated\":\"")
                append(timestamp())
                append("\",\"entries\":[")
                stats.forEachIndexed { index, stat ->
                    append("{\"actionId\":\"")
                    append(stat.actionId)
                    append("\",\"count\":")
                    append(stat.count)
                    append("}")
                    if (index < stats.lastIndex) append(",")
                }
                append("]}")
            }
            statsFile.writeText(content)
        }
    }

    private fun timestamp(): String = formatter.format(ZonedDateTime.now())

    private fun formatTimestamp(value: Long): String = formatter.format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
}
