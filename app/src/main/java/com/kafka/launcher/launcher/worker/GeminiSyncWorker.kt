package com.kafka.launcher.launcher.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kafka.launcher.config.GeminiConfig
import com.kafka.launcher.data.local.db.KafkaDatabase
import com.kafka.launcher.data.log.ActionLogFileWriter
import com.kafka.launcher.data.log.GeminiSyncLogEvent
import com.kafka.launcher.data.log.GeminiSyncLogWriter
import com.kafka.launcher.data.quickaction.QuickActionCatalogStore
import com.kafka.launcher.data.remote.GeminiApiClient
import com.kafka.launcher.data.repo.ActionLogRepository
import com.kafka.launcher.data.store.GeminiApiKeyStore
import com.kafka.launcher.data.store.GeminiRecommendationStore
import com.kafka.launcher.domain.usecase.GeminiFeedbackSignal
import com.kafka.launcher.domain.usecase.GeminiPayloadBuilder
import com.kafka.launcher.launcher.AiSyncStageKey
import com.kafka.launcher.launcher.AiSyncStatus
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

class GeminiSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val context = appContext.applicationContext
    private val database by lazy { KafkaDatabase.build(context) }
    private val actionLogRepository by lazy { ActionLogRepository(database.actionLogDao(), ActionLogFileWriter(context)) }
    private val recommendationStore = GeminiRecommendationStore(context)
    private val quickActionCatalogStore = QuickActionCatalogStore(context)
    private val payloadBuilder = GeminiPayloadBuilder()
    private val apiClient = GeminiApiClient()
    private val apiKeyStore = GeminiApiKeyStore(context)
    private val syncLogWriter = GeminiSyncLogWriter(context)

    override suspend fun doWork(): Result {
        val startedAt = System.nanoTime()
        val last = recommendationStore.snapshot()
        val now = Instant.now()
        if (last != null && last.generatedAt.isNotBlank()) {
            val lastInstant = Instant.parse(last.generatedAt)
            if (Duration.between(lastInstant, now).toHours() < GeminiConfig.periodHours) {
                record(GeminiSyncLogEvent(stage = "skipped", reason = "fresh_snapshot", durationMs = elapsedMs(startedAt)))
                return Result.success(workDataOf(AiSyncStageKey to AiSyncStatus.Succeeded.stageId))
            }
        }

        val events = actionLogRepository.exportEvents(GeminiConfig.payloadEventLimit)
        val stats = actionLogRepository.statsSnapshot(GeminiConfig.payloadEventLimit)
        val feedback = quickActionCatalogStore.snapshot().entries.map { entry ->
            GeminiFeedbackSignal(
                id = entry.id,
                usageCount = entry.usageCount,
                acceptedCount = entry.acceptedCount,
                dismissedCount = entry.dismissedCount
            )
        }
        if (events.isEmpty() && stats.isEmpty() && feedback.isEmpty()) {
            record(
                GeminiSyncLogEvent(
                    stage = "skipped",
                    reason = "no_input",
                    durationMs = elapsedMs(startedAt)
                )
            )
            return Result.success(workDataOf(AiSyncStageKey to AiSyncStatus.Succeeded.stageId))
        }

        val apiKey = apiKeyStore.current()
        if (apiKey.isBlank()) {
            record(
                GeminiSyncLogEvent(
                    stage = "skipped",
                    reason = "api_key_missing",
                    eventCount = events.size,
                    statCount = stats.size,
                    feedbackCount = feedback.size,
                    durationMs = elapsedMs(startedAt)
                )
            )
            return Result.success(workDataOf(AiSyncStageKey to AiSyncStatus.Succeeded.stageId))
        }

        setProgress(workDataOf(AiSyncStageKey to AiSyncStatus.Running.stageId))
        val payload = payloadBuilder.build(events, stats, feedback)
        val payloadSha256 = sha256(payload)
        record(
            GeminiSyncLogEvent(
                stage = "request",
                reason = "gemini_sync",
                eventCount = events.size,
                statCount = stats.size,
                feedbackCount = feedback.size,
                payloadSha256 = payloadSha256,
                durationMs = elapsedMs(startedAt)
            )
        )

        val recommendations = try {
            apiClient.fetchRecommendations(payload, apiKey)
        } catch (_: Exception) {
            record(
                GeminiSyncLogEvent(
                    stage = "failed",
                    reason = "api_exception",
                    eventCount = events.size,
                    statCount = stats.size,
                    feedbackCount = feedback.size,
                    payloadSha256 = payloadSha256,
                    durationMs = elapsedMs(startedAt)
                )
            )
            return Result.retry()
        }

        if (recommendations == null) {
            record(
                GeminiSyncLogEvent(
                    stage = "completed",
                    reason = "no_result",
                    eventCount = events.size,
                    statCount = stats.size,
                    feedbackCount = feedback.size,
                    payloadSha256 = payloadSha256,
                    durationMs = elapsedMs(startedAt)
                )
            )
            return Result.success(workDataOf(AiSyncStageKey to AiSyncStatus.Succeeded.stageId))
        }

        setProgress(workDataOf(AiSyncStageKey to AiSyncStatus.UpdatingCatalog.stageId))
        val stamped = recommendations.copy(generatedAt = now.toString())
        recommendationStore.update(stamped)
        quickActionCatalogStore.mergeFromGemini(stamped.newActions, stamped.generatedAt)
        record(
            GeminiSyncLogEvent(
                stage = "completed",
                reason = "recommendations_applied",
                eventCount = events.size,
                statCount = stats.size,
                feedbackCount = feedback.size,
                recommendationCount = stamped.newActions.size,
                payloadSha256 = payloadSha256,
                durationMs = elapsedMs(startedAt)
            )
        )
        return Result.success(workDataOf(AiSyncStageKey to AiSyncStatus.Succeeded.stageId))
    }

    private suspend fun record(event: GeminiSyncLogEvent) {
        try {
            syncLogWriter.record(event)
        } catch (_: Exception) {
            // Observability must never prevent the launcher or Gemini sync from progressing.
        }
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

    private fun sha256(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
