package com.kafka.launcher.config

import com.kafka.launcher.domain.model.AppSort

data class DiscordShortcut(
    val id: String,
    val label: String,
    val uri: String,
    val priority: Int
)

object LauncherConfig {
    const val logDirectoryName = "logs"
    const val logManifestFileName = "logs_manifest.json"
    const val logPackageFileName = "logs_bundle.zip"
    const val quickActionSnapshotFileName = "quickactions_snapshot.txt"
    const val quickActionEventsFileName = "quickactions_events.txt"
    const val actionLogEventsFileName = "action_events.jsonl"
    const val actionLogRecentFileName = "action_recent.json"
    const val actionLogStatsFileName = "action_stats.json"
    const val statsLimit = 50
    const val favoritesLimit = 5
    const val recentLimit = 12
    const val appsPerRow = 8
    const val categoryPreviewLimit = 4
    const val bottomQuickActionLimit = 6
    const val homeGridMinHeightDp = 320
    const val homeBackgroundColor: Long = 0xFF0D1117
    const val cardBackgroundColor: Long = 0xFF1E242C
    const val surfaceLowColor: Long = 0xFF161B22
    const val surfaceBorderColor: Long = 0xFF232A33
    const val sectionTitleColor: Long = 0xFFE6EAF2
    const val sectionTitleVariantColor: Long = 0xFF9DA7B8
    const val primaryButtonColor: Long = 0xFF6C4DFF
    const val primaryButtonContentColor: Long = 0xFFFFFFFF
    const val sectionCardCornerRadiusDp = 12
    const val sectionCardElevationDp = 2
    const val sectionCardPaddingHorizontalDp = 12
    const val sectionCardPaddingVerticalDp = 10
    const val sectionSpacingTopDp = 8
    const val sectionSpacingBottomDp = 8
    const val sectionVerticalSpacingDp = 8
    const val appGridLabelFontSizeSp = 13
    const val appGridLabelLineHeightSp = 18
    const val appGridLabelWidthDp = 68
    const val appGridLabelMaxLines = 2
    const val appGridSpacingDp = 8
    const val sectionTitleLineHeightSp = 20
    const val primaryButtonElevationDefaultDp = 3
    const val primaryButtonElevationPressedDp = 1
    const val appTileBorderWidthDp = 1
    const val homeContentHorizontalPaddingDp = 8
    const val homeContentVerticalPaddingDp = 8
    const val navigationModeKey = "navigation_mode"
    const val navigationModeGestureValue = 2
    const val navigationModeThreeButtonValue = 0
    const val pinnedAppsKey = "pinned_apps"
    val gestureUnsupportedManufacturers = setOf("xiaomi")
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
