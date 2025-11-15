package com.kafka.launcher.ui.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kafka.launcher.R
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.launcher.LauncherState
import com.kafka.launcher.ui.components.AppGrid
import com.kafka.launcher.ui.components.KafkaSearchBar

@Composable
fun AppDrawerScreen(
    state: LauncherState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.drawer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.drawer_back))
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
            if (state.filteredApps.isEmpty()) {
                Text(text = stringResource(id = R.string.empty_results))
            } else {
                AppGrid(
                    apps = state.filteredApps,
                    onAppClick = onAppClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
