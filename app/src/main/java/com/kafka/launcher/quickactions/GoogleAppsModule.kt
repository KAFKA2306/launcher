package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class GoogleAppsModule : QuickActionProvider {
    override val id: String = "google_apps"

    override fun actions(context: Context): List<QuickAction> = listOf(
        QuickAction(
            id = "google_search_query",
            providerId = id,
            label = "Google検索",
            actionType = ActionType.WEB_SEARCH,
            data = LauncherConfig.googleSearchUrl,
            packageName = LauncherConfig.googleSearchPackageName,
            priority = 6
        ),
        QuickAction(
            id = "google_search_image",
            providerId = id,
            label = "画像検索",
            actionType = ActionType.WEB_SEARCH,
            data = LauncherConfig.googleImageSearchUrl,
            packageName = LauncherConfig.googleSearchPackageName,
            priority = 5
        ),
        QuickAction(
            id = "google_search_news",
            providerId = id,
            label = "ニュース検索",
            actionType = ActionType.WEB_SEARCH,
            data = LauncherConfig.googleNewsSearchUrl,
            packageName = LauncherConfig.googleSearchPackageName,
            priority = 4
        ),
        QuickAction(
            id = "google_drive_open",
            providerId = id,
            label = "Googleドライブ",
            actionType = ActionType.OPEN_APP,
            packageName = LauncherConfig.googleDrivePackageName,
            priority = 6
        ),
        QuickAction(
            id = "google_drive_starred",
            providerId = id,
            label = "スター付き",
            actionType = ActionType.BROWSER_URL,
            data = LauncherConfig.googleDriveStarredUrl,
            packageName = LauncherConfig.googleDrivePackageName,
            priority = 5
        ),
        QuickAction(
            id = "google_drive_shared",
            providerId = id,
            label = "共有アイテム",
            actionType = ActionType.BROWSER_URL,
            data = LauncherConfig.googleDriveSharedUrl,
            packageName = LauncherConfig.googleDrivePackageName,
            priority = 4
        ),
        QuickAction(
            id = "google_docs_create",
            providerId = id,
            label = "ドキュメント作成",
            actionType = ActionType.BROWSER_URL,
            data = LauncherConfig.googleDocsCreateUrl,
            priority = 3
        ),
        QuickAction(
            id = "google_sheets_create",
            providerId = id,
            label = "スプレッドシート作成",
            actionType = ActionType.BROWSER_URL,
            data = LauncherConfig.googleSheetsCreateUrl,
            priority = 3
        ),
        QuickAction(
            id = "google_slides_create",
            providerId = id,
            label = "スライド作成",
            actionType = ActionType.BROWSER_URL,
            data = LauncherConfig.googleSlidesCreateUrl,
            priority = 3
        ),
        QuickAction(
            id = "google_meet_open",
            providerId = id,
            label = "Google Meet",
            actionType = ActionType.OPEN_APP,
            packageName = LauncherConfig.googleMeetPackageName,
            priority = 5
        ),
        QuickAction(
            id = "google_meet_new",
            providerId = id,
            label = "新しい会議",
            actionType = ActionType.BROWSER_URL,
            data = LauncherConfig.googleMeetNewMeetingUrl,
            packageName = LauncherConfig.googleMeetPackageName,
            priority = 4
        ),
        QuickAction(
            id = "google_keep_open",
            providerId = id,
            label = "Google Keep",
            actionType = ActionType.OPEN_APP,
            packageName = LauncherConfig.googleKeepPackageName,
            priority = 4
        ),
        QuickAction(
            id = "google_keep_new_note",
            providerId = id,
            label = "メモを追加",
            actionType = ActionType.BROWSER_URL,
            data = LauncherConfig.googleKeepNewNoteUrl,
            packageName = LauncherConfig.googleKeepPackageName,
            priority = 3
        ),
        QuickAction(
            id = "google_photos_open",
            providerId = id,
            label = "Googleフォト",
            actionType = ActionType.OPEN_APP,
            packageName = LauncherConfig.googlePhotosPackageName,
            priority = 3
        ),
        QuickAction(
            id = "youtube_open",
            providerId = id,
            label = "YouTube",
            actionType = ActionType.OPEN_APP,
            packageName = LauncherConfig.youtubePackageName,
            priority = 5
        ),
        QuickAction(
            id = "youtube_search",
            providerId = id,
            label = "YouTube検索",
            actionType = ActionType.WEB_SEARCH,
            data = LauncherConfig.youtubeSearchUrl,
            packageName = LauncherConfig.youtubePackageName,
            priority = 4
        )
    )
}
