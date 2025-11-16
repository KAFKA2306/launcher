package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.data.log.QuickActionAuditLogger
import com.kafka.launcher.data.quickaction.QuickActionCatalogStore
import com.kafka.launcher.domain.model.QuickAction

class QuickActionExecutor(
    context: Context,
    private val logger: QuickActionAuditLogger,
    private val catalogStore: QuickActionCatalogStore
) {
    private val appContext = context.applicationContext
    private val intentFactory = QuickActionIntentFactory(appContext)

    fun execute(action: QuickAction, query: String) {
        val intent = intentFactory.build(action, query) ?: return
        if (action.providerId == LauncherConfig.aiQuickActionProviderId || action.id.startsWith(LauncherConfig.aiQuickActionPrefix)) {
            catalogStore.incrementUsage(action.id)
        }
        logger.logExecution(action, query)
        appContext.startActivity(intent)
    }
}
