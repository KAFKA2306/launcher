package com.kafka.launcher.ui.discord.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kafka.launcher.config.DiscordConfig

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun DiscordWebView(
    url: String,
    reloadSignal: Int,
    onUrlChanged: (String) -> Unit,
    onPostDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bootstrapScript = remember(context) { loadBootstrapScript(context) }
    val bridge = remember(onUrlChanged, onPostDetected) {
        DiscordJavascriptBridge(onChannelChanged = onUrlChanged, onPostDetected = onPostDetected)
    }
    var appliedReloadSignal by remember { mutableStateOf(reloadSignal) }
    val webView = remember {
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            addJavascriptInterface(bridge, "DiscordBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                    if (bootstrapScript.isNotBlank()) {
                        view?.evaluateJavascript(bootstrapScript, null)
                    }
                    finishedUrl?.let(onUrlChanged)
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { view ->
            if (url.isNotBlank() && url != view.url) {
                view.loadUrl(url)
            }
            if (reloadSignal != appliedReloadSignal) {
                view.reload()
                appliedReloadSignal = reloadSignal
            }
        }
    )
}

private fun loadBootstrapScript(context: Context): String {
    return runCatching {
        context.assets.open(DiscordConfig.bootstrapAssetPath).bufferedReader().use { it.readText() }
    }.getOrElse { "" }
}
