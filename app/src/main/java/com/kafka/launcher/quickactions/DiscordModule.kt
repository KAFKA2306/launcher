package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class DiscordModule : QuickActionProvider {
    override val id: String = "discord"

    override fun actions(context: Context): List<QuickAction> = listOf(
        QuickAction(
            id = "discord_open",
            providerId = id,
            label = "Discord",
            actionType = ActionType.DISCORD_OPEN,
            packageName = "com.discord",
            priority = 2
        )
    )
}
