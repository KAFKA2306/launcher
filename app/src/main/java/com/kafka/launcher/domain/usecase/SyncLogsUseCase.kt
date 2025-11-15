package com.kafka.launcher.domain.usecase

import com.kafka.launcher.domain.model.ActionLog
import com.kafka.launcher.remote.ApiClient

class SyncLogsUseCase(private val apiClient: ApiClient) {
    suspend operator fun invoke(logs: List<ActionLog>) {
        apiClient.uploadLogs(logs)
    }
}
