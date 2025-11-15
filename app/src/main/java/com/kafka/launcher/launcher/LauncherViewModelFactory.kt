package com.kafka.launcher.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kafka.launcher.data.repo.ActionLogRepository
import com.kafka.launcher.data.repo.AppRepository
import com.kafka.launcher.data.repo.QuickActionRepository
import com.kafka.launcher.data.repo.SettingsRepository
import com.kafka.launcher.domain.usecase.RecommendActionsUseCase

class LauncherViewModelFactory(
    private val appRepository: AppRepository,
    private val quickActionRepository: QuickActionRepository,
    private val actionLogRepository: ActionLogRepository,
    private val settingsRepository: SettingsRepository,
    private val recommendActionsUseCase: RecommendActionsUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            return LauncherViewModel(
                appRepository = appRepository,
                quickActionRepository = quickActionRepository,
                actionLogRepository = actionLogRepository,
                settingsRepository = settingsRepository,
                recommendActionsUseCase = recommendActionsUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
