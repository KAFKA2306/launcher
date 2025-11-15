package com.kafka.launcher.quickactions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class QuickActionExecutor(private val context: Context) {
    fun execute(action: QuickAction, query: String) {
        when (action.actionType) {
            ActionType.OPEN_APP -> launchPackage(action.packageName)
            ActionType.WEB_SEARCH -> openUrl((action.data ?: "") + query, action.packageName)
            ActionType.MAP_VIEW -> openUri(action.data, query, action.packageName)
            ActionType.MAP_NAVIGATION -> openUri(action.data, query, action.packageName)
            ActionType.CALENDAR_VIEW -> openCalendar(action.packageName)
            ActionType.CALENDAR_INSERT -> insertCalendar(action.packageName)
            ActionType.EMAIL_INBOX -> launchPackage(action.packageName)
            ActionType.EMAIL_COMPOSE -> composeEmail(action.packageName)
            ActionType.DISCORD_OPEN -> launchPackage(action.packageName)
            ActionType.BROWSER_URL -> openUrl(action.data ?: "", action.packageName)
        }
    }

    private fun launchPackage(packageName: String?) {
        if (packageName == null) return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openUri(base: String?, query: String, packageName: String?) {
        val value = base ?: ""
        val uri = Uri.parse(value + query)
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (packageName != null) intent.`package` = packageName
        context.startActivity(intent)
    }

    private fun openUrl(url: String, packageName: String?) {
        if (url.isEmpty()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (packageName != null) intent.`package` = packageName
        context.startActivity(intent)
    }

    private fun openCalendar(packageName: String?) {
        val intent = Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (packageName != null) intent.`package` = packageName
        context.startActivity(intent)
    }

    private fun insertCalendar(packageName: String?) {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (packageName != null) intent.`package` = packageName
        context.startActivity(intent)
    }

    private fun composeEmail(packageName: String?) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (packageName != null) intent.`package` = packageName
        context.startActivity(intent)
    }
}
