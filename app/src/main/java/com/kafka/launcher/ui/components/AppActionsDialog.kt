package com.kafka.launcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kafka.launcher.R
import com.kafka.launcher.domain.model.InstalledApp

@Composable
fun AppActionsDialog(
    app: InstalledApp,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.app_actions_title, app.label)) },
        text = {
            Column {
                TextButton(onClick = onToggleFavorite) {
                    val labelRes = if (isPinned) R.string.app_action_unfavorite else R.string.app_action_favorite
                    Text(text = stringResource(id = labelRes))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDelete) {
                    Text(text = stringResource(id = R.string.app_action_delete))
                }
            }
        },
        confirmButton = {}
    )
}
