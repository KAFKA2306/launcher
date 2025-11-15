package com.kafka.launcher.domain.model

data class QuickAction(
    val id: String,
    val providerId: String,
    val label: String,
    val actionType: ActionType,
    val data: String? = null,
    val packageName: String? = null,
    val priority: Int = 0
)

enum class ActionType {
    OPEN_APP,
    WEB_SEARCH,
    MAP_VIEW,
    MAP_NAVIGATION,
    CALENDAR_VIEW,
    CALENDAR_INSERT,
    EMAIL_INBOX,
    EMAIL_COMPOSE,
    DISCORD_OPEN,
    BROWSER_URL
}

data class UserQuickActionConfig(
    val id: String,
    val label: String,
    val actionType: ActionType,
    val data: String? = null
)
