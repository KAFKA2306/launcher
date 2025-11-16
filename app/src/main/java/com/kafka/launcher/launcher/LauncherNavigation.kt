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
import com.kafka.launcher.ui.components.AppActionsDialog
import com.kafka.launcher.ui.drawer.AppDrawerScreen
import com.kafka.launcher.ui.home.HomeScreen
import com.kafka.launcher.ui.settings.SettingsScreen

object LauncherDestinations {
    const val HOME = "home"
    const val DRAWER = "drawer"
    const val SETTINGS = "settings"
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
                navigationInfo = state.navigationInfo,
                onOpenDrawer = { navController.navigate(LauncherDestinations.DRAWER) },
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
                onRequestHomeRole = onRequestHomeRole
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
