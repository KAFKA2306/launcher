package com.kafka.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kafka.launcher.R
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.launcher.LauncherState
import com.kafka.launcher.ui.components.FavoriteAppsRow
import com.kafka.launcher.ui.components.KafkaSearchBar
import com.kafka.launcher.ui.components.QuickActionRow
import com.kafka.launcher.ui.components.LauncherIcons

@Composable
fun HomeScreen(
    state: LauncherState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onQuickActionClick: (QuickAction) -> Unit,
    onRecommendedClick: (QuickAction) -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }
        KafkaSearchBar(
            value = state.searchQuery,
            placeholder = stringResource(id = R.string.search_placeholder),
            onValueChange = onSearchQueryChange,
            onClear = onClearSearch
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (state.searchQuery.isNotBlank()) {
            SearchResults(
                actions = state.filteredQuickActions,
                apps = state.filteredApps,
                onQuickActionClick = onQuickActionClick,
                onAppClick = onAppClick
            )
        } else {
            QuickActionRow(
                title = stringResource(id = R.string.recommended_title),
                actions = state.recommendedActions,
                onActionClick = onRecommendedClick
            )
            Spacer(modifier = Modifier.height(24.dp))
            QuickActionRow(
                title = stringResource(id = R.string.actions_title),
                actions = state.quickActions,
                onActionClick = onQuickActionClick
            )
            if (state.settings.showFavorites && state.favoriteApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                FavoriteAppsRow(
                    title = stringResource(id = R.string.favorites_title),
                    apps = state.favoriteApps,
                    onAppClick = onAppClick
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
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
