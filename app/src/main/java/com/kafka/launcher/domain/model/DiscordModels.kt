package com.kafka.launcher.domain.model

enum class DiscordChannelType {
    CHANNEL,
    THREAD
}

data class DiscordChannelKey(
    val guildId: String,
    val channelId: String,
    val threadId: String? = null
) {
    val stableId: String = if (threadId.isNullOrEmpty()) "$guildId:$channelId" else "$guildId:$channelId:$threadId"
    val path: String = if (threadId.isNullOrEmpty()) "channels/$guildId/$channelId" else "channels/$guildId/$channelId/$threadId"
}

data class DiscordChannel(
    val key: DiscordChannelKey,
    val id: String = key.stableId,
    val type: DiscordChannelType = DiscordChannelType.CHANNEL,
    val url: String,
    val label: String,
    val serverName: String = "",
    val categoryName: String? = null,
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false
)

data class MutedDisplayNameEntry(
    val canonicalName: String,
    val aliases: List<String>
)

data class SelfDisplayNameEntry(
    val canonicalName: String,
    val aliases: List<String>
)

data class DiscordNotificationRecord(
    val id: Long,
    val timestamp: Long,
    val title: String,
    val subText: String?,
    val text: String?,
    val mappedChannelId: String?
)

data class NotificationPatternRule(
    val id: String,
    val titleRegex: String?,
    val subTextRegex: String?,
    val targetChannelId: String
)

data class ChannelUsageStats(
    val channelKey: DiscordChannelKey,
    val channelId: String = channelKey.stableId,
    val openCount: Long = 0,
    val totalFocusSeconds: Long = 0,
    val postCount: Long = 0,
    val notificationCount: Long = 0,
    val lastActiveAt: Long = 0
)

data class DiscordRankingWeights(
    val open: Int,
    val focus: Int,
    val post: Int,
    val notification: Int
) {
    companion object {
        val Default = DiscordRankingWeights(open = 1, focus = 1, post = 3, notification = 1)
    }
}

data class DiscordPreferences(
    val mutedNames: List<MutedDisplayNameEntry> = emptyList(),
    val selfNames: List<SelfDisplayNameEntry> = emptyList(),
    val rankingWeights: DiscordRankingWeights = DiscordRankingWeights.Default,
    val quickAccessGuideDismissed: Boolean = false
)
