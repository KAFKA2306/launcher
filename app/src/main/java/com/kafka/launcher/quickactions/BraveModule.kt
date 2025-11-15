package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class BraveModule : QuickActionProvider {
    override val id: String = "brave"

    override fun actions(context: Context): List<QuickAction> = listOf(
        QuickAction(
            id = "brave_open",
            providerId = id,
            label = "Brave",
            actionType = ActionType.OPEN_APP,
            packageName = "com.brave.browser",
            priority = 2
        ),
        QuickAction(
            id = "brave_search",
            providerId = id,
            label = "Brave検索",
            actionType = ActionType.WEB_SEARCH,
            data = "https://search.brave.com/search?q=",
            packageName = "com.brave.browser",
            priority = 3
        ),
        QuickAction(
            id = "brave_x",
            providerId = id,
            label = "X",
            actionType = ActionType.BROWSER_URL,
            data = "https://x.com",
            packageName = "com.brave.browser",
            priority = 1
        ),
        QuickAction(
            id = "brave_perplexity",
            providerId = id,
            label = "Perplexity",
            actionType = ActionType.BROWSER_URL,
            data = "https://www.perplexity.ai",
            packageName = "com.brave.browser",
            priority = 1
        ),
        QuickAction(
            id = "brave_vrchat",
            providerId = id,
            label = "VRChat",
            actionType = ActionType.BROWSER_URL,
            data = "https://hello.vrchat.com",
            packageName = "com.brave.browser",
            priority = 1
        ),
        QuickAction(
            id = "brave_codex",
            providerId = id,
            label = "Codex",
            actionType = ActionType.BROWSER_URL,
            data = "https://openai.com/blog/openai-codex",
            packageName = "com.brave.browser",
            priority = 1
        ),
        QuickAction(
            id = "brave_github",
            providerId = id,
            label = "GitHub",
            actionType = ActionType.BROWSER_URL,
            data = "https://github.com/KAFKA2306",
            packageName = "com.brave.browser",
            priority = 1
        )
    )
}
