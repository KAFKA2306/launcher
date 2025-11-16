package com.kafka.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.R
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.launcher.LauncherState
import com.kafka.launcher.ui.components.AppGrid
import com.kafka.launcher.ui.components.FavoriteAppsRow
import com.kafka.launcher.ui.components.KafkaSearchBar
import com.kafka.launcher.ui.components.QuickActionRow
import com.kafka.launcher.ui.components.LauncherIcons
import com.kafka.launcher.ui.components.SectionCard

@Composable
fun HomeScreen(
    state: LauncherState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onQuickActionClick: (QuickAction) -> Unit,
    onRecommendedClick: (QuickAction) -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onAppLongPress: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dockQuickActions = state.quickActions.take(LauncherConfig.bottomQuickActionLimit)
    val buttonShape = RoundedCornerShape(LauncherConfig.sectionCardCornerRadiusDp.dp)
    val primaryButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = LauncherConfig.primaryButtonElevationDefaultDp.dp,
        pressedElevation = LauncherConfig.primaryButtonElevationPressedDp.dp
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(LauncherConfig.homeBackgroundColor))
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(
                horizontal = LauncherConfig.homeContentHorizontalPaddingDp.dp,
                vertical = LauncherConfig.homeContentVerticalPaddingDp.dp
            )
    ) {
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onOpenDrawer,
                modifier = Modifier.weight(1f),
                shape = buttonShape,
                elevation = primaryButtonElevation
            ) {
                Icon(
                    painter = painterResource(id = LauncherIcons.Drawer),
                    contentDescription = stringResource(id = R.string.drawer_button)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(id = R.string.drawer_button))
            }
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
                shape = buttonShape,
                elevation = primaryButtonElevation
            ) {
                Icon(
                    painter = painterResource(id = LauncherIcons.Ai),
                    contentDescription = stringResource(id = R.string.ai_button)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(id = R.string.ai_button))
            }
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
                shape = buttonShape,
                elevation = primaryButtonElevation
            ) {
                Text(text = stringResource(id = R.string.settings_button))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        BottomLauncherPanel(
            state = state,
            quickActions = dockQuickActions,
            onSearchQueryChange = onSearchQueryChange,
            onClearSearch = onClearSearch,
            onRecommendedClick = onRecommendedClick,
            onAppClick = onAppClick,
            onAppLongPress = onAppLongPress,
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
    onAppLongPress: (InstalledApp) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(LauncherConfig.sectionVerticalSpacingDp.dp),
            contentPadding = PaddingValues(
                top = LauncherConfig.sectionSpacingTopDp.dp,
                bottom = LauncherConfig.sectionSpacingBottomDp.dp
            )
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
                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        SearchResults(
                            actions = state.filteredQuickActions,
                            apps = state.filteredApps,
                            onQuickActionClick = onQuickActionClick,
                            onAppClick = onAppClick
                        )
                    }
                }
            } else {
                if (state.recommendedActions.isNotEmpty()) {
                    item {
                        SectionCard(modifier = Modifier.fillMaxWidth()) {
                            QuickActionRow(
                                title = stringResource(id = R.string.recommended_title),
                                actions = state.recommendedActions,
                                onActionClick = onRecommendedClick
                            )
                        }
                    }
                }
                if (state.recentApps.isNotEmpty()) {
                    item {
                        SectionCard(modifier = Modifier.fillMaxWidth()) {
                            FavoriteAppsRow(
                                title = stringResource(id = R.string.recents_title),
                                apps = state.recentApps,
                                onAppClick = onAppClick,
                                onAppLongPress = onAppLongPress
                            )
                        }
                    }
                }
                if (state.settings.showFavorites && state.favoriteApps.isNotEmpty()) {
                    item {
                        SectionCard(modifier = Modifier.fillMaxWidth()) {
                            FavoriteAppsRow(
                                title = stringResource(id = R.string.favorites_title),
                                apps = state.favoriteApps,
                                onAppClick = onAppClick,
                                onAppLongPress = onAppLongPress
                            )
                        }
                    }
                }
                if (quickActions.isNotEmpty()) {
                    item {
                        SectionCard(modifier = Modifier.fillMaxWidth()) {
                            QuickActionRow(
                                title = stringResource(id = R.string.actions_title),
                                actions = quickActions,
                                onActionClick = onQuickActionClick
                            )
                        }
                    }
                }
            }
            item {
                AppGridSection(
                    apps = state.installedApps,
                    onAppClick = onAppClick,
                    onAppLongPress = onAppLongPress
                )
            }
        }
    }
}

@Composable
private fun AppGridSection(
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit,
    onAppLongPress: (InstalledApp) -> Unit
) {
    if (apps.isEmpty()) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.empty_results), style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.drawer_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(LauncherConfig.sectionVerticalSpacingDp.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(LauncherConfig.homeGridMinHeightDp.dp)
        ) {
            AppGrid(
                apps = apps,
                onAppClick = onAppClick,
                onAppLongPress = onAppLongPress,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LauncherConfig.sectionSpacingBottomDp.dp)
            )
        }
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
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = LauncherConfig.sectionCardElevationDp.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
