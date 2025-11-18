package com.kafka.launcher.ui.discord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kafka.launcher.domain.discord.DiscordInteractor

class DiscordViewModelFactory(
    private val interactor: DiscordInteractor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscordViewModel::class.java)) {
            return DiscordViewModel(interactor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
