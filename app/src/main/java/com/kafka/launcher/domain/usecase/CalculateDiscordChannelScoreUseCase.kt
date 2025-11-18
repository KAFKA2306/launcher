package com.kafka.launcher.domain.usecase

import com.kafka.launcher.domain.model.ChannelUsageStats
import com.kafka.launcher.domain.model.DiscordRankingWeights
import kotlin.math.ln

class CalculateDiscordChannelScoreUseCase {
    operator fun invoke(stats: ChannelUsageStats, weights: DiscordRankingWeights): Double {
        val openTerm = weights.open * ln(1.0 + stats.openCount.toDouble())
        val focusTerm = weights.focus * ln(1.0 + stats.totalFocusSeconds.toDouble() / 60.0)
        val postTerm = weights.post * stats.postCount
        val notificationTerm = weights.notification * stats.notificationCount
        return openTerm + focusTerm + postTerm + notificationTerm
    }
}
