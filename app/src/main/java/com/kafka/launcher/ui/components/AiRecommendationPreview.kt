package com.kafka.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kafka.launcher.R
import com.kafka.launcher.launcher.AiPreviewState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AiRecommendationPreview(state: AiPreviewState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(id = R.string.ai_preview_title), style = MaterialTheme.typography.titleMedium)
            if (state.generatedAt.isNotBlank()) {
                Text(
                    text = stringResource(id = R.string.ai_preview_updated_at, formatTimestamp(state.generatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(text = stringResource(id = R.string.ai_preview_empty), style = MaterialTheme.typography.bodySmall)
            }
            state.windows.forEach { window ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = window.id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (window.primary.isNotEmpty()) {
                        Text(text = stringResource(id = R.string.ai_preview_primary_label, window.primary.joinToString(", ")))
                    }
                    if (window.fallback.isNotEmpty()) {
                        Text(text = stringResource(id = R.string.ai_preview_fallback_label, window.fallback.joinToString(", ")))
                    }
                }
            }
            if (state.rationales.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(id = R.string.ai_preview_rationale_title), style = MaterialTheme.typography.titleSmall)
                state.rationales.forEach { rationale ->
                    Text(text = rationale.target, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = rationale.summary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatTimestamp(value: String): String {
    val instant = Instant.parse(value)
    val zoned = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    return formatter.format(zoned)
}
