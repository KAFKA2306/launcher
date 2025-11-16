package com.kafka.launcher.launcher.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

import com.kafka.launcher.config.GeminiConfig
import com.kafka.launcher.data.local.db.KafkaDatabase
import com.kafka.launcher.data.log.ActionLogFileWriter
import com.kafka.launcher.data.quickaction.QuickActionCatalogStore
import com.kafka.launcher.data.remote.GeminiApiClient
import com.kafka.launcher.data.repo.ActionLogRepository
import com.kafka.launcher.data.store.GeminiRecommendationStore
import com.kafka.launcher.data.store.GeminiApiKeyStore
import com.kafka.launcher.domain.usecase.GeminiPayloadBuilder
import com.kafka.launcher.launcher.AiSyncStageKey
import com.kafka.launcher.launcher.AiSyncStatus
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

    override suspend fun doWork(): Result {
        val last = recommendationStore.snapshot()
        val now = Instant.now()
        if (last != null && last.generatedAt.isNotBlank()) {
            val lastInstant = Instant.parse(last.generatedAt)
            if (Duration.between(lastInstant, now).toHours() < GeminiConfig.periodHours) {
                return Result.success(workDataOf(AiSyncStageKey to AiSyncStatus.Succeeded.stageId))
            }
        }
        val events = actionLogRepository.exportEvents(GeminiConfig.payloadEventLimit)
        val stats = actionLogRepository.statsSnapshot(GeminiConfig.payloadEventLimit)
        if (events.isEmpty() && stats.isEmpty()) {
            return Result.success(workDataOf(AiSyncStageKey to AiSyncStatus.Succeeded.stageId))
        }
        val apiKey = apiKeyStore.current()
        if (apiKey.isBlank()) {
            return Result.success(workDataOf(AiSyncStageKey to AiSyncStatus.Succeeded.stageId))
        }
        setProgress(workDataOf(AiSyncStageKey to AiSyncStatus.Running.stageId))
        val payload = payloadBuilder.build(events, stats)
        val recommendations = apiClient.fetchRecommendations(payload, apiKey)
        if (recommendations != null) {
            setProgress(workDataOf(AiSyncStageKey to AiSyncStatus.UpdatingCatalog.stageId))
            val stamped = recommendations.copy(generatedAt = now.toString())
            recommendationStore.update(stamped)
            quickActionCatalogStore.mergeFromGemini(stamped.newActions, stamped.generatedAt)
        }
        return Result.success(workDataOf(AiSyncStageKey to AiSyncStatus.Succeeded.stageId))
    }
}
