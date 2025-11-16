package com.kafka.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kafka.launcher.domain.model.InstalledApp

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun FavoriteAppsRow(
    title: String,
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit,
    onAppLongPress: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return
    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps, key = { it.packageName }) { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongPress(app) }
                        )
                ) {
                    AppIcon(app = app, size = 56.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
