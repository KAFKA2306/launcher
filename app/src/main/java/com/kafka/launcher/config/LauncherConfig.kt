package com.kafka.launcher.config

import com.kafka.launcher.domain.model.ActionType

data class DiscordShortcut(
    val id: String,
    val label: String,
    val uri: String,
    val priority: Int
)

data class QuickActionPreset(
    val id: String,
    val label: String,
    val actionType: ActionType,
    val data: String? = null,
    val packageName: String? = null,
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
    val googleQuickActions = listOf(
        QuickActionPreset(
            id = "google_search_query",
            label = "Google検索",
            actionType = ActionType.WEB_SEARCH,
            data = googleSearchUrl,
            packageName = googleSearchPackageName,
            priority = 6
        ),
        QuickActionPreset(
            id = "google_search_image",
            label = "画像検索",
            actionType = ActionType.WEB_SEARCH,
            data = googleImageSearchUrl,
            packageName = googleSearchPackageName,
            priority = 5
        ),
        QuickActionPreset(
            id = "google_search_news",
            label = "ニュース検索",
            actionType = ActionType.WEB_SEARCH,
            data = googleNewsSearchUrl,
            packageName = googleSearchPackageName,
            priority = 4
        ),
        QuickActionPreset(
            id = "google_drive_open",
            label = "Googleドライブ",
            actionType = ActionType.OPEN_APP,
            packageName = googleDrivePackageName,
            priority = 6
        ),
        QuickActionPreset(
            id = "google_drive_starred",
            label = "スター付き",
            actionType = ActionType.BROWSER_URL,
            data = googleDriveStarredUrl,
            packageName = googleDrivePackageName,
            priority = 5
        ),
        QuickActionPreset(
            id = "google_drive_shared",
            label = "共有アイテム",
            actionType = ActionType.BROWSER_URL,
            data = googleDriveSharedUrl,
            packageName = googleDrivePackageName,
            priority = 4
        ),
        QuickActionPreset(
            id = "google_docs_create",
            label = "ドキュメント作成",
            actionType = ActionType.BROWSER_URL,
            data = googleDocsCreateUrl,
            priority = 3
        ),
        QuickActionPreset(
            id = "google_sheets_create",
            label = "スプレッドシート作成",
            actionType = ActionType.BROWSER_URL,
            data = googleSheetsCreateUrl,
            priority = 3
        ),
        QuickActionPreset(
            id = "google_slides_create",
            label = "スライド作成",
            actionType = ActionType.BROWSER_URL,
            data = googleSlidesCreateUrl,
            priority = 3
        ),
        QuickActionPreset(
            id = "google_meet_open",
            label = "Google Meet",
            actionType = ActionType.OPEN_APP,
            packageName = googleMeetPackageName,
            priority = 5
        ),
        QuickActionPreset(
            id = "google_meet_new",
            label = "新しい会議",
            actionType = ActionType.BROWSER_URL,
            data = googleMeetNewMeetingUrl,
            packageName = googleMeetPackageName,
            priority = 4
        ),
        QuickActionPreset(
            id = "google_keep_open",
            label = "Google Keep",
            actionType = ActionType.OPEN_APP,
            packageName = googleKeepPackageName,
            priority = 4
        ),
        QuickActionPreset(
            id = "google_keep_new_note",
            label = "メモを追加",
            actionType = ActionType.BROWSER_URL,
            data = googleKeepNewNoteUrl,
            packageName = googleKeepPackageName,
            priority = 3
        ),
        QuickActionPreset(
            id = "google_photos_open",
            label = "Googleフォト",
            actionType = ActionType.OPEN_APP,
            packageName = googlePhotosPackageName,
            priority = 3
        ),
        QuickActionPreset(
            id = "youtube_open",
            label = "YouTube",
            actionType = ActionType.OPEN_APP,
            packageName = youtubePackageName,
            priority = 5
        ),
        QuickActionPreset(
            id = "youtube_search",
            label = "YouTube検索",
            actionType = ActionType.WEB_SEARCH,
            data = youtubeSearchUrl,
            packageName = youtubePackageName,
            priority = 4
        )
    )
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
