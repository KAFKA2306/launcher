package com.kafka.launcher.data.repo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.kafka.launcher.data.log.QuickActionAuditLogger
import com.kafka.launcher.data.quickaction.QuickActionCatalog
import com.kafka.launcher.data.quickaction.QuickActionCatalogStore
import com.kafka.launcher.data.quickaction.toQuickAction
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.quickactions.QuickActionIntentFactory
import com.kafka.launcher.quickactions.QuickActionProvider
import java.util.Locale
import kotlin.jvm.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

class QuickActionRepository(
    private val context: Context,
    private val providers: List<QuickActionProvider>,
    private val logger: QuickActionAuditLogger,
    private val catalogStore: QuickActionCatalogStore
) {
    private val quickActions = MutableStateFlow(emptyList<QuickAction>())
    private val intentFactory = QuickActionIntentFactory(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var catalogSnapshot: QuickActionCatalog = catalogStore.snapshot()
    private val packageFilter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addDataScheme("package")
    }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    init {
        registerPackageReceiver()
        scope.launch {
            catalogStore.data.collect { catalog ->
                catalogSnapshot = catalog
                refresh()
            }
        }
        refresh()
    }

    fun observe(): StateFlow<List<QuickAction>> = quickActions

    fun filter(query: String): List<QuickAction> {
        if (query.isBlank()) return quickActions.value
        val lower = query.lowercase(Locale.getDefault())
        return quickActions.value.filter { it.label.lowercase(Locale.getDefault()).contains(lower) }
    }

    fun refresh() {
        scope.launch {
            val actions = buildQuickActions()
            quickActions.value = actions
            logger.writeSnapshot(actions)
        }
    }

    private fun buildQuickActions(): List<QuickAction> {
        val staticActions = providers
            .flatMap { it.actions(context) }
            .filter { isAvailable(it) }
        val aiActions = catalogSnapshot.entries
            .asSequence()
            .filter { it.dismissedCount == 0L }
            .mapNotNull { it.toQuickAction() }
            .filter { isAvailable(it) }
            .map { it.copy(priority = it.priority + acceptedBonus(it.id)) }
            .toList()
        return (staticActions + aiActions)
            .distinctBy { it.id }
            .sortedByDescending { it.priority }
    }

    private fun acceptedBonus(id: String): Int {
        val entry = catalogSnapshot.entries.firstOrNull { it.id == id } ?: return 0
        return entry.acceptedCount.toInt()
    }

    private fun isAvailable(action: QuickAction): Boolean {
        return intentFactory.build(action, "") != null
    }

    @Suppress("DEPRECATION")
    private fun registerPackageReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, packageFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(packageReceiver, packageFilter)
        }
    }
}
