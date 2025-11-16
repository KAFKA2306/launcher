package com.kafka.launcher.launcher

import com.kafka.launcher.domain.model.AppCategory
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
    val categorizedApps: Map<AppCategory, List<InstalledApp>> = emptyMap(),
    val recentApps: List<InstalledApp> = emptyList(),
    val favoriteApps: List<InstalledApp> = emptyList(),
    val pinnedPackages: Set<String> = emptySet(),
    val settings: Settings = Settings(),
    val navigationInfo: NavigationInfo = NavigationInfo(),
    val geminiPins: List<String> = emptyList(),
    val suppressedActionIds: Set<String> = emptySet(),
    val recommendationTimestamp: String? = null,
    val currentTimeWindowId: String? = null,
    val aiPreview: AiPreviewState = AiPreviewState(),
    val geminiApiKeyInput: String = "",
    val geminiApiKeyConfigured: Boolean = false,
    val isLoading: Boolean = true
)

data class AiPreviewState(
    val generatedAt: String = "",
    val windows: List<AiPreviewWindow> = emptyList(),
    val rationales: List<AiPreviewRationale> = emptyList(),
    val isExpanded: Boolean = false
)

data class AiPreviewWindow(
    val id: String,
    val primary: List<String>,
    val fallback: List<String>
)

data class AiPreviewRationale(
    val target: String,
    val summary: String
)
