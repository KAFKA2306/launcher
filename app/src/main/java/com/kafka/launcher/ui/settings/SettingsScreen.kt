package com.kafka.launcher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.kafka.launcher.R
import com.kafka.launcher.domain.model.AppSort
import com.kafka.launcher.domain.model.Settings
import com.kafka.launcher.domain.model.NavigationInfo
import com.kafka.launcher.domain.model.NavigationMode
import com.kafka.launcher.launcher.AiPreviewState
import com.kafka.launcher.ui.components.LauncherIcons
import com.kafka.launcher.ui.components.NavigationNotice
import com.kafka.launcher.ui.components.AiRecommendationPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    navigationInfo: NavigationInfo,
    onToggleFavorites: (Boolean) -> Unit,
    onSortSelected: (AppSort) -> Unit,
    onBack: () -> Unit,
    onRequestHomeRole: () -> Unit,
    recommendationTimestamp: String?,
    geminiApiKeyInput: String,
    isGeminiApiKeyConfigured: Boolean,
    onGeminiApiKeyInputChange: (String) -> Unit,
    onSaveGeminiApiKey: () -> Unit,
    onClearGeminiApiKey: () -> Unit,
    aiPreviewState: AiPreviewState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = LauncherIcons.Back), contentDescription = stringResource(id = R.string.drawer_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (navigationInfo.mode == NavigationMode.THREE_BUTTON) {
                NavigationNotice(info = navigationInfo, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(id = R.string.toggle_favorites))
                Switch(checked = settings.showFavorites, onCheckedChange = onToggleFavorites)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(id = R.string.sort_label))
                AppSort.values().forEach { sort ->
                    SortRow(
                        label = stringResource(id = if (sort == AppSort.NAME) R.string.sort_name else R.string.sort_usage),
                        selected = settings.appSort == sort,
                        onClick = { onSortSelected(sort) }
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.request_home_role_description))
                Button(onClick = onRequestHomeRole, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.request_home_role))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(id = R.string.ai_preview_title))
                val geminiText = recommendationTimestamp?.takeIf { it.isNotBlank() }?.let { timestamp ->
                    stringResource(id = R.string.settings_gemini_last_synced, formatGeminiTimestamp(timestamp))
                } ?: stringResource(id = R.string.settings_gemini_never)
                Text(text = geminiText, style = MaterialTheme.typography.bodySmall)
            }
            AiRecommendationPreview(state = aiPreviewState, modifier = Modifier.fillMaxWidth())
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.settings_gemini_api_key_title))
                OutlinedTextField(
                    value = geminiApiKeyInput,
                    onValueChange = onGeminiApiKeyInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(text = stringResource(id = R.string.settings_gemini_api_key_placeholder)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSaveGeminiApiKey,
                        enabled = geminiApiKeyInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.settings_gemini_api_key_save))
                    }
                    OutlinedButton(
                        onClick = onClearGeminiApiKey,
                        enabled = isGeminiApiKeyConfigured,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.settings_gemini_api_key_clear))
                    }
                }
                val statusText = if (isGeminiApiKeyConfigured) {
                    stringResource(id = R.string.settings_gemini_api_key_configured)
                } else {
                    stringResource(id = R.string.settings_gemini_api_key_missing)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatGeminiTimestamp(value: String): String {
    val instant = java.time.Instant.parse(value)
    val zoned = instant.atZone(java.time.ZoneId.systemDefault())
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    return formatter.format(zoned)
}

@Composable
private fun SortRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        RadioButton(selected = selected, onClick = onClick)
    }
}
