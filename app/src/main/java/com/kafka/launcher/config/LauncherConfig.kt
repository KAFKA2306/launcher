package com.kafka.launcher.config

import com.kafka.launcher.domain.model.AppSort

data class DiscordShortcut(
    val id: String,
    val label: String,
    val uri: String,
    val priority: Int
)

object LauncherConfig {
    const val statsLimit = 50
    const val recommendationFallbackCount = 4
    const val favoritesLimit = 5
    const val recentLimit = 12
    const val appsPerRow = 8
    const val categoryPreviewLimit = 4
    const val bottomQuickActionLimit = 6
    const val homeGridMinHeightDp = 320
    const val navigationModeKey = "navigation_mode"
    const val navigationModeGestureValue = 2
    const val navigationModeThreeButtonValue = 0
    val gestureUnsupportedManufacturers = setOf("xiaomi", "redmi", "poco", "blackshark")
    const val appUsagePrefix = "app:"
    val defaultAppSort = AppSort.USAGE
    const val discordPackageName = "com.discord"
    private const val discordChannelBaseUrl = "https://discord.com/channels"
    val discordShortcuts = listOf(
        DiscordShortcut(
            id = "discord_dm_inbox",
            label = "Discord DM一覧",
            uri = "$discordChannelBaseUrl/@me",
            priority = 3
        ),
        DiscordShortcut(
            id = "discord_testers_faq",
            label = "Discord Testers #faq",
            uri = "$discordChannelBaseUrl/81384788765712384/82027497244617984",
            priority = 1
        )
    )
}
