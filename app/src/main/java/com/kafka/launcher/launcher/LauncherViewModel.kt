package com.kafka.launcher.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.data.repo.ActionLogRepository
import com.kafka.launcher.data.repo.AppRepository
import com.kafka.launcher.data.repo.PinnedAppsRepository
import com.kafka.launcher.data.repo.QuickActionRepository
import com.kafka.launcher.data.repo.SettingsRepository
import com.kafka.launcher.domain.model.ActionLog
import com.kafka.launcher.domain.model.ActionStats
import com.kafka.launcher.domain.model.AppCategory
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
import java.util.LinkedHashMap

class LauncherViewModel(
    private val appRepository: AppRepository,
    private val quickActionRepository: QuickActionRepository,
    private val actionLogRepository: ActionLogRepository,
    private val settingsRepository: SettingsRepository,
    private val recommendActionsUseCase: RecommendActionsUseCase,
    private val navigationInfo: NavigationInfo,
    private val pinnedAppsRepository: PinnedAppsRepository
) : ViewModel() {

    private val statsSnapshot = MutableStateFlow<List<ActionStats>>(emptyList())
    private val recentSnapshot = MutableStateFlow<List<ActionLog>>(emptyList())
    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    private var cachedApps: List<InstalledApp> = emptyList()

    init {
        _state.update { it.copy(navigationInfo = navigationInfo) }
        observeQuickActions()
        observeStats()
        observeRecentLogs()
        observeSettings()
        observePinnedApps()
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

    fun pinApp(packageName: String) {
        viewModelScope.launch {
            pinnedAppsRepository.pin(packageName)
        }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch {
            pinnedAppsRepository.unpin(packageName)
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

    private fun observeRecentLogs() {
        viewModelScope.launch {
            actionLogRepository.recent(LauncherConfig.recentLimit).collect { logs ->
                recentSnapshot.value = logs
                updateRecentApps()
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

    private fun observePinnedApps() {
        viewModelScope.launch {
            pinnedAppsRepository.pinnedApps.collect { pinned ->
                _state.update { it.copy(pinnedPackages = pinned) }
                updateFavoriteApps()
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = appRepository.loadApps()
            cachedApps = apps
            updateRecentApps()
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
        val categorized = categorizeApps(sortedApps)
        _state.update {
            it.copy(
                searchQuery = query,
                installedApps = sortedApps,
                filteredApps = filteredApps,
                filteredQuickActions = filteredQuickActions,
                categorizedApps = categorized
            )
        }
    }

    private fun sortApps(apps: List<InstalledApp>, sort: AppSort): List<InstalledApp> {
        if (apps.isEmpty()) return emptyList()
        val usageMap = statsSnapshot.value.associate { it.actionId to it.count }
        return when (sort) {
            AppSort.NAME -> apps.sortedBy { it.label.lowercase() }
            AppSort.USAGE -> apps.sortedWith(
                compareByDescending<InstalledApp> { usageMap[appUsageKey(it.packageName)] ?: 0L }
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
        val pinned = _state.value.pinnedPackages
            .mapNotNull { appsByPackage[it] }
            .sortedBy { it.label.lowercase() }
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

        val combined = (pinned + resolvedFavorites)
            .distinctBy { it.packageName }
            .take(LauncherConfig.favoritesLimit)

        _state.update { it.copy(favoriteApps = combined) }
    }

    private fun updateRecentApps() {
        if (cachedApps.isEmpty()) {
            _state.update { it.copy(recentApps = emptyList()) }
            return
        }
        val appsByPackage = cachedApps.associateBy { it.packageName }
        val recents = recentSnapshot.value
            .asSequence()
            .mapNotNull { log ->
                if (!log.actionId.startsWith(LauncherConfig.appUsagePrefix)) return@mapNotNull null
                val packageName = log.actionId.removePrefix(LauncherConfig.appUsagePrefix)
                appsByPackage[packageName]
            }
            .distinctBy { it.packageName }
            .take(LauncherConfig.recentLimit)
            .toList()
        _state.update { it.copy(recentApps = recents) }
    }

    private fun appUsageKey(packageName: String) = "${LauncherConfig.appUsagePrefix}$packageName"

    private fun categorizeApps(apps: List<InstalledApp>): Map<AppCategory, List<InstalledApp>> {
        if (apps.isEmpty()) return emptyMap()
        val grouped = apps.groupBy { it.category }
        val ordered = AppCategory.values().sortedBy { it.priority }
        val result = LinkedHashMap<AppCategory, List<InstalledApp>>()
        ordered.forEach { category ->
            val items = grouped[category]
            if (!items.isNullOrEmpty()) {
                result[category] = items
            }
        }
        return result
    }
}
