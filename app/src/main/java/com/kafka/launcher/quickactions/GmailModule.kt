package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class GmailModule : QuickActionProvider {
    override val id: String = "gmail"

    override fun actions(context: Context): List<QuickAction> = listOf(
        QuickAction(
            id = "gmail_inbox",
            providerId = id,
            label = "未読を見る",
            actionType = ActionType.EMAIL_INBOX,
            packageName = "com.google.android.gm",
            priority = 3
        ),
        QuickAction(
            id = "gmail_compose",
            providerId = id,
            label = "メール作成",
            actionType = ActionType.EMAIL_COMPOSE,
            packageName = "com.google.android.gm",
            priority = 2
        )
    )
}
