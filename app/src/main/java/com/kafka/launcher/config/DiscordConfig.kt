package com.kafka.launcher.config

import com.kafka.launcher.domain.model.DiscordRankingWeights

object DiscordConfig {
    const val packageName = "com.discord"
    const val baseUrl = "https://discord.com"
    const val channelPath = "/channels"
    const val appPath = "/app"
    const val storeDirectory = "discord"
    const val channelStoreFileName = "channels.pb"
    const val usageStoreFileName = "usage.pb"
    const val notificationStoreFileName = "notifications.pb"
    const val patternStoreFileName = "notification_rules.pb"
    const val mutedNamesKey = "discord_muted_names"
    const val selfNamesKey = "discord_self_names"
    const val rankingWeightsKey = "discord_ranking_weights"
    const val quickAccessGuideKey = "discord_quick_access_guide"
    const val bootstrapAssetPath = "discord/bootstrap.js"
    const val webModuleId = "discord_webview"
    const val webModuleLabel = "Discord WebView"
    val defaultRankingWeights = DiscordRankingWeights.Default
    val channelBaseUrl = "$baseUrl$channelPath"
    val defaultAppUrl = "$baseUrl$appPath"
}
