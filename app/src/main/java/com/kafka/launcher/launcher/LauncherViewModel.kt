package com.kafka.launcher.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.data.repo.ActionLogRepository
import com.kafka.launcher.data.repo.AppRepository
import com.kafka.launcher.data.repo.QuickActionRepository
import com.kafka.launcher.data.repo.SettingsRepository
import com.kafka.launcher.domain.model.ActionStats
import com.kafka.launcher.domain.model.AppSort
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.NavigationInfo
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.domain.usecase.RecommendActionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val appRepository: AppRepository,
    private val quickActionRepository: QuickActionRepository,
    private val actionLogRepository: ActionLogRepository,
    private val settingsRepository: SettingsRepository,
    private val recommendActionsUseCase: RecommendActionsUseCase,
    private val navigationInfo: NavigationInfo
) : ViewModel() {

    private val statsSnapshot = MutableStateFlow<List<ActionStats>>(emptyList())
    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    private var cachedApps: List<InstalledApp> = emptyList()

    init {
        _state.update { it.copy(navigationInfo = navigationInfo) }
        observeQuickActions()
        observeStats()
        observeSettings()
        loadApps()
        updateFavoriteApps()
    }

    fun onSearchQueryChange(query: String) {
        applyFilters(query)
    }

    fun clearSearch() {
        applyFilters("")
    }

    fun onQuickActionExecuted(actionId: String) {
        viewModelScope.launch {
            actionLogRepository.log(actionId)
        }
    }

    fun onAppLaunched(packageName: String) {
        viewModelScope.launch {
            actionLogRepository.log(appUsageKey(packageName))
        }
    }

    fun setShowFavorites(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowFavorites(enabled)
        }
    }

    fun setAppSort(sort: AppSort) {
        viewModelScope.launch {
            settingsRepository.setAppSort(sort)
        }
    }

    private fun observeQuickActions() {
        viewModelScope.launch {
            quickActionRepository.observe().collect { actions ->
                _state.update { it.copy(quickActions = actions) }
                refreshRecommendations(actions, statsSnapshot.value)
                applyFilters()
            }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            actionLogRepository.stats(LauncherConfig.statsLimit).collect { stats ->
                statsSnapshot.value = stats
                refreshRecommendations(_state.value.quickActions, stats)
                updateFavoriteApps()
                applyFilters()
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
                updateFavoriteApps()
                applyFilters()
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = appRepository.loadApps()
            cachedApps = apps
            applyFilters()
            updateFavoriteApps()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun refreshRecommendations(actions: List<QuickAction>, stats: List<ActionStats>) {
        val fallback = if (actions.isEmpty()) emptyList() else actions.take(LauncherConfig.recommendationFallbackCount)
        val recommendations = recommendActionsUseCase(actions, stats, fallback)
        _state.update { it.copy(recommendedActions = recommendations) }
    }

    private fun applyFilters(query: String = _state.value.searchQuery) {
        val sortedApps = sortApps(cachedApps, _state.value.settings.appSort)
        val filteredApps = if (query.isBlank()) sortedApps else appRepository.filter(sortedApps, query)
        val filteredQuickActions = if (query.isBlank()) emptyList() else quickActionRepository.filter(query)
        _state.update {
            it.copy(
                searchQuery = query,
                installedApps = sortedApps,
                filteredApps = filteredApps,
                filteredQuickActions = filteredQuickActions
            )
        }
    }

    private fun sortApps(apps: List<InstalledApp>, sort: AppSort): List<InstalledApp> {
        if (apps.isEmpty()) return emptyList()
        val usageMap = statsSnapshot.value.associate { it.actionId to it.count }
        return when (sort) {
            AppSort.NAME -> apps.sortedBy { it.label.lowercase() }
            AppSort.USAGE -> apps.sortedWith(
                compareByDescending<InstalledApp> { usageMap[appUsageKey(it.packageName)] ?: 0 }
                    .thenBy { it.label.lowercase() }
            )
        }
    }

    private fun updateFavoriteApps() {
        val settings = _state.value.settings
        if (!settings.showFavorites || cachedApps.isEmpty()) {
            _state.update { it.copy(favoriteApps = emptyList()) }
            return
        }
        val appsByPackage = cachedApps.associateBy { it.packageName }
        val favorites = statsSnapshot.value
            .asSequence()
            .filter { it.actionId.startsWith(LauncherConfig.appUsagePrefix) }
            .sortedByDescending { it.count }
            .mapNotNull { stat ->
                val packageName = stat.actionId.removePrefix(LauncherConfig.appUsagePrefix)
                appsByPackage[packageName]
            }
            .distinctBy { it.packageName }
            .take(LauncherConfig.favoritesLimit)
            .toList()

        val resolvedFavorites = if (favorites.isNotEmpty()) {
            favorites
        } else {
            sortApps(cachedApps, AppSort.NAME).take(LauncherConfig.favoritesLimit)
        }

        _state.update { it.copy(favoriteApps = resolvedFavorites) }
    }

    private fun appUsageKey(packageName: String) = "${LauncherConfig.appUsagePrefix}$packageName"
}
