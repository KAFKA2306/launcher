package com.kafka.launcher.domain.model

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val componentName: ComponentName,
    val label: String,
    val icon: Drawable,
    val isPinned: Boolean = false
)
