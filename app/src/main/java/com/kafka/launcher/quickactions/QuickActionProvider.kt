package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.domain.model.QuickAction

interface QuickActionProvider {
    val id: String
    fun actions(context: Context): List<QuickAction>
}
