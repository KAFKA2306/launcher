package com.kafka.launcher.data.repo

import android.content.Context
import com.kafka.launcher.domain.model.QuickAction
import com.kafka.launcher.quickactions.QuickActionProvider
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QuickActionRepository(
    private val context: Context,
    private val providers: List<QuickActionProvider>
) {
    private val quickActions = MutableStateFlow(emptyList<QuickAction>())

    init {
        quickActions.value = providers.flatMap { it.actions(context) }.sortedByDescending { it.priority }
    }

    fun observe(): StateFlow<List<QuickAction>> = quickActions

    fun filter(query: String): List<QuickAction> {
        if (query.isBlank()) return quickActions.value
        val lower = query.lowercase(Locale.getDefault())
        return quickActions.value.filter { it.label.lowercase(Locale.getDefault()).contains(lower) }
    }
}
