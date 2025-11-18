package com.kafka.launcher.data.repo

import com.kafka.launcher.data.store.DiscordChannelStore
import com.kafka.launcher.data.store.DiscordNotificationPatternStore
import com.kafka.launcher.data.store.DiscordNotificationStore
import com.kafka.launcher.data.store.DiscordPreferencesStore
import com.kafka.launcher.data.store.DiscordUsageStore
import com.kafka.launcher.domain.model.ChannelUsageStats
import com.kafka.launcher.domain.model.DiscordChannel
import com.kafka.launcher.domain.model.DiscordChannelKey
import com.kafka.launcher.domain.model.DiscordPreferences
import com.kafka.launcher.domain.model.DiscordRankingWeights
import com.kafka.launcher.domain.model.DiscordNotificationRecord
import com.kafka.launcher.domain.model.MutedDisplayNameEntry
import com.kafka.launcher.domain.model.NotificationPatternRule
import com.kafka.launcher.domain.model.SelfDisplayNameEntry
import kotlinx.coroutines.flow.Flow

class DiscordRepository(
    private val preferencesStore: DiscordPreferencesStore,
    private val channelStore: DiscordChannelStore,
    private val usageStore: DiscordUsageStore,
    private val notificationStore: DiscordNotificationStore,
    private val patternStore: DiscordNotificationPatternStore
) {
    val preferences: Flow<DiscordPreferences> = preferencesStore.data
    val channels: Flow<List<DiscordChannel>> = channelStore.data
    val usageStats: Flow<List<ChannelUsageStats>> = usageStore.data
    val notifications: Flow<List<DiscordNotificationRecord>> = notificationStore.data
    val patternRules: Flow<List<NotificationPatternRule>> = patternStore.data

    suspend fun snapshotPreferences(): DiscordPreferences {
        return preferencesStore.snapshot()
    }

    suspend fun setMutedNames(entries: List<MutedDisplayNameEntry>) {
        preferencesStore.update { it.copy(mutedNames = entries) }
    }

    suspend fun setSelfNames(entries: List<SelfDisplayNameEntry>) {
        preferencesStore.update { it.copy(selfNames = entries) }
    }

    suspend fun setRankingWeights(weights: DiscordRankingWeights) {
        preferencesStore.update { it.copy(rankingWeights = weights) }
    }

    suspend fun setQuickAccessGuideDismissed(dismissed: Boolean) {
        preferencesStore.update { it.copy(quickAccessGuideDismissed = dismissed) }
    }

    suspend fun setChannels(channels: List<DiscordChannel>) {
        channelStore.update { channels }
    }

    suspend fun upsertChannel(channel: DiscordChannel) {
        channelStore.update { list ->
            val filtered = list.filterNot { it.id == channel.id }
            filtered + channel
        }
    }

    suspend fun setChannelFavorite(channelId: String, favorite: Boolean) {
        channelStore.update { list ->
            list.map { if (it.id == channelId) it.copy(favorite = favorite) else it }
        }
    }

    suspend fun incrementOpenCount(channelKey: DiscordChannelKey, timestamp: Long) {
        usageStore.update { stats -> stats.updateEntry(channelKey) { it.copy(openCount = it.openCount + 1, lastActiveAt = timestamp) } }
    }

    suspend fun incrementFocusSeconds(channelKey: DiscordChannelKey, seconds: Long, timestamp: Long) {
        usageStore.update { stats -> stats.updateEntry(channelKey) { it.copy(totalFocusSeconds = it.totalFocusSeconds + seconds, lastActiveAt = timestamp) } }
    }

    suspend fun incrementPostCount(channelKey: DiscordChannelKey, timestamp: Long) {
        usageStore.update { stats -> stats.updateEntry(channelKey) { it.copy(postCount = it.postCount + 1, lastActiveAt = timestamp) } }
    }

    suspend fun incrementNotificationCount(channelKey: DiscordChannelKey, timestamp: Long) {
        usageStore.update { stats -> stats.updateEntry(channelKey) { it.copy(notificationCount = it.notificationCount + 1, lastActiveAt = timestamp) } }
    }

    suspend fun appendNotification(record: DiscordNotificationRecord, limit: Int = 100) {
        notificationStore.update { list ->
            val updated = list + record
            val overflow = updated.size - limit
            if (overflow > 0) updated.drop(overflow) else updated
        }
    }

    suspend fun setNotificationMapping(notificationId: Long, channelId: String?) {
        notificationStore.update { list ->
            list.map { if (it.id == notificationId) it.copy(mappedChannelId = channelId) else it }
        }
    }

    suspend fun replaceNotifications(notifications: List<DiscordNotificationRecord>) {
        notificationStore.update { notifications }
    }

    suspend fun clearNotifications() {
        notificationStore.update { emptyList() }
    }

    suspend fun savePatternRule(rule: NotificationPatternRule) {
        patternStore.update { list ->
            val filtered = list.filterNot { it.id == rule.id }
            filtered + rule
        }
    }

    suspend fun deletePatternRule(ruleId: String) {
        patternStore.update { list -> list.filterNot { it.id == ruleId } }
    }

    suspend fun setPatternRules(rules: List<NotificationPatternRule>) {
        patternStore.update { rules }
    }

    private fun List<ChannelUsageStats>.updateEntry(
        channelKey: DiscordChannelKey,
        transform: (ChannelUsageStats) -> ChannelUsageStats
    ): List<ChannelUsageStats> {
        val targetId = channelKey.stableId
        val index = indexOfFirst { it.channelId == targetId }
        val current = if (index >= 0) get(index) else ChannelUsageStats(channelKey = channelKey)
        val updated = transform(current)
        return if (index >= 0) {
            toMutableList().apply { set(index, updated) }
        } else {
            this + updated
        }
    }
}
