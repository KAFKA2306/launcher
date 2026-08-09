package com.kafka.launcher.data.log

import android.content.Context
import com.kafka.launcher.config.LauncherConfig
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONObject

class GeminiSyncLogWriter(context: Context) {
    private val directory = LogDirectoryWriter(context)
    private val eventsFile = directory.resolve(LauncherConfig.geminiSyncEventsFileName)
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    suspend fun record(event: GeminiSyncLogEvent) {
        directory.write {
            val payload = JSONObject()
                .put("schemaVersion", 1)
                .put("generated", formatter.format(ZonedDateTime.now()))
                .put("stage", event.stage)
                .put("reason", event.reason)
                .put("eventCount", event.eventCount)
                .put("statCount", event.statCount)
                .put("feedbackCount", event.feedbackCount)
                .put("recommendationCount", event.recommendationCount)
                .put("durationMs", event.durationMs)
            if (event.payloadSha256 != null) {
                payload.put("payloadSha256", event.payloadSha256)
            }
            eventsFile.appendText(payload.toString() + "\n")
        }
    }
}

data class GeminiSyncLogEvent(
    val stage: String,
    val reason: String,
    val eventCount: Int = 0,
    val statCount: Int = 0,
    val feedbackCount: Int = 0,
    val recommendationCount: Int = 0,
    val durationMs: Long = 0,
    val payloadSha256: String? = null
)
