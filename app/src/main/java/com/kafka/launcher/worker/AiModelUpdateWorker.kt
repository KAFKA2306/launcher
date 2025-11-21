package com.kafka.launcher.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kafka.launcher.data.local.db.KafkaDatabase
import com.kafka.launcher.data.repo.AiModelRepository
import com.kafka.launcher.domain.model.AiModelStatus
import kotlinx.coroutines.flow.first

class AiModelUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val database by lazy { KafkaDatabase.build(appContext) }
    private val repository by lazy { AiModelRepository(database.aiModelDao()) }

    override suspend fun doWork(): Result {
        // 1. Fetch adopted models
        val adoptedModels = repository.adoptedModels.first()

        if (adoptedModels.isEmpty()) {
            return Result.success()
        }

        // 2. Mock check for updates
        // In a real app, we would fetch a remote catalog and compare versions.
        // Here, we just simulate a refresh which might pull new data if we had a real backend.
        // For now, we just call refreshModels to sync with our "mock remote".
        try {
            repository.refreshModels()
            
            // 3. Check if any adopted model has a newer version in the "remote" (which is just the repo's mock list for now)
            // Since refreshModels only inserts new ones in the current implementation, 
            // we would need logic to update existing ones. 
            // For this MVP, simply running refreshModels is enough to demonstrate the background job.
            
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}
