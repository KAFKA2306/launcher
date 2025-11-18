package com.kafka.launcher.domain.usecase

import com.kafka.launcher.domain.model.DiscordChannelKey
import java.net.URI

class ParseDiscordChannelKeyUseCase {
    operator fun invoke(url: String): DiscordChannelKey? {
        if (url.isEmpty()) return null
        val sanitized = url.substringBefore('#').substringBefore('?')
        val uri = URI.create(sanitized)
        val segments = uri.path.split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) return null
        if (segments.first() != "channels") return null
        val guildId = segments.getOrNull(1) ?: return null
        val channelId = segments.getOrNull(2) ?: return null
        val threadId = segments.getOrNull(3)
        return DiscordChannelKey(guildId = guildId, channelId = channelId, threadId = threadId)
    }
}
