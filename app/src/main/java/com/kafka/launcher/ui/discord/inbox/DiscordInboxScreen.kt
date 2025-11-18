package com.kafka.launcher.ui.discord.inbox

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kafka.launcher.domain.model.DiscordNotificationRecord
import com.kafka.launcher.ui.discord.DiscordViewModel
import com.kafka.launcher.ui.discord.components.DiscordNotificationRow
import com.kafka.launcher.ui.discord.components.NotificationAccessCard

@Composable
fun DiscordInboxScreen(
    viewModel: DiscordViewModel,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onJump: (DiscordNotificationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())
    if (!hasNotificationPermission) {
        NotificationAccessCard(
            modifier = modifier.padding(16.dp),
            onRequestPermission = onRequestPermission
        )
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(notifications, key = { it.id }) { record ->
            DiscordNotificationRow(record = record, onJump = { onJump(record) })
        }
    }
}
