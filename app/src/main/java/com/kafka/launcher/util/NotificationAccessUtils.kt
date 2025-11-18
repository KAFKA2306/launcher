package com.kafka.launcher.util

import android.content.Context
import androidx.core.app.NotificationManagerCompat

fun Context.hasNotificationListenerAccess(): Boolean {
    val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
    return enabledPackages.contains(packageName)
}
