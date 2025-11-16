package com.kafka.launcher.domain.model

data class GeminiRecommendations(
    val generatedAt: String,
    val windows: List<GeminiRecommendationWindow> = emptyList(),
    val globalPins: List<String> = emptyList(),
    val suppressions: List<String> = emptyList(),
    val rationales: List<GeminiRecommendationRationale> = emptyList(),
    val newActions: List<GeminiGeneratedAction> = emptyList()
)

data class GeminiRecommendationWindow(
    val id: String,
    val start: String? = null,
    val end: String? = null,
    val primaryActionIds: List<String> = emptyList(),
    val fallbackActionIds: List<String> = emptyList()
)

data class GeminiRecommendationRationale(
    val targetId: String,
    val summary: String
)

data class GeminiGeneratedAction(
    val id: String,
    val label: String,
    val actionType: String,
    val data: String? = null,
    val packageName: String? = null,
    val timeWindows: List<String> = emptyList()
)
