package com.kafka.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.ui.theme.appGridLabel

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AppGrid(
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit,
    onAppLongPress: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(LauncherConfig.appsPerRow),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LauncherConfig.appGridSpacingDp.dp),
        horizontalArrangement = Arrangement.spacedBy(LauncherConfig.appGridSpacingDp.dp),
        contentPadding = contentPadding
    ) {
        items(apps, key = { it.componentName.flattenToString() }) { app ->
            AppTile(
                app = app,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongPress(app) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(app: InstalledApp, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        color = Color(LauncherConfig.surfaceLowColor),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(LauncherConfig.sectionCardCornerRadiusDp.dp),
        border = BorderStroke(
            width = LauncherConfig.appTileBorderWidthDp.dp,
            color = Color(LauncherConfig.surfaceBorderColor)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(8.dp)
        ) {
            AppIcon(app = app, size = 40.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.appGridLabel,
                maxLines = LauncherConfig.appGridLabelMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(LauncherConfig.appGridLabelWidthDp.dp),
                textAlign = TextAlign.Center,
                softWrap = true
            )
        }
    }
}
