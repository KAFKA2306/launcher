package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class GoogleCalendarModule : QuickActionProvider {
    override val id: String = "google_calendar"

    override fun actions(context: Context): List<QuickAction> = listOf(
        QuickAction(
            id = "google_calendar_today",
            providerId = id,
            label = "今日の予定",
            actionType = ActionType.CALENDAR_VIEW,
            packageName = "com.google.android.calendar",
            priority = 3
        ),
        QuickAction(
            id = "google_calendar_new",
            providerId = id,
            label = "イベント作成",
            actionType = ActionType.CALENDAR_INSERT,
            packageName = "com.google.android.calendar",
            priority = 2
        )
    )
}
