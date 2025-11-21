package com.kafka.launcher.data.repo

import com.kafka.launcher.data.local.db.AiModelDao
import com.kafka.launcher.domain.model.AiModel
import com.kafka.launcher.domain.model.AiModelStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
class AiModelRepository(
    private val aiModelDao: AiModelDao
) {
    val allModels: Flow<List<AiModel>> = aiModelDao.getAllModels()
    val candidateModels: Flow<List<AiModel>> = aiModelDao.getModelsByStatus(AiModelStatus.CANDIDATE)
    val adoptedModels: Flow<List<AiModel>> = aiModelDao.getModelsByStatus(AiModelStatus.ADOPTED)
    val rejectedModels: Flow<List<AiModel>> = aiModelDao.getModelsByStatus(AiModelStatus.REJECTED)

    suspend fun refreshModels() {
        // Mock remote fetch
        val remoteModels = listOf(
            AiModel(
                id = "gemini-nano",
                name = "Gemini Nano",
                description = "Efficient model for on-device tasks.",
                version = "1.0.0",
                downloadUrl = "https://example.com/gemini-nano.bin"
            ),
            AiModel(
                id = "gemma-2b",
                name = "Gemma 2B",
                description = "Lightweight open model.",
                version = "1.1.0",
                downloadUrl = "https://example.com/gemma-2b.bin"
            ),
            AiModel(
                id = "bert-mobile",
                name = "BERT Mobile",
                description = "Optimized BERT for text classification.",
                version = "2.3.1",
                downloadUrl = "https://example.com/bert-mobile.bin"
            )
        )

        // Sync logic: Insert new ones, keep existing status
        val existingModels = aiModelDao.getAllModels().first()
        val newModels = remoteModels.filter { remote ->
            existingModels.none { it.id == remote.id }
        }
        
        if (newModels.isNotEmpty()) {
            aiModelDao.insertModels(newModels)
        }
    }

    suspend fun adoptModel(modelId: String) {
        val model = aiModelDao.getModelById(modelId)
        if (model != null) {
            aiModelDao.updateModel(model.copy(status = AiModelStatus.ADOPTED))
            // Trigger download logic here in real implementation
        }
    }

    suspend fun rejectModel(modelId: String) {
        val model = aiModelDao.getModelById(modelId)
        if (model != null) {
            aiModelDao.updateModel(model.copy(status = AiModelStatus.REJECTED))
        }
    }

    suspend fun restoreModel(modelId: String) {
        val model = aiModelDao.getModelById(modelId)
        if (model != null) {
            aiModelDao.updateModel(model.copy(status = AiModelStatus.CANDIDATE))
        }
    }
}
