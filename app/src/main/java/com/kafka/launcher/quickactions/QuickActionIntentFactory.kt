package com.kafka.launcher.quickactions

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.core.net.toUri
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class QuickActionIntentFactory(private val context: Context) {
    private val packageManager = context.packageManager

    fun build(action: QuickAction, query: String): Intent? {
        return when (action.actionType) {
            ActionType.OPEN_APP,
            ActionType.EMAIL_INBOX,
            ActionType.DISCORD_OPEN -> launchPackage(action.packageName)
            ActionType.WEB_SEARCH -> openUrl((action.data ?: "") + query, action.packageName)
            ActionType.MAP_VIEW -> openUri(action.data, query, action.packageName)
            ActionType.MAP_NAVIGATION -> openUri(action.data, query, action.packageName)
            ActionType.CALENDAR_VIEW -> openCalendar(action.packageName)
            ActionType.CALENDAR_INSERT -> insertCalendar(action.packageName)
            ActionType.EMAIL_COMPOSE -> composeEmail(action.packageName)
            ActionType.BROWSER_URL -> openUrl(action.data ?: "", action.packageName)
        }
    }

    private fun launchPackage(packageName: String?): Intent? {
        if (packageName.isNullOrBlank()) return null
        return packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun openUri(base: String?, query: String, packageName: String?): Intent? {
        if (base.isNullOrBlank()) return null
        val intent = Intent(Intent.ACTION_VIEW, (base + query).toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return preferPackage(intent, packageName)
    }

    private fun openUrl(url: String, packageName: String?): Intent? {
        if (url.isBlank()) return null
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return preferPackage(intent, packageName)
    }

    private fun openCalendar(packageName: String?): Intent? {
        val intent = Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return preferPackage(intent, packageName)
    }

    private fun insertCalendar(packageName: String?): Intent? {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return preferPackage(intent, packageName)
    }

    private fun composeEmail(packageName: String?): Intent? {
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return preferPackage(intent, packageName)
    }

    private fun preferPackage(intent: Intent, packageName: String?): Intent? {
        if (packageName.isNullOrBlank()) return intent.takeIf { isResolvable(it) }
        val targeted = Intent(intent).setPackage(packageName)
        return targeted.takeIf { isResolvable(it) }
    }

    private fun isResolvable(intent: Intent) = intent.resolveActivity(packageManager) != null
}
