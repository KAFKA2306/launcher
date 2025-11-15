package com.kafka.launcher.data.repo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.quickactions.QuickActionIntentFactory
import com.kafka.launcher.quickactions.QuickActionProvider
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QuickActionRepository(
    private val context: Context,
    private val providers: List<QuickActionProvider>
) {
    private val quickActions = MutableStateFlow(emptyList<QuickAction>())
    private val intentFactory = QuickActionIntentFactory(context)
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
        refresh()
    }

    fun observe(): StateFlow<List<QuickAction>> = quickActions

    fun filter(query: String): List<QuickAction> {
        if (query.isBlank()) return quickActions.value
        val lower = query.lowercase(Locale.getDefault())
        return quickActions.value.filter { it.label.lowercase(Locale.getDefault()).contains(lower) }
    }

    private fun refresh() {
        quickActions.value = providers
            .flatMap { it.actions(context) }
            .filter { isAvailable(it) }
            .sortedByDescending { it.priority }
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
