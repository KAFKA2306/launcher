package com.kafka.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import android.provider.Settings
import com.kafka.launcher.data.local.datastore.settingsDataStore
import com.kafka.launcher.data.local.db.KafkaDatabase
import com.kafka.launcher.data.log.ActionLogFileWriter
import com.kafka.launcher.data.log.QuickActionAuditLogger
import com.kafka.launcher.data.quickaction.QuickActionCatalogStore
import com.kafka.launcher.data.repo.ActionLogRepository
import com.kafka.launcher.data.repo.AppRepository
import com.kafka.launcher.data.repo.PinnedAppsRepository
import com.kafka.launcher.data.repo.QuickActionRepository
import com.kafka.launcher.data.repo.SettingsRepository
import com.kafka.launcher.data.store.GeminiRecommendationStore
import com.kafka.launcher.data.store.GeminiApiKeyStore
import com.kafka.launcher.data.system.NavigationInfoResolver
import com.kafka.launcher.domain.model.AppSort
import com.kafka.launcher.domain.model.InstalledApp
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.domain.discord.DiscordInteractor
import com.kafka.launcher.domain.usecase.NormalizeDiscordDisplayNameUseCase
import com.kafka.launcher.domain.usecase.ParseDiscordChannelKeyUseCase
import com.kafka.launcher.domain.usecase.RecommendActionsUseCase
import com.kafka.launcher.launcher.AiSyncStageKey
import com.kafka.launcher.launcher.AiSyncStatus
import com.kafka.launcher.config.GeminiConfig
import com.kafka.launcher.launcher.DiscordProvider
import com.kafka.launcher.launcher.LauncherNavHost
import com.kafka.launcher.launcher.LauncherState
import com.kafka.launcher.launcher.LauncherViewModel
import com.kafka.launcher.launcher.LauncherViewModelFactory
import com.kafka.launcher.launcher.worker.GeminiWorkScheduler
import com.kafka.launcher.quickactions.BraveModule
import com.kafka.launcher.quickactions.DiscordModule
import com.kafka.launcher.quickactions.GmailModule
import com.kafka.launcher.quickactions.GoogleCalendarModule
import com.kafka.launcher.quickactions.GoogleMapsModule
import com.kafka.launcher.quickactions.QuickActionExecutor
import com.kafka.launcher.ui.discord.DiscordViewModel
import com.kafka.launcher.ui.discord.DiscordViewModelFactory
import com.kafka.launcher.ui.settings.DiscordSettingsSection
import com.kafka.launcher.ui.theme.KafkaLauncherTheme
import com.kafka.launcher.ui.components.rememberNotificationAccessState

class MainActivity : ComponentActivity() {
    private val auditLogger by lazy { QuickActionAuditLogger(applicationContext) }
    private val actionLogFileWriter by lazy { ActionLogFileWriter(applicationContext) }
    private val geminiStore by lazy { GeminiRecommendationStore(applicationContext) }
    private val geminiApiKeyStore by lazy { GeminiApiKeyStore(applicationContext) }
    private val quickActionCatalogStore by lazy { QuickActionCatalogStore(applicationContext) }
    private val discordRepository by lazy { DiscordProvider.create(applicationContext) }
    private val discordInteractor by lazy {
        DiscordInteractor(
            repository = discordRepository,
            parseChannelKey = ParseDiscordChannelKeyUseCase(),
            normalizeDisplayName = NormalizeDiscordDisplayNameUseCase()
        )
    }
    private val launcherViewModel: LauncherViewModel by lazy {
        val appContext = applicationContext
        val database = KafkaDatabase.build(appContext)
        val navigationInfo = NavigationInfoResolver(appContext).resolve()
        val factory = LauncherViewModelFactory(
            appRepository = AppRepository(appContext),
            quickActionRepository = QuickActionRepository(
                appContext,
                listOf(
                    GoogleCalendarModule(),
                    GoogleMapsModule(),
                    GmailModule(),
                    DiscordModule(),
                    BraveModule()
                ),
                auditLogger,
                quickActionCatalogStore
            ),
            actionLogRepository = ActionLogRepository(database.actionLogDao(), actionLogFileWriter),
            settingsRepository = SettingsRepository(appContext.settingsDataStore),
            pinnedAppsRepository = PinnedAppsRepository(appContext.settingsDataStore),
            recommendActionsUseCase = RecommendActionsUseCase(),
            navigationInfo = navigationInfo,
            geminiRecommendationStore = geminiStore,
            geminiApiKeyStore = geminiApiKeyStore,
            quickActionCatalogStore = quickActionCatalogStore
        )
        ViewModelProvider(this, factory)[LauncherViewModel::class.java]
    }
    private val discordViewModel: DiscordViewModel by lazy {
        val factory = DiscordViewModelFactory(discordInteractor)
        ViewModelProvider(this, factory)[DiscordViewModel::class.java]
    }

    private val quickActionExecutor by lazy { QuickActionExecutor(this, auditLogger, quickActionCatalogStore) }
    private val uninstallLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        launcherViewModel.refreshApps()
    }
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val workManager by lazy { WorkManager.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestHomeRole()
        GeminiWorkScheduler.schedule(applicationContext)
        observeGeminiWork()
        setContent {
            val state by launcherViewModel.state.collectAsStateWithLifecycle()
            KafkaLauncherApp(
                state = state,
                onSearchQueryChange = launcherViewModel::onSearchQueryChange,
                onClearSearch = launcherViewModel::clearSearch,
                onQuickActionClick = { action -> handleQuickAction(action, state.searchQuery) },
                onRecommendedClick = { action -> handleQuickAction(action, state.searchQuery) },
                onAppClick = { app -> openInstalledApp(app) },
                onToggleFavorites = launcherViewModel::setShowFavorites,
                onSortSelected = launcherViewModel::setAppSort,
                onRequestHomeRole = ::requestHomeRole,
                onPinApp = launcherViewModel::pinApp,
                onUnpinApp = launcherViewModel::unpinApp,
                onDeleteApp = ::uninstallApp,
                onGeminiApiKeyInputChange = launcherViewModel::onGeminiApiKeyInputChange,
                onSaveGeminiApiKey = launcherViewModel::saveGeminiApiKey,
                onClearGeminiApiKey = launcherViewModel::clearGeminiApiKey,
                onAiRefresh = {
                    launcherViewModel.onAiSyncRequested()
                    GeminiWorkScheduler.refreshNow(applicationContext)
                },
                onAiAccept = launcherViewModel::acceptAiAction,
                onAiDismiss = launcherViewModel::dismissAiAction,
                onAiRestore = launcherViewModel::restoreAiAction,
                discordSettingsContent = {
                    val notificationAccess by rememberNotificationAccessState()
                    DiscordSettingsSection(
                        viewModel = discordViewModel,
                        notificationPermissionGranted = notificationAccess,
                        onOpenNotificationSettings = ::openNotificationAccessSettings
                    )
                }
            )
        }
    }

    private fun handleQuickAction(action: QuickAction, query: String) {
        quickActionExecutor.execute(action, query)
        launcherViewModel.onQuickActionExecuted(action.id)
    }

    private fun openInstalledApp(app: InstalledApp) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = app.componentName
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        launcherViewModel.onAppLaunched(app.packageName)
    }

    private fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = ("package:$packageName").toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        uninstallLauncher.launch(intent)
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return
        if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return
        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
    }

    private fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun observeGeminiWork() {
        workManager
            .getWorkInfosForUniqueWorkLiveData(GeminiConfig.manualWorkName)
            .observe(this) { infos ->
                val info = infos.firstOrNull() ?: return@observe
                val stage = info.progress.getString(AiSyncStageKey)
                    ?: info.outputData.getString(AiSyncStageKey)
                when (info.state) {
                    WorkInfo.State.ENQUEUED -> {
                        launcherViewModel.onAiSyncStageChanged(AiSyncStatus.Enqueued, "")
                    }
                    WorkInfo.State.RUNNING -> {
                        val status = AiSyncStatus.fromStageId(stage) ?: AiSyncStatus.Running
                        val targetStatus = if (status == AiSyncStatus.UpdatingCatalog) AiSyncStatus.UpdatingCatalog else AiSyncStatus.Running
                        launcherViewModel.onAiSyncStageChanged(targetStatus, "")
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        launcherViewModel.onAiSyncStageChanged(AiSyncStatus.Succeeded, "")
                    }
                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED -> {
                        val status = AiSyncStatus.fromStageId(stage) ?: AiSyncStatus.Failed
                        launcherViewModel.onAiSyncStageChanged(status, stage ?: "")
                    }
                    else -> {}
                }
            }
    }
}

@Composable
private fun KafkaLauncherApp(
    state: LauncherState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onQuickActionClick: (QuickAction) -> Unit,
    onRecommendedClick: (QuickAction) -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onToggleFavorites: (Boolean) -> Unit,
    onSortSelected: (AppSort) -> Unit,
    onRequestHomeRole: () -> Unit,
    onPinApp: (String) -> Unit,
    onUnpinApp: (String) -> Unit,
    onDeleteApp: (String) -> Unit,
    onGeminiApiKeyInputChange: (String) -> Unit,
    onSaveGeminiApiKey: () -> Unit,
    onClearGeminiApiKey: () -> Unit,
    onAiRefresh: () -> Unit,
    onAiAccept: (String) -> Unit,
    onAiDismiss: (String) -> Unit,
    onAiRestore: (String) -> Unit,
    discordSettingsContent: @Composable () -> Unit
) {
    KafkaLauncherTheme {
        LauncherNavHost(
            state = state,
            onSearchQueryChange = onSearchQueryChange,
            onClearSearch = onClearSearch,
            onQuickActionClick = onQuickActionClick,
            onRecommendedClick = onRecommendedClick,
            onAppClick = onAppClick,
            onToggleFavorites = onToggleFavorites,
            onSortSelected = onSortSelected,
            onRequestHomeRole = onRequestHomeRole,
            onPinApp = onPinApp,
            onUnpinApp = onUnpinApp,
            onDeleteApp = onDeleteApp,
            onGeminiApiKeyInputChange = onGeminiApiKeyInputChange,
            onSaveGeminiApiKey = onSaveGeminiApiKey,
            onClearGeminiApiKey = onClearGeminiApiKey,
            onAiRefresh = onAiRefresh,
            onAiAccept = onAiAccept,
            onAiDismiss = onAiDismiss,
            onAiRestore = onAiRestore,
            discordSettingsContent = discordSettingsContent
        )
    }
}
