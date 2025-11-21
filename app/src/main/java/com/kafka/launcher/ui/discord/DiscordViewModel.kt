package com.kafka.launcher.ui.discord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kafka.launcher.domain.discord.DiscordInteractor
import com.kafka.launcher.domain.model.DiscordChannel
import com.kafka.launcher.domain.model.DiscordNotificationRecord
import com.kafka.launcher.domain.model.DiscordRankingWeights
import com.kafka.launcher.domain.model.NotificationPatternRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiscordViewModel(
    private val interactor: DiscordInteractor
) : ViewModel() {

    val preferences = interactor.preferences
    val channels = interactor.channels
    val usage = interactor.usage
    val notifications = interactor.notifications
    val patternRules = interactor.patternRules

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun openChannel(channel: DiscordChannel) {
        viewModelScope.launch {
            interactor.recordOpen(channel.url, System.currentTimeMillis())
        }
    }

    fun recordOpenUrl(url: String) {
        if (url.isEmpty()) return
        viewModelScope.launch {
            interactor.recordOpen(url, System.currentTimeMillis())
        }
    }

    fun recordFocus(url: String, seconds: Long) {
        viewModelScope.launch {
            interactor.recordFocus(url, seconds, System.currentTimeMillis())
        }
    }

    fun recordPost(url: String) {
        viewModelScope.launch {
            interactor.recordPost(url, System.currentTimeMillis())
        }
    }

    fun setFavorite(channelId: String, favorite: Boolean) {
        viewModelScope.launch {
            interactor.setFavorite(channelId, favorite)
        }
    }

    fun renameChannel(channel: DiscordChannel, newLabel: String) {
        viewModelScope.launch {
            interactor.addChannel(channel.copy(label = newLabel))
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            interactor.deleteChannel(channelId)
        }
    }

    fun saveMuted(entries: List<String>) {
        viewModelScope.launch {
            interactor.setMutedNames(entries)
        }
    }

    fun saveSelf(entries: List<String>) {
        viewModelScope.launch {
            interactor.setSelfNames(entries)
        }
    }

    fun saveWeights(weights: DiscordRankingWeights) {
        viewModelScope.launch {
            interactor.setRankingWeights(weights)
        }
    }

    fun addChannel(channel: DiscordChannel) {
        viewModelScope.launch {
            interactor.addChannel(channel)
        }
    }

    fun dismissQuickAccessGuide() {
        viewModelScope.launch {
            interactor.dismissQuickAccessGuide()
        }
    }

    fun savePattern(rule: NotificationPatternRule) {
        viewModelScope.launch {
            interactor.savePatternRule(rule)
        }
    }

    fun deletePattern(ruleId: String) {
        viewModelScope.launch {
            interactor.deletePatternRule(ruleId)
        }
    }

    fun mapNotification(record: DiscordNotificationRecord, channelId: String?) {
        viewModelScope.launch {
            interactor.mapNotification(record.id, channelId)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            interactor.clearNotifications()
        }
    }
}
