package com.kafka.launcher.data.repo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
            InstalledApp(
                packageName = it.activityInfo.packageName,
                componentName = component,
                label = label,
                icon = icon
            )
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    fun filter(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        if (query.isBlank()) return apps
        val lower = query.lowercase(Locale.getDefault())
        return apps.filter { it.label.lowercase(Locale.getDefault()).contains(lower) }
    }
}
