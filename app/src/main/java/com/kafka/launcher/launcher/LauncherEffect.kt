package com.kafka.launcher.launcher

import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.QuickAction

sealed interface LauncherEffect {
    data class LaunchQuickAction(val action: QuickAction, val query: String) : LauncherEffect
    data class LaunchApp(val app: InstalledApp) : LauncherEffect
}
