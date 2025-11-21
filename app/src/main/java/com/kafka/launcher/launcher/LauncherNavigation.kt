package com.kafka.launcher.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kafka.launcher.domain.model.AppSort
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.ui.ai.AiScreen
import com.kafka.launcher.ui.components.AppActionsDialog
import com.kafka.launcher.ui.drawer.AppDrawerScreen
import com.kafka.launcher.ui.home.HomeScreen
import com.kafka.launcher.ui.settings.SettingsScreen

object LauncherDestinations {
    const val HOME = "home"
    const val DRAWER = "drawer"
    const val SETTINGS = "settings"
    const val AI = "ai"
    const val AI_HUB = "ai_hub"
}

@Composable
fun LauncherNavHost(
    state: LauncherState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onQuickActionClick: (QuickAction) -> Unit,
    onRecommendedClick: (QuickAction) -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onToggleFavorites: (Boolean) -> Unit,
    onSortSelected: (AppSort) -> Unit,
    onRequestHomeRole: () -> Unit,
    onPinApp: (String) -> Unit,
    onUnpinApp: (String) -> Unit,
    onDeleteApp: (String) -> Unit,
    onGeminiApiKeyInputChange: (String) -> Unit,
    onSaveGeminiApiKey: () -> Unit,
    onClearGeminiApiKey: () -> Unit,
    onAiRefresh: () -> Unit,
    onAiAccept: (String) -> Unit,
    onAiDismiss: (String) -> Unit,
    onAiRestore: (String) -> Unit,
    discordSettingsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    NavHost(
        navController = navController,
        startDestination = LauncherDestinations.HOME,
        modifier = modifier
    ) {
        composable(LauncherDestinations.HOME) {
            HomeScreen(
                state = state,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                onQuickActionClick = onQuickActionClick,
                onRecommendedClick = onRecommendedClick,
                onAppClick = onAppClick,
                onAppLongPress = { selectedApp = it },
                onOpenDrawer = { navController.navigate(LauncherDestinations.DRAWER) },
                onOpenAi = { navController.navigate(LauncherDestinations.AI) },
                onOpenSettings = { navController.navigate(LauncherDestinations.SETTINGS) }
            )
        }
        composable(LauncherDestinations.DRAWER) {
            AppDrawerScreen(
                state = state,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                onAppClick = onAppClick,
                onAppLongPress = { selectedApp = it },
                onBack = { navController.popBackStack() }
            )
        }
        composable(LauncherDestinations.SETTINGS) {
            SettingsScreen(
                settings = state.settings,
                navigationInfo = state.navigationInfo,
                onToggleFavorites = onToggleFavorites,
                onSortSelected = onSortSelected,
                onBack = { navController.popBackStack() },
                onRequestHomeRole = onRequestHomeRole,
                recommendationTimestamp = state.recommendationTimestamp,
                geminiApiKeyInput = state.geminiApiKeyInput,
                isGeminiApiKeyConfigured = state.geminiApiKeyConfigured,
                onGeminiApiKeyInputChange = onGeminiApiKeyInputChange,
                onSaveGeminiApiKey = onSaveGeminiApiKey,
                onClearGeminiApiKey = onClearGeminiApiKey,
                aiPreviewState = state.aiPreview,
                discordSettingsContent = discordSettingsContent,
                onOpenAiHub = { navController.navigate(LauncherDestinations.AI_HUB) }
            )
        }
        composable(LauncherDestinations.AI) {
            AiScreen(
                state = state.aiCenter,
                onBack = { navController.popBackStack() },
                onRefresh = onAiRefresh,
                onAccept = onAiAccept,
                onDismiss = onAiDismiss,
                onRestore = onAiRestore
            )
        }
        composable(LauncherDestinations.AI_HUB) {
            com.kafka.launcher.ui.aihub.AiHubScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
    val currentApp = selectedApp
    if (currentApp != null) {
        val pinned = state.pinnedPackages.contains(currentApp.packageName)
        AppActionsDialog(
            app = currentApp,
            isPinned = pinned,
            onDismiss = { selectedApp = null },
            onToggleFavorite = {
                if (pinned) {
                    onUnpinApp(currentApp.packageName)
                } else {
                    onPinApp(currentApp.packageName)
                }
                selectedApp = null
            },
            onDelete = {
                onDeleteApp(currentApp.packageName)
                selectedApp = null
            }
        )
    }
}
