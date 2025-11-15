package com.kafka.launcher.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.kafka.launcher.domain.model.ActionType

fun actionIcon(type: ActionType): ImageVector = when (type) {
    ActionType.OPEN_APP -> Icons.Outlined.Apps
    ActionType.WEB_SEARCH -> Icons.Outlined.Search
    ActionType.MAP_VIEW -> Icons.Outlined.Map
    ActionType.MAP_NAVIGATION -> Icons.Outlined.Navigation
    ActionType.CALENDAR_VIEW -> Icons.Outlined.Event
    ActionType.CALENDAR_INSERT -> Icons.Outlined.Add
    ActionType.EMAIL_INBOX -> Icons.Outlined.Email
    ActionType.EMAIL_COMPOSE -> Icons.Outlined.Email
    ActionType.DISCORD_OPEN -> Icons.Outlined.Chat
    ActionType.BROWSER_URL -> Icons.Outlined.Language
}
