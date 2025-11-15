package com.kafka.launcher.quickactions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
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
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(base + query)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!packageName.isNullOrBlank()) intent.`package` = packageName
        return if (intent.resolveActivity(packageManager) != null) intent else null
    }

    private fun openUrl(url: String, packageName: String?): Intent? {
        if (url.isBlank()) return null
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!packageName.isNullOrBlank()) intent.`package` = packageName
        return if (intent.resolveActivity(packageManager) != null) intent else null
    }

    private fun openCalendar(packageName: String?): Intent? {
        val intent = Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!packageName.isNullOrBlank()) intent.`package` = packageName
        return if (intent.resolveActivity(packageManager) != null) intent else null
    }

    private fun insertCalendar(packageName: String?): Intent? {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!packageName.isNullOrBlank()) intent.`package` = packageName
        return if (intent.resolveActivity(packageManager) != null) intent else null
    }

    private fun composeEmail(packageName: String?): Intent? {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!packageName.isNullOrBlank()) intent.`package` = packageName
        return if (intent.resolveActivity(packageManager) != null) intent else null
    }
}
