package com.kafka.launcher.config

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
    const val discordPackageName = "com.discord"
    private const val discordChannelBaseUrl = "https://discord.com/channels"
    const val googleSearchPackageName = "com.google.android.googlequicksearchbox"
    const val googleSearchUrl = "https://www.google.com/search?q="
    const val googleImageSearchUrl = "https://www.google.com/search?tbm=isch&q="
    const val googleNewsSearchUrl = "https://news.google.com/search?q="
    const val googleDrivePackageName = "com.google.android.apps.docs"
    const val googleDriveStarredUrl = "https://drive.google.com/drive/starred"
    const val googleDriveSharedUrl = "https://drive.google.com/drive/shared-with-me"
    const val googleDocsCreateUrl = "https://docs.google.com/document/create"
    const val googleSheetsCreateUrl = "https://sheets.google.com/create"
    const val googleSlidesCreateUrl = "https://slides.google.com/create"
    const val googleMeetPackageName = "com.google.android.apps.tachyon"
    const val googleMeetNewMeetingUrl = "https://meet.google.com/new"
    const val googleKeepPackageName = "com.google.android.keep"
    const val googleKeepNewNoteUrl = "https://keep.google.com/create"
    const val googlePhotosPackageName = "com.google.android.apps.photos"
    const val youtubePackageName = "com.google.android.youtube"
    const val youtubeSearchUrl = "https://www.youtube.com/results?search_query="
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
