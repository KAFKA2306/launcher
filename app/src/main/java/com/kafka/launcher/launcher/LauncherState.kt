package com.kafka.launcher.launcher

import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.NavigationInfo
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.domain.model.Settings

data class LauncherState(
    val searchQuery: String = "",
    val quickActions: List<QuickAction> = emptyList(),
    val filteredQuickActions: List<QuickAction> = emptyList(),
    val recommendedActions: List<QuickAction> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val filteredApps: List<InstalledApp> = emptyList(),
    val favoriteApps: List<InstalledApp> = emptyList(),
    val settings: Settings = Settings(),
    val navigationInfo: NavigationInfo = NavigationInfo(),
    val isLoading: Boolean = true
)
