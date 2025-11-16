package com.kafka.launcher.domain.usecase

import com.kafka.launcher.domain.model.ActionStats
import com.kafka.launcher.domain.model.QuickAction

class RecommendActionsUseCase {
    operator fun invoke(actions: List<QuickAction>, stats: List<ActionStats>): List<QuickAction> {
        if (actions.isEmpty() || stats.isEmpty()) return emptyList()
        return stats.mapNotNull { stat -> actions.firstOrNull { it.id == stat.actionId } }
    }
}
