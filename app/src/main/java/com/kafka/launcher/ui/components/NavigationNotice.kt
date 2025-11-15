package com.kafka.launcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kafka.launcher.R
import com.kafka.launcher.domain.model.NavigationInfo
import com.kafka.launcher.domain.model.NavigationMode

@Composable
fun NavigationNotice(info: NavigationInfo, modifier: Modifier = Modifier) {
    if (info.mode != NavigationMode.THREE_BUTTON) return
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(id = R.string.navigation_notice_title), style = MaterialTheme.typography.titleSmall)
            val body = if (info.isOemRestricted && info.manufacturer.isNotBlank()) {
                stringResource(id = R.string.navigation_notice_body_restricted, info.manufacturer)
            } else {
                stringResource(id = R.string.navigation_notice_body_three_button)
            }
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
