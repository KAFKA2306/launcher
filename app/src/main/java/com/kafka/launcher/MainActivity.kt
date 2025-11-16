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
import com.kafka.launcher.data.local.datastore.settingsDataStore
import com.kafka.launcher.data.local.db.KafkaDatabase
import com.kafka.launcher.data.log.ActionLogFileWriter
import com.kafka.launcher.data.log.QuickActionAuditLogger
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
import com.kafka.launcher.domain.usecase.RecommendActionsUseCase
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
import com.kafka.launcher.ui.theme.KafkaLauncherTheme

class MainActivity : ComponentActivity() {
    private val auditLogger by lazy { QuickActionAuditLogger(applicationContext) }
    private val actionLogFileWriter by lazy { ActionLogFileWriter(applicationContext) }
    private val geminiStore by lazy { GeminiRecommendationStore(applicationContext) }
    private val geminiApiKeyStore by lazy { GeminiApiKeyStore(applicationContext) }
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
                auditLogger
            ),
            actionLogRepository = ActionLogRepository(database.actionLogDao(), actionLogFileWriter),
            settingsRepository = SettingsRepository(appContext.settingsDataStore),
            pinnedAppsRepository = PinnedAppsRepository(appContext.settingsDataStore),
            recommendActionsUseCase = RecommendActionsUseCase(),
            navigationInfo = navigationInfo,
            geminiRecommendationStore = geminiStore,
            geminiApiKeyStore = geminiApiKeyStore
        )
        ViewModelProvider(this, factory)[LauncherViewModel::class.java]
    }

    private val quickActionExecutor by lazy { QuickActionExecutor(this, auditLogger) }
    private val uninstallLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        launcherViewModel.refreshApps()
    }
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestHomeRole()
        GeminiWorkScheduler.schedule(applicationContext)
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
                onClearGeminiApiKey = launcherViewModel::clearGeminiApiKey
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
    onClearGeminiApiKey: () -> Unit
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
            onClearGeminiApiKey = onClearGeminiApiKey
        )
    }
}
