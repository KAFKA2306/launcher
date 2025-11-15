package com.kafka.launcher.data.repo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.kafka.launcher.domain.model.AppCategory
import com.kafka.launcher.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AppRepository(private val context: Context) {
    suspend fun loadApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pm = context.packageManager
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        resolved.map {
            val label = it.loadLabel(pm).toString()
            val icon = it.loadIcon(pm)
            val component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
            val category = resolveCategory(it.activityInfo.applicationInfo.category)
            InstalledApp(
                packageName = it.activityInfo.packageName,
                componentName = component,
                label = label,
                icon = icon,
                category = category
            )
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun filter(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        if (query.isBlank()) return apps
        val lower = query.lowercase(Locale.getDefault())
        return apps.filter { it.label.lowercase(Locale.getDefault()).contains(lower) }
    }

    private fun resolveCategory(code: Int): AppCategory {
        return when (code) {
            ApplicationInfo.CATEGORY_SOCIAL,
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.COMMUNICATION
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.WORK
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.MEDIA
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.TRAVEL
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.TOOLS
            else -> AppCategory.OTHER
        }
    }
}
