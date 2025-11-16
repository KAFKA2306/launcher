package com.kafka.launcher.data.quickaction

import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

fun QuickActionCatalogEntry.toQuickAction(): QuickAction? {
    val type = resolveActionType(actionType) ?: return null
    return QuickAction(
        id = id,
        providerId = providerId,
        label = label,
        actionType = type,
        data = data,
        packageName = packageName,
        priority = priority
    )
}

private fun resolveActionType(value: String): ActionType? {
    if (value.isBlank()) return null
    return ActionType.entries.firstOrNull { it.name == value }
}
