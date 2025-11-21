package com.kafka.launcher.ui.discord.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kafka.launcher.R
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
    modifier: Modifier = Modifier,
    showAccessFallback: Boolean = true
) {
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())
    if (!hasNotificationPermission) {
        if (showAccessFallback) {
            NotificationAccessCard(
                modifier = modifier.padding(16.dp),
                onRequestPermission = onRequestPermission
            )
        } else {
            DiscordInboxPlaceholder(
                modifier = modifier,
                onRequestPermission = onRequestPermission
            )
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(notifications, key = { it.id }) { record ->
            DiscordNotificationRow(record = record, onJump = { onJump(record) })
        }
    }
}

@Composable
private fun DiscordInboxPlaceholder(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(id = R.string.discord_notification_permission_title))
        Text(
            text = stringResource(id = R.string.discord_notifications_permission_placeholder),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onRequestPermission) {
            Text(text = stringResource(id = R.string.discord_notification_permission_button))
        }
    }
}
