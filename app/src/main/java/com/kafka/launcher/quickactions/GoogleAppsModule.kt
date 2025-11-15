package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.QuickAction

class GoogleAppsModule : QuickActionProvider {
    override val id: String = "google_apps"

    override fun actions(context: Context): List<QuickAction> = LauncherConfig.googleQuickActions.map {
        QuickAction(
            id = it.id,
            providerId = id,
            label = it.label,
            actionType = it.actionType,
            data = it.data,
            packageName = it.packageName,
            priority = it.priority
        )
    }
}
