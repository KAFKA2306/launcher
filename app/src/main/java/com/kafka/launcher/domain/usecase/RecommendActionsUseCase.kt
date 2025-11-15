package com.kafka.launcher.domain.usecase

import com.kafka.launcher.domain.model.ActionStats
import com.kafka.launcher.domain.model.QuickAction

class RecommendActionsUseCase {
    operator fun invoke(actions: List<QuickAction>, stats: List<ActionStats>, fallback: List<QuickAction>): List<QuickAction> {
        if (stats.isEmpty()) return fallback
        val ranked = stats.mapNotNull { stat -> actions.firstOrNull { it.id == stat.actionId } }
        return if (ranked.isEmpty()) fallback else ranked
    }
}
