package com.kafka.launcher.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kafka.launcher.config.GeminiConfig
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.data.repo.ActionLogRepository
import com.kafka.launcher.data.repo.AppRepository
import com.kafka.launcher.data.repo.PinnedAppsRepository
import com.kafka.launcher.data.repo.QuickActionRepository
import com.kafka.launcher.data.repo.SettingsRepository
import com.kafka.launcher.data.store.GeminiRecommendationStore
import com.kafka.launcher.data.store.GeminiApiKeyStore
import com.kafka.launcher.domain.model.ActionLog
import com.kafka.launcher.domain.model.ActionStats
import com.kafka.launcher.domain.model.AppCategory
import com.kafka.launcher.domain.model.AppSort
import com.kafka.launcher.domain.model.GeminiRecommendationWindow
import com.kafka.launcher.domain.model.GeminiRecommendations
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.NavigationInfo
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.domain.usecase.RecommendActionsUseCase
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.LinkedHashMap
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
    private val navigationInfo: NavigationInfo,
    private val pinnedAppsRepository: PinnedAppsRepository,
    private val geminiRecommendationStore: GeminiRecommendationStore,
    private val geminiApiKeyStore: GeminiApiKeyStore
) : ViewModel() {

    private val statsSnapshot = MutableStateFlow<List<ActionStats>>(emptyList())
    private val recentSnapshot = MutableStateFlow<List<ActionLog>>(emptyList())
    private val geminiSnapshot = MutableStateFlow<GeminiRecommendations?>(null)
    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    private var cachedApps: List<InstalledApp> = emptyList()
    private var rawQuickActions: List<QuickAction> = emptyList()

    init {
        _state.update { it.copy(navigationInfo = navigationInfo) }
        observeQuickActions()
        observeStats()
        observeRecentLogs()
        observeSettings()
        observePinnedApps()
        observeGeminiRecommendations()
        observeGeminiApiKey()
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

    fun toggleAiPreview() {
        val current = _state.value.aiPreview
        _state.update { it.copy(aiPreview = current.copy(isExpanded = !current.isExpanded)) }
    }

    fun onGeminiApiKeyInputChange(value: String) {
        _state.update { it.copy(geminiApiKeyInput = value) }
    }

    fun saveGeminiApiKey() {
        val input = _state.value.geminiApiKeyInput.trim()
        if (input.isBlank()) return
        viewModelScope.launch {
            geminiApiKeyStore.save(input)
            _state.update { it.copy(geminiApiKeyInput = "") }
        }
    }

    fun clearGeminiApiKey() {
        viewModelScope.launch {
            geminiApiKeyStore.clear()
            _state.update { it.copy(geminiApiKeyInput = "") }
        }
    }

    private fun observeQuickActions() {
        viewModelScope.launch {
            quickActionRepository.observe().collect { actions ->
                rawQuickActions = actions
                refreshGeminiOutputs()
            }
        }
    }

    private fun observeGeminiApiKey() {
        viewModelScope.launch {
            geminiApiKeyStore.data.collect { key ->
                _state.update { it.copy(geminiApiKeyConfigured = key.isNotBlank()) }
            }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            actionLogRepository.stats(LauncherConfig.statsLimit).collect { stats ->
                statsSnapshot.value = stats
                refreshGeminiOutputs()
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

    private fun observeGeminiRecommendations() {
        viewModelScope.launch {
            geminiRecommendationStore.data.collect { snapshot ->
                geminiSnapshot.value = snapshot
                refreshGeminiOutputs()
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

    private fun refreshGeminiOutputs() {
        val snapshot = geminiSnapshot.value
        val suppressed = snapshot?.suppressions?.toSet() ?: emptySet()
        val filteredQuickActions = filterSuppressed(rawQuickActions, suppressed)
        val (recommendations, windowId) = computeRecommendations(filteredQuickActions, statsSnapshot.value, snapshot)
        val preview = buildAiPreview(snapshot, filteredQuickActions, _state.value.aiPreview.isExpanded)
        _state.update {
            it.copy(
                quickActions = filteredQuickActions,
                recommendedActions = recommendations,
                currentTimeWindowId = windowId,
                geminiPins = snapshot?.globalPins ?: emptyList(),
                suppressedActionIds = suppressed,
                recommendationTimestamp = snapshot?.generatedAt,
                aiPreview = preview
            )
        }
        updateFavoriteApps()
        applyFilters()
    }

    private fun computeRecommendations(
        actions: List<QuickAction>,
        stats: List<ActionStats>,
        snapshot: GeminiRecommendations?
    ): RecommendationResult {
        if (snapshot == null) {
            val fallback = recommendActionsUseCase(actions, stats)
            return RecommendationResult(fallback, null)
        }
        val window = resolveWindow(snapshot)
        if (window == null) {
            val fallback = recommendActionsUseCase(actions, stats)
            return RecommendationResult(fallback, null)
        }
        val map = actions.associateBy { it.id }
        val ordered = (window.primaryActionIds + window.fallbackActionIds)
            .mapNotNull { map[it] }
            .distinct()
        if (ordered.isEmpty()) {
            val fallback = recommendActionsUseCase(actions, stats)
            return RecommendationResult(fallback, window.id)
        }
        return RecommendationResult(ordered.take(LauncherConfig.bottomQuickActionLimit), window.id)
    }

    private fun resolveWindow(snapshot: GeminiRecommendations): GeminiRecommendationWindow? {
        if (snapshot.windows.isEmpty()) return null
        val now = ZonedDateTime.now()
        val isWeekend = now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY
        val window = snapshot.windows.firstOrNull { matchesWindow(it, now.toLocalTime(), isWeekend) }
        return window ?: snapshot.windows.first()
    }

    private fun matchesWindow(window: GeminiRecommendationWindow, current: LocalTime, weekend: Boolean): Boolean {
        val startValue = window.start ?: return false
        val endValue = window.end ?: return false
        val start = LocalTime.parse(startValue)
        val end = LocalTime.parse(endValue)
        if (window.id.contains("weekend") && !weekend) return false
        if (window.id.contains("weekday") && weekend) return false
        return if (start <= end) {
            !current.isBefore(start) && current.isBefore(end)
        } else {
            !current.isBefore(start) || current.isBefore(end)
        }
    }

    private fun buildAiPreview(
        snapshot: GeminiRecommendations?,
        actions: List<QuickAction>,
        expanded: Boolean
    ): AiPreviewState {
        if (snapshot == null) {
            return AiPreviewState(isExpanded = expanded)
        }
        val map = actions.associateBy { it.id }
        val windows = snapshot.windows
            .take(GeminiConfig.aiPreviewWindowLimit)
            .map { window ->
                AiPreviewWindow(
                    id = window.id,
                    primary = window.primaryActionIds.map { map[it]?.label ?: it },
                    fallback = window.fallbackActionIds.map { map[it]?.label ?: it }
                )
            }
        val rationales = snapshot.rationales
            .take(GeminiConfig.aiPreviewRationaleLimit)
            .map { rationale ->
                val label = map[rationale.targetId]?.label ?: rationale.targetId
                AiPreviewRationale(target = label, summary = rationale.summary)
            }
        return AiPreviewState(
            generatedAt = snapshot.generatedAt,
            windows = windows,
            rationales = rationales,
            isExpanded = expanded
        )
    }

    private fun filterSuppressed(actions: List<QuickAction>, suppressed: Set<String>): List<QuickAction> {
        if (suppressed.isEmpty()) return actions
        return actions.filterNot { suppressed.contains(it.id) }
    }

    private fun applyFilters(query: String = _state.value.searchQuery) {
        val sortedApps = sortApps(cachedApps, _state.value.settings.appSort)
        val filteredApps = if (query.isBlank()) sortedApps else appRepository.filter(sortedApps, query)
        val quickActionResults = if (query.isBlank()) emptyList() else quickActionRepository.filter(query)
        val suppressed = _state.value.suppressedActionIds
        val filteredQuickActions = if (suppressed.isEmpty()) quickActionResults else quickActionResults.filterNot { suppressed.contains(it.id) }
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
        val geminiPins = _state.value.geminiPins.mapNotNull { appsByPackage[it] }
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
        val combined = (geminiPins + pinned + favorites)
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

    private data class RecommendationResult(
        val actions: List<QuickAction>,
        val windowId: String?
    )
}
