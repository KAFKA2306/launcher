package com.kafka.launcher.remote

import com.kafka.launcher.domain.model.ActionLog

class ApiClient {
    suspend fun uploadLogs(logs: List<ActionLog>) = Unit
    suspend fun fetchRecommendations(): List<String> = emptyList()
}
