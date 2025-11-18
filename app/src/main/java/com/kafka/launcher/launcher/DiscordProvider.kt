package com.kafka.launcher.launcher

import android.content.Context
import com.kafka.launcher.data.repo.DiscordRepository
import com.kafka.launcher.data.store.DiscordChannelStore
import com.kafka.launcher.data.store.DiscordNotificationPatternStore
import com.kafka.launcher.data.store.DiscordNotificationStore
import com.kafka.launcher.data.store.DiscordPreferencesStore
import com.kafka.launcher.data.store.DiscordUsageStore

object DiscordProvider {
    fun create(context: Context): DiscordRepository {
        val appContext = context.applicationContext
        return DiscordRepository(
            preferencesStore = DiscordPreferencesStore(appContext),
            channelStore = DiscordChannelStore(appContext),
            usageStore = DiscordUsageStore(appContext),
            notificationStore = DiscordNotificationStore(appContext),
            patternStore = DiscordNotificationPatternStore(appContext)
        )
    }
}
