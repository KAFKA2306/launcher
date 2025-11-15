package com.kafka.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.kafka.launcher.domain.model.InstalledApp

@Composable
fun AppIcon(
    app: InstalledApp,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = app.label,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
