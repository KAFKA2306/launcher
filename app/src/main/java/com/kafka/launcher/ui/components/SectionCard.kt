package com.kafka.launcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kafka.launcher.config.LauncherConfig

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(LauncherConfig.sectionCardCornerRadiusDp.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = LauncherConfig.sectionCardElevationDp.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = LauncherConfig.sectionCardPaddingHorizontalDp.dp,
                vertical = LauncherConfig.sectionCardPaddingVerticalDp.dp
            ),
            content = content
        )
    }
}
