package com.kafka.launcher.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kafka.launcher.data.repo.AiModelRepository
import com.kafka.launcher.domain.model.AiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AiHubViewModel(
    private val aiModelRepository: AiModelRepository
) : ViewModel() {

    val candidateModels: StateFlow<List<AiModel>> = aiModelRepository.candidateModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adoptedModels: StateFlow<List<AiModel>> = aiModelRepository.adoptedModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rejectedModels: StateFlow<List<AiModel>> = aiModelRepository.rejectedModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            aiModelRepository.refreshModels()
        }
    }

    fun adoptModel(modelId: String) {
        viewModelScope.launch {
            aiModelRepository.adoptModel(modelId)
        }
    }

    fun rejectModel(modelId: String) {
        viewModelScope.launch {
            aiModelRepository.rejectModel(modelId)
        }
    }

    fun restoreModel(modelId: String) {
        viewModelScope.launch {
            aiModelRepository.restoreModel(modelId)
        }
    }
}
