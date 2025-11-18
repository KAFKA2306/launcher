package com.kafka.launcher.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.model.DiscordPreferences
import com.kafka.launcher.domain.model.DiscordRankingWeights
import com.kafka.launcher.ui.discord.DiscordViewModel
import com.kafka.launcher.R

@Composable
fun DiscordSettingsSection(
    viewModel: DiscordViewModel,
    notificationPermissionGranted: Boolean,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferencesState = viewModel.preferences.collectAsState(initial = DiscordPreferences())
    val preferences = preferencesState.value ?: return
    var mutedInput by rememberSaveable(preferences.mutedNames) {
        mutableStateOf(preferences.mutedNames.joinToString(",") { it.aliases.firstOrNull() ?: it.canonicalName })
    }
    var selfInput by rememberSaveable(preferences.selfNames) {
        mutableStateOf(preferences.selfNames.joinToString(",") { it.aliases.firstOrNull() ?: it.canonicalName })
    }
    val weights = preferences.rankingWeights
    var openWeight by rememberSaveable(weights) { mutableStateOf(weights.open.toString()) }
    var focusWeight by rememberSaveable(weights) { mutableStateOf(weights.focus.toString()) }
    var postWeight by rememberSaveable(weights) { mutableStateOf(weights.post.toString()) }
    var notifWeight by rememberSaveable(weights) { mutableStateOf(weights.notification.toString()) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(id = R.string.discord_settings_title),
            style = MaterialTheme.typography.titleMedium
        )
        DiscordNotificationAccessRow(
            granted = notificationPermissionGranted,
            onOpenSettings = onOpenNotificationSettings
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(id = R.string.discord_settings_muted_label))
            OutlinedTextField(
                value = mutedInput,
                onValueChange = { mutedInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(id = R.string.discord_settings_muted_placeholder)) }
            )
            Text(
                text = stringResource(id = R.string.discord_settings_muted_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = {
                val items = mutedInput.split(',')
                    .mapNotNull { it.trim().ifBlank { null } }
                viewModel.saveMuted(items)
            }) {
                Text(text = stringResource(id = R.string.discord_settings_save_muted))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(id = R.string.discord_settings_self_label))
            OutlinedTextField(
                value = selfInput,
                onValueChange = { selfInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(id = R.string.discord_settings_self_placeholder)) }
            )
            Text(
                text = stringResource(id = R.string.discord_settings_self_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = {
                val items = selfInput.split(',')
                    .mapNotNull { it.trim().ifBlank { null } }
                viewModel.saveSelf(items)
            }) {
                Text(text = stringResource(id = R.string.discord_settings_save_self))
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = stringResource(id = R.string.discord_settings_weights_title))
                Text(
                    text = stringResource(id = R.string.discord_settings_weights_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = showAdvanced, onCheckedChange = { showAdvanced = it })
        }
        AnimatedVisibility(visible = showAdvanced) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = openWeight,
                        onValueChange = { openWeight = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = stringResource(id = R.string.discord_settings_weight_open)) }
                    )
                    OutlinedTextField(
                        value = focusWeight,
                        onValueChange = { focusWeight = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = stringResource(id = R.string.discord_settings_weight_focus)) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = postWeight,
                        onValueChange = { postWeight = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = stringResource(id = R.string.discord_settings_weight_post)) }
                    )
                    OutlinedTextField(
                        value = notifWeight,
                        onValueChange = { notifWeight = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = stringResource(id = R.string.discord_settings_weight_notification)) }
                    )
                }
                Button(onClick = {
                    val weightsValue = DiscordRankingWeights(
                        open = openWeight.toIntOrNull() ?: DiscordConfig.defaultRankingWeights.open,
                        focus = focusWeight.toIntOrNull() ?: DiscordConfig.defaultRankingWeights.focus,
                        post = postWeight.toIntOrNull() ?: DiscordConfig.defaultRankingWeights.post,
                        notification = notifWeight.toIntOrNull() ?: DiscordConfig.defaultRankingWeights.notification
                    )
                    viewModel.saveWeights(weightsValue)
                }) {
                    Text(text = stringResource(id = R.string.discord_settings_save_weights))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = viewModel::dismissQuickAccessGuide, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.discord_settings_hide_quick_access))
        }
    }
}

@Composable
private fun DiscordNotificationAccessRow(
    granted: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(id = R.string.discord_settings_notification_title))
        Text(
            text = if (granted) {
                stringResource(id = R.string.discord_settings_notification_granted)
            } else {
                stringResource(id = R.string.discord_settings_notification_required)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onOpenSettings, enabled = !granted) {
            Text(text = stringResource(id = R.string.discord_settings_open_notification_settings))
        }
    }
}
