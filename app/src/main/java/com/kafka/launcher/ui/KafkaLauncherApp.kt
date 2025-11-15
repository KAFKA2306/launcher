package com.kafka.launcher.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kafka.launcher.launcher.LauncherEffect
import com.kafka.launcher.launcher.LauncherViewModel
import com.kafka.launcher.quickactions.QuickActionExecutor
import com.kafka.launcher.ui.drawer.AppDrawerScreen
import com.kafka.launcher.ui.home.HomeScreen
import com.kafka.launcher.ui.settings.SettingsScreen
import com.kafka.launcher.ui.theme.KafkaLauncherTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun KafkaLauncherApp(viewModel: LauncherViewModel) {
    KafkaLauncherTheme {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = rememberNavController()
        val context = LocalContext.current
        val executor = remember(context) { QuickActionExecutor(context.applicationContext) }
        LaunchedEffect(viewModel) {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is LauncherEffect.LaunchQuickAction -> executor.execute(effect.action, effect.query)
                    is LauncherEffect.LaunchApp -> launchApp(context, effect.app.packageName, effect.app.componentName.className)
                }
            }
        }
        BackHandler(enabled = navController.currentDestination?.route != LauncherRoute.Home.route) {
            if (!navController.popBackStack()) {
                navController.navigate(LauncherRoute.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
            }
        }
        NavHost(navController = navController, startDestination = LauncherRoute.Home.route) {
            composable(LauncherRoute.Home.route) {
                HomeScreen(
                    state = state,
                    onQueryChanged = viewModel::onQueryChanged,
                    onQuickActionSelected = viewModel::onQuickActionSelected,
                    onAppSelected = viewModel::onAppSelected,
                    onOpenDrawer = { navController.navigate(LauncherRoute.Drawer.route) },
                    onOpenSettings = { navController.navigate(LauncherRoute.Settings.route) }
                )
            }
            composable(LauncherRoute.Drawer.route) {
                AppDrawerScreen(
                    apps = state.apps,
                    onAppSelected = viewModel::onAppSelected,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(LauncherRoute.Settings.route) {
                SettingsScreen(
                    showFavorites = state.showFavorites,
                    appSort = state.appSort,
                    onShowFavoritesChanged = viewModel::onFavoritesToggled,
                    onAppSortChanged = viewModel::onSortChanged,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

enum class LauncherRoute(val route: String) {
    Home("home"),
    Drawer("drawer"),
    Settings("settings")
}

private fun launchApp(context: Context, packageName: String, className: String) {
    val intent = Intent(Intent.ACTION_MAIN).setClassName(packageName, className).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
