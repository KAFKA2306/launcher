package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class DiscordModule : QuickActionProvider {
    override val id: String = "discord"

    override fun actions(context: Context): List<QuickAction> {
        val base = QuickAction(
            id = "discord_open",
            providerId = id,
            label = "Discord",
            actionType = ActionType.DISCORD_OPEN,
            packageName = LauncherConfig.discordPackageName,
            priority = 4
        )
        val shortcuts = LauncherConfig.discordShortcuts.map { shortcut ->
            QuickAction(
                id = shortcut.id,
                providerId = id,
                label = shortcut.label,
                actionType = ActionType.BROWSER_URL,
                data = shortcut.uri,
                packageName = LauncherConfig.discordPackageName,
                priority = shortcut.priority
            )
        }
        return listOf(base) + shortcuts
    }
}
