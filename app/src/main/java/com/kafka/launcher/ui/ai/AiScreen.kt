package com.kafka.launcher.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kafka.launcher.R
import com.kafka.launcher.launcher.AiActionUiModel
import com.kafka.launcher.launcher.AiCenterState
import com.kafka.launcher.launcher.AiSyncStatus
import com.kafka.launcher.ui.components.LauncherIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    state: AiCenterState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onRestore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.ai_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = LauncherIcons.Back),
                            contentDescription = stringResource(id = R.string.drawer_back)
                        )
                    }
                },
                actions = {
                    OutlinedButton(onClick = onRefresh) {
                        Text(text = stringResource(id = R.string.ai_sync_now))
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SyncHeader(state = state)
            }
            item {
                AiSection(
                    title = stringResource(id = R.string.ai_section_candidates),
                    actions = state.candidates,
                    primaryLabel = stringResource(id = R.string.ai_action_accept),
                    secondaryLabel = stringResource(id = R.string.ai_action_hide),
                    onPrimary = onAccept,
                    onSecondary = onDismiss
                )
            }
            item {
                AiSection(
                    title = stringResource(id = R.string.ai_section_adopted),
                    actions = state.adopted,
                    primaryLabel = stringResource(id = R.string.ai_action_hide),
                    onPrimary = onDismiss,
                    onSecondary = null,
                    secondaryLabel = null
                )
            }
            item {
                AiSection(
                    title = stringResource(id = R.string.ai_section_hidden),
                    actions = state.hidden,
                    primaryLabel = stringResource(id = R.string.ai_action_restore),
                    onPrimary = onRestore,
                    onSecondary = null,
                    secondaryLabel = null
                )
            }
        }
    }
}

@Composable
private fun SyncHeader(state: AiCenterState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = lastUpdatedLabel(state),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        when (state.syncStatus) {
            AiSyncStatus.Idle -> {}
            AiSyncStatus.Enqueued -> {
                Text(
                    text = stringResource(id = R.string.ai_sync_status_enqueued),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AiSyncStatus.Running -> {
                Text(
                    text = stringResource(id = R.string.ai_sync_status_running),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AiSyncStatus.UpdatingCatalog -> {
                Text(
                    text = stringResource(id = R.string.ai_sync_status_updating),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AiSyncStatus.Succeeded -> {
                Text(
                    text = stringResource(id = R.string.ai_sync_status_succeeded),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AiSyncStatus.Failed -> {
                Text(
                    text = stringResource(id = R.string.ai_sync_status_failed),
                    style = MaterialTheme.typography.bodySmall
                )
                if (state.lastError.isNotBlank()) {
                    Text(
                        text = state.lastError,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSection(
    title: String,
    actions: List<AiActionUiModel>,
    primaryLabel: String?,
    secondaryLabel: String?,
    onPrimary: ((String) -> Unit)?,
    onSecondary: ((String) -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (actions.isEmpty()) {
            Text(text = stringResource(id = R.string.empty_results), style = MaterialTheme.typography.bodyMedium)
        } else {
            actions.forEach { action ->
                AiActionCard(
                    model = action,
                    primaryLabel = primaryLabel,
                    secondaryLabel = secondaryLabel,
                    onPrimary = onPrimary,
                    onSecondary = onSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AiActionCard(
    model: AiActionUiModel,
    primaryLabel: String?,
    secondaryLabel: String?,
    onPrimary: ((String) -> Unit)?,
    onSecondary: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = model.label, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = model.detail, style = MaterialTheme.typography.bodyMedium)
            if (model.timeWindows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.ai_time_window_label, model.timeWindows.joinToString()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.ai_usage_count, model.usageCount),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (primaryLabel != null && onPrimary != null) {
                    Button(onClick = { onPrimary(model.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = primaryLabel)
                    }
                }
                if (secondaryLabel != null && onSecondary != null) {
                    OutlinedButton(onClick = { onSecondary(model.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = secondaryLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun lastUpdatedLabel(state: AiCenterState): String {
    if (state.lastUpdated.isBlank()) {
        return stringResource(id = R.string.ai_last_updated_never)
    }
    return stringResource(id = R.string.ai_last_updated, state.lastUpdated)
}
