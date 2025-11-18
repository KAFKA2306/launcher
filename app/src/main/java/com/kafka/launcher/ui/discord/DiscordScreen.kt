package com.kafka.launcher.ui.discord

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kafka.launcher.R
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.model.DiscordChannel
import com.kafka.launcher.domain.model.DiscordPreferences
import com.kafka.launcher.domain.usecase.ParseDiscordChannelKeyUseCase
import com.kafka.launcher.ui.components.rememberNotificationAccessState
import com.kafka.launcher.ui.discord.components.DiscordChannelList
import com.kafka.launcher.ui.discord.components.DiscordSearchBar
import com.kafka.launcher.ui.discord.components.DiscordWebView
import com.kafka.launcher.ui.discord.inbox.DiscordInboxScreen

@Composable
fun DiscordScreen(
    viewModel: DiscordViewModel,
    initialUrl: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val channels by viewModel.channels.collectAsState(initial = emptyList())
    val preferences by viewModel.preferences.collectAsState(initial = DiscordPreferences())
    val notificationAccessState by rememberNotificationAccessState()
    var selectedChannelId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentUrl by rememberSaveable { mutableStateOf(initialUrl) }
    var reloadSignal by rememberSaveable { mutableStateOf(0) }
    val parseChannelKey = remember { ParseDiscordChannelKeyUseCase() }
    val context = LocalContext.current

    val filteredChannels = remember(query, channels) {
        if (query.isBlank()) {
            channels
        } else {
            channels.filter { it.label.contains(query, ignoreCase = true) || it.serverName.contains(query, ignoreCase = true) }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentUrl) {
        var focusStart = System.currentTimeMillis()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    focusStart = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    val durationMs = System.currentTimeMillis() - focusStart
                    if (durationMs > 1000 && currentUrl.isNotBlank()) {
                        viewModel.recordFocus(currentUrl, durationMs / 1000)
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.discord_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(id = R.string.discord_screen_close))
                    }
                },
                actions = {
                    IconButton(onClick = { reloadSignal++ }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(id = R.string.discord_screen_reload))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!preferences.quickAccessGuideDismissed) {
                DiscordQuickAccessGuide(
                    onDismiss = viewModel::dismissQuickAccessGuide
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(0.38f)) {
                    DiscordChannelPane(
                        query = query,
                        onQueryChange = viewModel::updateSearch,
                        channels = filteredChannels,
                        selectedChannelId = selectedChannelId,
                        onChannelSelected = { channel ->
                            selectedChannelId = channel.id
                            currentUrl = channel.url
                            viewModel.openChannel(channel)
                        },
                        onOpenDiscordApp = {
                            val intent = context.packageManager.getLaunchIntentForPackage(DiscordConfig.packageName)
                            intent?.let { context.startActivity(it) }
                        }
                    )
                }
                Box(modifier = Modifier.weight(0.62f)) {
                    DiscordWebPane(
                        url = currentUrl,
                        reloadSignal = reloadSignal,
                        onUrlChanged = { url ->
                            if (url.isBlank()) return@DiscordWebPane
                            currentUrl = url
                            viewModel.recordOpenUrl(url)
                            val targetId = parseChannelKey(url)?.stableId
                            if (targetId != null) {
                                val match = channels.firstOrNull { it.id == targetId }
                                if (match != null) {
                                    selectedChannelId = match.id
                                }
                            }
                        },
                        onPostDetected = viewModel::recordPost,
                        onAddCurrentChannel = {
                            val channel = createChannelFromUrl(parseChannelKey, channels, currentUrl)
                            channel?.let { viewModel.addChannel(it) }
                        }
                    )
                }
            }
            DiscordInboxScreen(
                viewModel = viewModel,
                hasNotificationPermission = notificationAccessState,
                onRequestPermission = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                onJump = { record ->
                    val targetId = record.mappedChannelId
                    val target = channels.firstOrNull { it.id == targetId }
                    if (target != null) {
                        selectedChannelId = target.id
                        currentUrl = target.url
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
private fun DiscordChannelPane(
    query: String,
    onQueryChange: (String) -> Unit,
    channels: List<DiscordChannel>,
    selectedChannelId: String?,
    onChannelSelected: (DiscordChannel) -> Unit,
    onOpenDiscordApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DiscordSearchBar(value = query, onValueChange = onQueryChange)
        if (channels.isEmpty()) {
            DiscordEmptyState(onOpenDiscordApp = onOpenDiscordApp)
        } else {
            DiscordChannelList(
                channels = channels,
                selectedChannelId = selectedChannelId,
                onChannelClick = onChannelSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DiscordWebPane(
    url: String,
    reloadSignal: Int,
    onUrlChanged: (String) -> Unit,
    onPostDetected: (String) -> Unit,
    onAddCurrentChannel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(onClick = onAddCurrentChannel) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(id = R.string.discord_screen_add_channel))
            }
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
            DiscordWebView(
                url = url,
                reloadSignal = reloadSignal,
                onUrlChanged = onUrlChanged,
                onPostDetected = onPostDetected
            )
        }
    }
}

@Composable
private fun DiscordQuickAccessGuide(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.discord_quick_access_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(id = R.string.discord_quick_access_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onDismiss) {
            Text(text = stringResource(id = R.string.discord_quick_access_dismiss))
        }
    }
}

@Composable
private fun DiscordEmptyState(
    onOpenDiscordApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.discord_empty_state_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(id = R.string.discord_empty_state_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onOpenDiscordApp) {
            Text(text = stringResource(id = R.string.discord_empty_state_open_app))
        }
    }
}

private fun createChannelFromUrl(
    parseDiscordChannelKeyUseCase: ParseDiscordChannelKeyUseCase,
    existingChannels: List<DiscordChannel>,
    url: String
): DiscordChannel? {
    val key = parseDiscordChannelKeyUseCase(url) ?: return null
    if (existingChannels.any { it.id == key.stableId }) return null
    val normalizedUrl = "${DiscordConfig.baseUrl}/${key.path}"
    val label = if (key.threadId.isNullOrBlank()) {
        "#${key.channelId}"
    } else {
        "#${key.channelId}/${key.threadId}"
    }
    val serverName = if (key.guildId == "@me") "DM" else key.guildId
    return DiscordChannel(
        key = key,
        url = normalizedUrl,
        label = label,
        serverName = serverName
    )
}
