package com.kafka.launcher.ui.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.annotation.StringRes
import com.kafka.launcher.R
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.AppCategory
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.launcher.LauncherState
import com.kafka.launcher.ui.components.AppGrid
import com.kafka.launcher.ui.components.AppIcon
import com.kafka.launcher.ui.components.KafkaSearchBar
import com.kafka.launcher.ui.components.LauncherIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    state: LauncherState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSearching = state.searchQuery.isNotBlank()
    val apps = if (isSearching) state.filteredApps else state.installedApps
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.drawer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = LauncherIcons.Back), contentDescription = stringResource(id = R.string.drawer_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            KafkaSearchBar(
                value = state.searchQuery,
                placeholder = stringResource(id = R.string.search_placeholder),
                onValueChange = onSearchQueryChange,
                onClear = onClearSearch
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (apps.isEmpty()) {
                Text(text = stringResource(id = R.string.empty_results))
            } else {
                DrawerGridWithCategories(
                    apps = apps,
                    categories = state.categorizedApps,
                    showCategories = !isSearching,
                    onAppClick = onAppClick
                )
            }
        }
    }
}

@Composable
private fun DrawerGridWithCategories(
    apps: List<InstalledApp>,
    categories: Map<AppCategory, List<InstalledApp>>,
    showCategories: Boolean,
    onAppClick: (InstalledApp) -> Unit
) {
    val bottomPadding = if (showCategories && categories.isNotEmpty()) 176.dp else 0.dp
    Box(modifier = Modifier.fillMaxSize()) {
        AppGrid(
            apps = apps,
            onAppClick = onAppClick,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding)
        )
        if (showCategories && categories.isNotEmpty()) {
            CategoryOverlay(
                categories = categories,
                onAppClick = onAppClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun CategoryOverlay(
    categories: Map<AppCategory, List<InstalledApp>>,
    onAppClick: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = categories.entries.toList()
    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(id = R.string.categories_title))
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(entries, key = { it.key }) { entry ->
                    CategoryCard(
                        category = entry.key,
                        apps = entry.value,
                        onAppClick = onAppClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: AppCategory,
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 160.dp)
    ) {
        Text(
            text = stringResource(id = categoryLabelRes(category)),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            apps.take(LauncherConfig.categoryPreviewLimit).forEach { app ->
                CategoryAppChip(app = app, onAppClick = onAppClick)
            }
        }
    }
}

@Composable
private fun CategoryAppChip(
    app: InstalledApp,
    onAppClick: (InstalledApp) -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .clickable { onAppClick(app) },
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            AppIcon(app = app, size = 32.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@StringRes
private fun categoryLabelRes(category: AppCategory): Int {
    return when (category) {
        AppCategory.COMMUNICATION -> R.string.category_communication
        AppCategory.WORK -> R.string.category_work
        AppCategory.MEDIA -> R.string.category_media
        AppCategory.TRAVEL -> R.string.category_travel
        AppCategory.GAMES -> R.string.category_games
        AppCategory.TOOLS -> R.string.category_tools
        AppCategory.OTHER -> R.string.category_other
    }
}
