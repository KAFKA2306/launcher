package com.kafka.launcher.ui.discord

import android.content.Intent
import android.provider.Settings
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kafka.launcher.R
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.model.ChannelUsageStats
import com.kafka.launcher.domain.model.DiscordChannel
import com.kafka.launcher.domain.model.DiscordPreferences
import com.kafka.launcher.domain.usecase.ParseDiscordChannelKeyUseCase
import com.kafka.launcher.domain.model.DiscordNotificationRecord
import com.kafka.launcher.ui.components.rememberNotificationAccessState
import com.kafka.launcher.ui.discord.components.DiscordSearchBar
import com.kafka.launcher.ui.discord.components.DiscordWebView
import com.kafka.launcher.ui.discord.components.NotificationAccessCard
import com.kafka.launcher.ui.discord.inbox.DiscordInboxScreen

@OptIn(ExperimentalMaterial3Api::class)
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
    val usage by viewModel.usage.collectAsState(initial = emptyList())
    val notificationAccessState by rememberNotificationAccessState()
    var selectedChannelId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentUrl by rememberSaveable { mutableStateOf(initialUrl) }
    var reloadSignal by rememberSaveable { mutableStateOf(0) }
    val parseChannelKey = remember { ParseDiscordChannelKeyUseCase() }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val leftScrollState = rememberScrollState()
    val centerScrollState = rememberScrollState()

    val savedChannels = remember(channels) {
        channels.sortedWith(
            compareByDescending<DiscordChannel> { it.favorite }
                .thenBy { it.serverName }
                .thenBy { it.label }
        )
    }
    val usageMap = remember(usage) { usage.associateBy { it.channelId } }
    val recentChannels = remember(savedChannels, usage) {
        val channelById = savedChannels.associateBy { it.id }
        usage.sortedByDescending { it.lastActiveAt }
            .mapNotNull { stats ->
                val channel = channelById[stats.channelId]
                if (channel != null) DiscordRecentChannelUiState(channel, stats.lastActiveAt) else null
            }
            .distinctBy { it.channel.id }
            .take(5)
    }
    val onboardingSteps = listOf(
        DiscordOnboardingStep(
            label = stringResource(id = R.string.discord_onboarding_step_open),
            completed = currentUrl.isNotBlank()
        ),
        DiscordOnboardingStep(
            label = stringResource(id = R.string.discord_onboarding_step_save),
            completed = channels.isNotEmpty()
        ),
        DiscordOnboardingStep(
            label = stringResource(id = R.string.discord_onboarding_step_notifications),
            completed = notificationAccessState
        )
    )

    var pendingAddChannel by remember { mutableStateOf<DiscordChannel?>(null) }
    var addChannelLabel by rememberSaveable { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<DiscordChannel?>(null) }
    var renameLabel by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<DiscordChannel?>(null) }

    LaunchedEffect(pendingAddChannel) {
        addChannelLabel = pendingAddChannel?.label.orEmpty()
    }
    LaunchedEffect(renameTarget) {
        renameLabel = renameTarget?.label.orEmpty()
    }

    DisposableEffect(lifecycleOwner, currentUrl) {
        var focusStart = System.currentTimeMillis()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> focusStart = System.currentTimeMillis()
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(onBack = onClose)

    val openDiscordApp = remember {
        {
            val intent = context.packageManager.getLaunchIntentForPackage(DiscordConfig.packageName)
            if (intent != null) {
                context.startActivity(intent)
            }
        }
    }
    val requestNotificationPermission = remember {
        {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun showToast(messageRes: Int) {
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    fun handleAddChannel(withDialog: Boolean) {
        val candidate = createChannelFromUrl(parseChannelKey, currentUrl)
        if (candidate == null) {
            showToast(R.string.discord_channel_toast_invalid)
            return
        }
        if (channels.any { it.id == candidate.id }) {
            showToast(R.string.discord_channel_toast_duplicate)
            return
        }
        if (withDialog) {
            pendingAddChannel = candidate
        } else {
            viewModel.addChannel(candidate)
            selectedChannelId = candidate.id
        }
    }

    fun handleClipboardCopy() {
        if (currentUrl.isNotBlank()) {
            clipboardManager.setText(AnnotatedString(currentUrl))
        }
    }

    val selectChannel: (DiscordChannel) -> Unit = { channel ->
        selectedChannelId = channel.id
        currentUrl = channel.url
        viewModel.openChannel(channel)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.discord_screen_title)) },
                navigationIcon = {
                    TextButton(onClick = onClose) {
                        Text(text = "×")
                    }
                },
                actions = {
                    TextButton(onClick = { reloadSignal++ }) {
                        Text(text = stringResource(id = R.string.discord_screen_reload))
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DiscordSetupPane(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight()
                    .verticalScroll(leftScrollState),
                showQuickAccessGuide = !preferences.quickAccessGuideDismissed,
                onDismissQuickAccessGuide = viewModel::dismissQuickAccessGuide,
                onboardingSteps = onboardingSteps,
                hasNotificationPermission = notificationAccessState,
                onRequestNotificationPermission = requestNotificationPermission,
                mutedNames = preferences.mutedNames.map { it.aliases.firstOrNull() ?: it.canonicalName },
                onSaveMutedNames = viewModel::saveMuted
            )
            DiscordChannelHubPane(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .verticalScroll(centerScrollState),
                query = query,
                onQueryChange = viewModel::updateSearch,
                currentUrl = currentUrl,
                onQuickSaveCurrentChannel = { handleAddChannel(false) },
                onEditCurrentChannel = { handleAddChannel(true) },
                onCopyCurrentUrl = { handleClipboardCopy() },
                recentChannels = recentChannels,
                savedChannels = savedChannels,
                usageMap = usageMap,
                selectedChannelId = selectedChannelId,
                onChannelSelected = selectChannel,
                onToggleFavorite = { channel, next -> viewModel.setFavorite(channel.id, next) },
                onRenameChannel = { renameTarget = it },
                onRemoveChannel = { deleteTarget = it },
                onOpenDiscordApp = openDiscordApp,
                viewModel = viewModel,
                hasNotificationPermission = notificationAccessState,
                onRequestPermission = requestNotificationPermission,
                onNotificationJump = { record ->
                    val target = channels.firstOrNull { it.id == record.mappedChannelId }
                    if (target != null) {
                        selectChannel(target)
                    }
                }
            )
            DiscordWebPane(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                url = currentUrl,
                reloadSignal = reloadSignal,
                onUrlChanged = { url ->
                    if (url.isBlank()) return@DiscordWebPane
                    currentUrl = url
                    viewModel.recordOpenUrl(url)
                    val targetId = parseChannelKey(url)?.stableId
                    val match = channels.firstOrNull { it.id == targetId }
                    if (targetId != null && match != null) {
                        selectedChannelId = match.id
                    }
                },
                onPostDetected = viewModel::recordPost
            )
        }
    }

    pendingAddChannel?.let { channel ->
        AlertDialog(
            onDismissRequest = { pendingAddChannel = null },
            title = { Text(text = stringResource(id = R.string.discord_channel_add_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(id = R.string.discord_channel_add_dialog_label))
                    OutlinedTextField(
                        value = addChannelLabel,
                        onValueChange = { addChannelLabel = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val label = addChannelLabel.ifBlank { channel.label }
                    viewModel.addChannel(channel.copy(label = label))
                    selectedChannelId = channel.id
                    pendingAddChannel = null
                }) {
                    Text(text = stringResource(id = R.string.discord_channel_add_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAddChannel = null }) {
                    Text(text = stringResource(id = R.string.discord_channel_add_dialog_cancel))
                }
            }
        )
    }

    renameTarget?.let { channel ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(text = stringResource(id = R.string.discord_channel_rename_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(id = R.string.discord_channel_add_dialog_label))
                    OutlinedTextField(
                        value = renameLabel,
                        onValueChange = { renameLabel = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val label = renameLabel.ifBlank { channel.label }
                    viewModel.renameChannel(channel, label)
                    renameTarget = null
                }) {
                    Text(text = stringResource(id = R.string.discord_channel_rename_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(text = stringResource(id = R.string.discord_channel_rename_dialog_cancel))
                }
            }
        )
    }

    deleteTarget?.let { channel ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(text = stringResource(id = R.string.discord_channel_delete_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.discord_channel_delete_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChannel(channel.id)
                    if (selectedChannelId == channel.id) {
                        selectedChannelId = null
                    }
                    deleteTarget = null
                }) {
                    Text(text = stringResource(id = R.string.discord_channel_delete_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(text = stringResource(id = R.string.discord_channel_delete_dialog_cancel))
                }
            }
        )
    }
}

data class DiscordOnboardingStep(
    val label: String,
    val completed: Boolean
)

data class DiscordRecentChannelUiState(
    val channel: DiscordChannel,
    val lastActiveAt: Long
)

@Composable
private fun DiscordSetupPane(
    showQuickAccessGuide: Boolean,
    onDismissQuickAccessGuide: () -> Unit,
    onboardingSteps: List<DiscordOnboardingStep>,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    mutedNames: List<String>,
    onSaveMutedNames: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showQuickAccessGuide) {
            DiscordQuickAccessGuide(onDismiss = onDismissQuickAccessGuide)
        }
        DiscordOnboardingCard(steps = onboardingSteps)
        NotificationAccessCard(
            onRequestPermission = onRequestNotificationPermission,
            enabled = !hasNotificationPermission,
            modifier = Modifier.fillMaxWidth()
        )
        DiscordMuteCard(
            entries = mutedNames,
            onSave = onSaveMutedNames,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DiscordOnboardingCard(steps: List<DiscordOnboardingStep>, modifier: Modifier = Modifier) {
    DiscordSectionCard(modifier = modifier) {
        Text(text = stringResource(id = R.string.discord_onboarding_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(id = R.string.discord_onboarding_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            steps.forEach { step ->
                val statusText = if (step.completed) {
                    stringResource(id = R.string.discord_onboarding_step_done)
                } else {
                    stringResource(id = R.string.discord_onboarding_step_todo)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = step.label)
                    Text(text = statusText, color = if (step.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DiscordMuteCard(
    entries: List<String>,
    onSave: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable(entries) { mutableStateOf(entries.joinToString(",")) }
    DiscordSectionCard(modifier = modifier) {
        Text(text = stringResource(id = R.string.discord_muted_users_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(id = R.string.discord_muted_users_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(text = stringResource(id = R.string.discord_muted_users_placeholder)) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val items = text.split(',').mapNotNull { item ->
                    val value = item.trim()
                    if (value.isBlank()) null else value
                }
                onSave(items)
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = stringResource(id = R.string.discord_muted_users_save))
        }
    }
}

@Composable
private fun DiscordChannelHubPane(
    query: String,
    onQueryChange: (String) -> Unit,
    currentUrl: String,
    onQuickSaveCurrentChannel: () -> Unit,
    onEditCurrentChannel: () -> Unit,
    onCopyCurrentUrl: () -> Unit,
    recentChannels: List<DiscordRecentChannelUiState>,
    savedChannels: List<DiscordChannel>,
    usageMap: Map<String, ChannelUsageStats>,
    selectedChannelId: String?,
    onChannelSelected: (DiscordChannel) -> Unit,
    onToggleFavorite: (DiscordChannel, Boolean) -> Unit,
    onRenameChannel: (DiscordChannel) -> Unit,
    onRemoveChannel: (DiscordChannel) -> Unit,
    onOpenDiscordApp: () -> Unit,
    viewModel: DiscordViewModel,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNotificationJump: (DiscordNotificationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DiscordCurrentChannelCard(
            currentUrl = currentUrl,
            onQuickSave = onQuickSaveCurrentChannel,
            onEditSave = onEditCurrentChannel,
            onCopyUrl = onCopyCurrentUrl,
            onOpenDiscordApp = onOpenDiscordApp
        )
        DiscordRecentChannelsCard(
            channels = recentChannels,
            onChannelSelected = onChannelSelected
        )
        DiscordSavedChannelsCard(
            query = query,
            onQueryChange = onQueryChange,
            channels = savedChannels,
            usageMap = usageMap,
            selectedChannelId = selectedChannelId,
            onChannelSelected = onChannelSelected,
            onToggleFavorite = onToggleFavorite,
            onRenameChannel = onRenameChannel,
            onRemoveChannel = onRemoveChannel,
            onOpenDiscordApp = onOpenDiscordApp
        )
        DiscordNotificationCard(
            viewModel = viewModel,
            hasPermission = hasNotificationPermission,
            onRequestPermission = onRequestPermission,
            onNotificationJump = onNotificationJump
        )
    }
}

@Composable
private fun DiscordCurrentChannelCard(
    currentUrl: String,
    onQuickSave: () -> Unit,
    onEditSave: () -> Unit,
    onCopyUrl: () -> Unit,
    onOpenDiscordApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    DiscordSectionCard(modifier = modifier) {
        Text(text = stringResource(id = R.string.discord_channel_current_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(id = R.string.discord_channel_current_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = currentUrl.ifBlank { "https://discord.com/channels/..." },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onQuickSave, shape = RoundedCornerShape(12.dp)) {
                Text(text = stringResource(id = R.string.discord_channel_save_button))
            }
            TextButton(onClick = onEditSave) {
                Text(text = stringResource(id = R.string.discord_channel_name_edit))
            }
            TextButton(onClick = onCopyUrl) {
                Text(text = stringResource(id = R.string.discord_channel_url_copy))
            }
        }
        OutlinedButton(onClick = onOpenDiscordApp, shape = RoundedCornerShape(12.dp)) {
            Text(text = stringResource(id = R.string.discord_empty_state_open_app))
        }
    }
}

@Composable
private fun DiscordRecentChannelsCard(
    channels: List<DiscordRecentChannelUiState>,
    onChannelSelected: (DiscordChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    DiscordSectionCard(modifier = modifier) {
        Text(text = stringResource(id = R.string.discord_recent_channels_title), style = MaterialTheme.typography.titleMedium)
        if (channels.isEmpty()) {
            Text(
                text = stringResource(id = R.string.discord_recent_channels_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                channels.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChannelSelected(item.channel) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = item.channel.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = item.channel.serverName.ifBlank { item.channel.key.guildId },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(
                                    id = R.string.discord_channel_last_active,
                                    formatRelativeTime(item.lastActiveAt)
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscordSavedChannelsCard(
    query: String,
    onQueryChange: (String) -> Unit,
    channels: List<DiscordChannel>,
    usageMap: Map<String, ChannelUsageStats>,
    selectedChannelId: String?,
    onChannelSelected: (DiscordChannel) -> Unit,
    onToggleFavorite: (DiscordChannel, Boolean) -> Unit,
    onRenameChannel: (DiscordChannel) -> Unit,
    onRemoveChannel: (DiscordChannel) -> Unit,
    onOpenDiscordApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    DiscordSectionCard(modifier = modifier) {
        Text(text = stringResource(id = R.string.discord_saved_channels_title), style = MaterialTheme.typography.titleMedium)
        DiscordSearchBar(value = query, onValueChange = onQueryChange)
        val filtered = remember(query, channels) {
            if (query.isBlank()) channels else channels.filter { channel ->
                channel.label.contains(query, ignoreCase = true) || channel.serverName.contains(query, ignoreCase = true)
            }
        }
        if (filtered.isEmpty()) {
            Text(
                text = stringResource(id = R.string.discord_empty_state_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onOpenDiscordApp) {
                Text(text = stringResource(id = R.string.discord_empty_state_open_app))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.forEach { channel ->
                    val lastActive = usageMap[channel.id]?.lastActiveAt ?: 0L
                    DiscordSavedChannelRow(
                        channel = channel,
                        isSelected = channel.id == selectedChannelId,
                        lastActiveLabel = stringResource(
                            id = R.string.discord_channel_last_active,
                            formatRelativeTime(lastActive)
                        ),
                        onChannelSelected = { onChannelSelected(channel) },
                        onToggleFavorite = { onToggleFavorite(channel, !channel.favorite) },
                        onRename = { onRenameChannel(channel) },
                        onRemove = { onRemoveChannel(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscordSavedChannelRow(
    channel: DiscordChannel,
    isSelected: Boolean,
    lastActiveLabel: String,
    onChannelSelected: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        color = background,
        onClick = onChannelSelected
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = channel.label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = channel.serverName.ifBlank { channel.key.guildId },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = lastActiveLabel, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onToggleFavorite) {
                    Text(text = if (channel.favorite) {
                        stringResource(id = R.string.discord_channel_actions_unfavorite)
                    } else {
                        stringResource(id = R.string.discord_channel_actions_favorite)
                    })
                }
                TextButton(onClick = onRename) {
                    Text(text = stringResource(id = R.string.discord_channel_actions_rename))
                }
                TextButton(onClick = onRemove) {
                    Text(text = stringResource(id = R.string.discord_channel_actions_remove))
                }
            }
        }
    }
}

@Composable
private fun DiscordNotificationCard(
    viewModel: DiscordViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNotificationJump: (DiscordNotificationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    DiscordSectionCard(modifier = modifier) {
        Text(text = stringResource(id = R.string.discord_notifications_title), style = MaterialTheme.typography.titleMedium)
        DiscordInboxScreen(
            viewModel = viewModel,
            hasNotificationPermission = hasPermission,
            onRequestPermission = onRequestPermission,
            onJump = onNotificationJump,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 320.dp),
            showAccessFallback = false
        )
        if (!hasPermission) {
            Text(
                text = stringResource(id = R.string.discord_notifications_permission_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    modifier: Modifier = Modifier
) {
    DiscordSectionCard(modifier = modifier) {
        Text(text = DiscordConfig.webModuleLabel, style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp)
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
    DiscordSectionCard(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.discord_quick_access_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(id = R.string.discord_quick_access_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
            Text(text = stringResource(id = R.string.discord_quick_access_dismiss))
        }
    }
}

@Composable
private fun DiscordSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F2FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun formatRelativeTime(timestamp: Long): String {
    val context = LocalContext.current
    return if (timestamp <= 0) {
        stringResource(id = R.string.discord_recent_inactive)
    } else {
        DateUtils.getRelativeTimeSpanString(
            timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
}

private fun createChannelFromUrl(
    parseDiscordChannelKeyUseCase: ParseDiscordChannelKeyUseCase,
    url: String
): DiscordChannel? {
    val key = parseDiscordChannelKeyUseCase(url) ?: return null
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
