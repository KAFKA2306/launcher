package com.kafka.launcher.ui.discord.components

import android.webkit.JavascriptInterface

class DiscordJavascriptBridge(
    private val onChannelChanged: (String) -> Unit,
    private val onPostDetected: (String) -> Unit
) {
    @JavascriptInterface
    fun onChannelUrlChanged(url: String) {
        onChannelChanged(url)
    }

    @JavascriptInterface
    fun onPostDetected(url: String) {
        onPostDetected(url)
    }

    @JavascriptInterface
    fun onBootstrapReady() = Unit
}
