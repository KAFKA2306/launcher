package com.kafka.launcher.ui.aihub

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kafka.launcher.domain.model.AiModel
import com.kafka.launcher.launcher.AiHubViewModel

@Composable
fun AiHubScreen(
    viewModel: AiHubViewModel = viewModel(),
    onBack: () -> Unit
) {
    val candidateModels by viewModel.candidateModels.collectAsState()
    val adoptedModels by viewModel.adoptedModels.collectAsState()
    val rejectedModels by viewModel.rejectedModels.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Kouho", "Saiyouzumi", "Hisaiyou")

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Text("Back")
        }

        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> ModelList(
                models = candidateModels,
                actionLabel = "Adopt",
                onAction = viewModel::adoptModel
            )
            1 -> ModelList(
                models = adoptedModels,
                actionLabel = "Reject",
                onAction = viewModel::rejectModel
            )
            2 -> ModelList(
                models = rejectedModels,
                actionLabel = "Restore",
                onAction = viewModel::restoreModel
            )
        }
    }
}

@Composable
fun ModelList(
    models: List<AiModel>,
    actionLabel: String,
    onAction: (String) -> Unit
) {
    LazyColumn {
        items(models) { model ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = model.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = model.description, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Version: ${model.version}", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { onAction(model.id) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
