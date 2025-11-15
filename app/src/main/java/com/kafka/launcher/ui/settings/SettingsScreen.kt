package com.kafka.launcher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kafka.launcher.R
import com.kafka.launcher.domain.model.AppSort
import com.kafka.launcher.domain.model.Settings
import com.kafka.launcher.domain.model.NavigationInfo
import com.kafka.launcher.domain.model.NavigationMode
import com.kafka.launcher.ui.components.LauncherIcons
import com.kafka.launcher.ui.components.NavigationNotice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    navigationInfo: NavigationInfo,
    onToggleFavorites: (Boolean) -> Unit,
    onSortSelected: (AppSort) -> Unit,
    onBack: () -> Unit,
    onRequestHomeRole: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_title)) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (navigationInfo.mode == NavigationMode.THREE_BUTTON) {
                NavigationNotice(info = navigationInfo, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(id = R.string.toggle_favorites))
                Switch(checked = settings.showFavorites, onCheckedChange = onToggleFavorites)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(id = R.string.sort_label))
                AppSort.values().forEach { sort ->
                    SortRow(
                        label = stringResource(id = if (sort == AppSort.NAME) R.string.sort_name else R.string.sort_usage),
                        selected = settings.appSort == sort,
                        onClick = { onSortSelected(sort) }
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.request_home_role_description))
                Button(onClick = onRequestHomeRole, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.request_home_role))
                }
            }
        }
    }
}

@Composable
private fun SortRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        RadioButton(selected = selected, onClick = onClick)
    }
}
