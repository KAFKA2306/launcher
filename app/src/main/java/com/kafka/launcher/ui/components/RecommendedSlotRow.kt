package com.kafka.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kafka.launcher.domain.model.QuickAction

@Composable
fun RecommendedSlotRow(actions: List<QuickAction>, onClick: (QuickAction) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(actions, key = { it.id }) { action ->
            ElevatedCard(onClick = { onClick(action) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 120.dp, height = 96.dp)) {
                    androidx.compose.material3.Icon(imageVector = actionIcon(action.actionType), contentDescription = null)
                    Text(text = action.label)
                }
            }
        }
    }
}
