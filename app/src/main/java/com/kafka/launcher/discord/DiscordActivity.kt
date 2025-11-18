package com.kafka.launcher.discord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.discord.DiscordInteractor
import com.kafka.launcher.domain.usecase.NormalizeDiscordDisplayNameUseCase
import com.kafka.launcher.domain.usecase.ParseDiscordChannelKeyUseCase
import com.kafka.launcher.launcher.DiscordProvider
import com.kafka.launcher.ui.discord.DiscordScreen
import com.kafka.launcher.ui.discord.DiscordViewModel
import com.kafka.launcher.ui.discord.DiscordViewModelFactory
import com.kafka.launcher.ui.theme.KafkaLauncherTheme

class DiscordActivity : ComponentActivity() {
    private val repository by lazy { DiscordProvider.create(applicationContext) }
    private val interactor by lazy {
        DiscordInteractor(
            repository = repository,
            parseChannelKey = ParseDiscordChannelKeyUseCase(),
            normalizeDisplayName = NormalizeDiscordDisplayNameUseCase()
        )
    }
    private val viewModel: DiscordViewModel by viewModels {
        DiscordViewModelFactory(interactor)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val initialUrl = intent?.getStringExtra(EXTRA_INITIAL_URL) ?: DiscordConfig.defaultAppUrl
        setContent {
            KafkaLauncherTheme {
                DiscordScreen(
                    viewModel = viewModel,
                    initialUrl = initialUrl,
                    onClose = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_INITIAL_URL = "extra.discord.initial_url"
    }
}
