package com.kafka.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.R
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.NavigationInfo
import com.kafka.launcher.domain.model.NavigationMode
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.launcher.LauncherState
import com.kafka.launcher.ui.components.AppGrid
import com.kafka.launcher.ui.components.FavoriteAppsRow
import com.kafka.launcher.ui.components.KafkaSearchBar
import com.kafka.launcher.ui.components.QuickActionRow
import com.kafka.launcher.ui.components.LauncherIcons
import com.kafka.launcher.ui.components.NavigationNotice

@Composable
fun HomeScreen(
    state: LauncherState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onQuickActionClick: (QuickAction) -> Unit,
    onRecommendedClick: (QuickAction) -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    navigationInfo: NavigationInfo,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dockQuickActions = state.quickActions.take(LauncherConfig.bottomQuickActionLimit)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onOpenDrawer, modifier = Modifier.weight(1f)) {
                Icon(
                    painter = painterResource(id = LauncherIcons.Drawer),
                    contentDescription = stringResource(id = R.string.drawer_button)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(id = R.string.drawer_button))
            }
            Button(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(id = R.string.settings_button))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (navigationInfo.mode == NavigationMode.THREE_BUTTON) {
            NavigationNotice(info = navigationInfo, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }
        BottomLauncherPanel(
            state = state,
            quickActions = dockQuickActions,
            onSearchQueryChange = onSearchQueryChange,
            onClearSearch = onClearSearch,
            onRecommendedClick = onRecommendedClick,
            onAppClick = onAppClick,
            onQuickActionClick = onQuickActionClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun BottomLauncherPanel(
    state: LauncherState,
    quickActions: List<QuickAction>,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRecommendedClick: (QuickAction) -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onQuickActionClick: (QuickAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSearching = state.searchQuery.isNotBlank()
    Box(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                KafkaSearchBar(
                    value = state.searchQuery,
                    placeholder = stringResource(id = R.string.search_placeholder),
                    onValueChange = onSearchQueryChange,
                    onClear = onClearSearch
                )
            }
            if (isSearching) {
                item {
                    SearchResults(
                        actions = state.filteredQuickActions,
                        apps = state.filteredApps,
                        onQuickActionClick = onQuickActionClick,
                        onAppClick = onAppClick
                    )
                }
            } else {
                item {
                    QuickActionRow(
                        title = stringResource(id = R.string.recommended_title),
                        actions = state.recommendedActions,
                        onActionClick = onRecommendedClick
                    )
                }
                if (state.recentApps.isNotEmpty()) {
                    item {
                        FavoriteAppsRow(
                            title = stringResource(id = R.string.recents_title),
                            apps = state.recentApps,
                            onAppClick = onAppClick
                        )
                    }
                }
                if (state.settings.showFavorites && state.favoriteApps.isNotEmpty()) {
                    item {
                        FavoriteAppsRow(
                            title = stringResource(id = R.string.favorites_title),
                            apps = state.favoriteApps,
                            onAppClick = onAppClick
                        )
                    }
                }
                item {
                    QuickActionRow(
                        title = stringResource(id = R.string.actions_title),
                        actions = quickActions,
                        onActionClick = onQuickActionClick
                    )
                }
            }
            item {
                AppGridSection(apps = state.installedApps, onAppClick = onAppClick)
            }
        }
    }
}

@Composable
private fun AppGridSection(
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit
) {
    if (apps.isEmpty()) {
        Text(text = stringResource(id = R.string.empty_results), style = MaterialTheme.typography.bodyMedium)
        return
    }
    Text(
        text = stringResource(id = R.string.drawer_title),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LauncherConfig.homeGridMinHeightDp.dp)
    ) {
        AppGrid(
            apps = apps,
            onAppClick = onAppClick,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        )
    }
}

@Composable
private fun SearchResults(
    actions: List<QuickAction>,
    apps: List<InstalledApp>,
    onQuickActionClick: (QuickAction) -> Unit,
    onAppClick: (InstalledApp) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (actions.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.search_results_actions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                actions.forEach { action ->
                    SearchResultCard(title = action.label, subtitle = action.providerId) {
                        onQuickActionClick(action)
                    }
                }
            }
        }
        if (apps.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.search_results_apps),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                apps.forEach { app ->
                    SearchResultCard(title = app.label, subtitle = app.packageName) {
                        onAppClick(app)
                    }
                }
            }
        }
        if (actions.isEmpty() && apps.isEmpty()) {
            Text(text = stringResource(id = R.string.empty_results), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SearchResultCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
