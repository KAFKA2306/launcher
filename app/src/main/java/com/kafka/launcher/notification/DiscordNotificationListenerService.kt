package com.kafka.launcher.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.kafka.launcher.config.DiscordConfig
import com.kafka.launcher.domain.model.DiscordNotificationRecord
import com.kafka.launcher.launcher.DiscordProvider

class DiscordNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { DiscordProvider.create(applicationContext) }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != DiscordConfig.packageName) return
        val record = sbn.toDiscordNotificationRecord() ?: return
        scope.launch {
            repository.appendNotification(record)
        }
    }

    private fun StatusBarNotification.toDiscordNotificationRecord(): DiscordNotificationRecord? {
        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        if (title.isBlank()) return null
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val recordId = postTime + id.toLong()
        return DiscordNotificationRecord(
            id = recordId,
            timestamp = postTime,
            title = title,
            subText = subText,
            text = text,
            mappedChannelId = null
        )
    }
}
