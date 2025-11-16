package com.kafka.launcher.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kafka.launcher.data.repo.ActionLogRepository
import com.kafka.launcher.data.repo.AppRepository
import com.kafka.launcher.data.repo.PinnedAppsRepository
import com.kafka.launcher.data.repo.QuickActionRepository
import com.kafka.launcher.data.repo.SettingsRepository
import com.kafka.launcher.data.store.GeminiRecommendationStore
import com.kafka.launcher.domain.model.NavigationInfo
import com.kafka.launcher.domain.usecase.RecommendActionsUseCase

class LauncherViewModelFactory(
    private val appRepository: AppRepository,
    private val quickActionRepository: QuickActionRepository,
    private val actionLogRepository: ActionLogRepository,
    private val settingsRepository: SettingsRepository,
    private val recommendActionsUseCase: RecommendActionsUseCase,
    private val navigationInfo: NavigationInfo,
    private val pinnedAppsRepository: PinnedAppsRepository,
    private val geminiRecommendationStore: GeminiRecommendationStore
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            return LauncherViewModel(
                appRepository = appRepository,
                quickActionRepository = quickActionRepository,
                actionLogRepository = actionLogRepository,
                settingsRepository = settingsRepository,
                recommendActionsUseCase = recommendActionsUseCase,
                navigationInfo = navigationInfo,
                pinnedAppsRepository = pinnedAppsRepository,
                geminiRecommendationStore = geminiRecommendationStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
