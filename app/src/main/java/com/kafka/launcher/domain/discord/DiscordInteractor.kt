package com.kafka.launcher.domain.discord

import com.kafka.launcher.data.repo.DiscordRepository
import com.kafka.launcher.domain.model.DiscordChannel
import com.kafka.launcher.domain.model.DiscordChannelKey
import com.kafka.launcher.domain.model.DiscordNotificationRecord
import com.kafka.launcher.domain.model.DiscordPreferences
import com.kafka.launcher.domain.model.DiscordRankingWeights
import com.kafka.launcher.domain.model.MutedDisplayNameEntry
import com.kafka.launcher.domain.model.NotificationPatternRule
import com.kafka.launcher.domain.model.SelfDisplayNameEntry
import com.kafka.launcher.domain.usecase.NormalizeDiscordDisplayNameUseCase
import com.kafka.launcher.domain.usecase.ParseDiscordChannelKeyUseCase
import kotlinx.coroutines.flow.Flow

class DiscordInteractor(
    private val repository: DiscordRepository,
    private val parseChannelKey: ParseDiscordChannelKeyUseCase,
    private val normalizeDisplayName: NormalizeDiscordDisplayNameUseCase
) {
    val preferences: Flow<DiscordPreferences> = repository.preferences
    val channels: Flow<List<DiscordChannel>> = repository.channels
    val usage = repository.usageStats
    val notifications: Flow<List<DiscordNotificationRecord>> = repository.notifications
    val patternRules: Flow<List<NotificationPatternRule>> = repository.patternRules

    suspend fun addChannel(channel: DiscordChannel) {
        repository.upsertChannel(channel)
    }

    suspend fun setChannels(channels: List<DiscordChannel>) {
        repository.setChannels(channels)
    }

    suspend fun setFavorite(channelId: String, favorite: Boolean) {
        repository.setChannelFavorite(channelId, favorite)
    }

    suspend fun deleteChannel(channelId: String) {
        repository.deleteChannel(channelId)
    }

    suspend fun recordOpen(url: String, timestamp: Long) {
        val key = keyFromUrl(url) ?: return
        repository.incrementOpenCount(key, timestamp)
    }

    suspend fun recordFocus(url: String, seconds: Long, timestamp: Long) {
        val key = keyFromUrl(url) ?: return
        repository.incrementFocusSeconds(key, seconds, timestamp)
    }

    suspend fun recordPost(url: String, timestamp: Long) {
        val key = keyFromUrl(url) ?: return
        repository.incrementPostCount(key, timestamp)
    }

    suspend fun recordNotification(channelKey: DiscordChannelKey, timestamp: Long) {
        repository.incrementNotificationCount(channelKey, timestamp)
    }

    suspend fun appendNotification(record: DiscordNotificationRecord) {
        repository.appendNotification(record)
    }

    suspend fun mapNotification(notificationId: Long, channelId: String?) {
        repository.setNotificationMapping(notificationId, channelId)
    }

    suspend fun clearNotifications() {
        repository.clearNotifications()
    }

    suspend fun savePatternRule(rule: NotificationPatternRule) {
        repository.savePatternRule(rule)
    }

    suspend fun deletePatternRule(ruleId: String) {
        repository.deletePatternRule(ruleId)
    }

    suspend fun setMutedNames(raw: List<String>) {
        val entries = raw.map { item ->
            val canonical = normalizeDisplayName(item)
            MutedDisplayNameEntry(canonicalName = canonical, aliases = listOf(item))
        }
        repository.setMutedNames(entries)
    }

    suspend fun setSelfNames(raw: List<String>) {
        val entries = raw.map { item ->
            val canonical = normalizeDisplayName(item)
            SelfDisplayNameEntry(canonicalName = canonical, aliases = listOf(item))
        }
        repository.setSelfNames(entries)
    }

    suspend fun setRankingWeights(weights: DiscordRankingWeights) {
        repository.setRankingWeights(weights)
    }

    suspend fun dismissQuickAccessGuide() {
        repository.setQuickAccessGuideDismissed(true)
    }

    private fun keyFromUrl(url: String): DiscordChannelKey? {
        return parseChannelKey(url)
    }
}
