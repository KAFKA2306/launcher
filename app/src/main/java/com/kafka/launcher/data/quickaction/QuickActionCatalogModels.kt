package com.kafka.launcher.data.quickaction

import com.kafka.launcher.config.LauncherConfig
import kotlinx.serialization.Serializable

@Serializable
data class QuickActionCatalog(
    val updatedAt: String = "",
    val entries: List<QuickActionCatalogEntry> = emptyList()
)

@Serializable
data class QuickActionCatalogEntry(
    val id: String,
    val label: String,
    val actionType: String,
    val data: String? = null,
    val packageName: String? = null,
    val providerId: String = LauncherConfig.aiQuickActionProviderId,
    val createdAt: String,
    val updatedAt: String,
    val timeWindows: List<String> = emptyList(),
    val usageCount: Long = 0,
    val acceptedCount: Long = 0,
    val dismissedCount: Long = 0,
    val priority: Int = LauncherConfig.aiQuickActionPriority
)
