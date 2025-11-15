package com.kafka.launcher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.R

@Composable
fun SearchResultsList(
    query: String,
    quickActions: List<QuickAction>,
    apps: List<InstalledApp>,
    onQuickActionSelected: (QuickAction) -> Unit,
    onAppSelected: (InstalledApp) -> Unit
) {
    if (query.isBlank()) return
    val hasResults = quickActions.isNotEmpty() || apps.isNotEmpty()
    val emptyLabel = stringResource(id = R.string.empty_results)
    Card(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
        if (!hasResults) {
            ListItem(
                headlineContent = { Text(text = emptyLabel) }
            )
        } else {
            LazyColumn {
                if (quickActions.isNotEmpty()) {
                    items(quickActions, key = { it.id }) { action ->
                        ListItem(
                            leadingContent = { androidx.compose.material3.Icon(imageVector = actionIcon(action.actionType), contentDescription = null) },
                            headlineContent = { Text(text = action.label) },
                            supportingContent = { Text(text = action.providerId) },
                            modifier = Modifier.clickable { onQuickActionSelected(action) }
                        )
                    }
                }
                if (apps.isNotEmpty()) {
                    items(apps, key = { it.packageName }) { app ->
                        ListItem(
                            leadingContent = { AppIcon(app = app, size = 40.dp) },
                            headlineContent = { Text(text = app.label) },
                            supportingContent = { Text(text = app.packageName) },
                            modifier = Modifier.clickable { onAppSelected(app) }
                        )
                    }
                }
            }
        }
    }
}
