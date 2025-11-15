package com.kafka.launcher.quickactions

import android.content.Context
import com.kafka.launcher.domain.model.ActionType
import com.kafka.launcher.domain.model.QuickAction

class GoogleMapsModule : QuickActionProvider {
    override val id: String = "google_maps"

    override fun actions(context: Context): List<QuickAction> = listOf(
        QuickAction(
            id = "maps_open",
            providerId = id,
            label = "マップを開く",
            actionType = ActionType.MAP_VIEW,
            data = "geo:0,0?q=",
            packageName = "com.google.android.apps.maps",
            priority = 3
        ),
        QuickAction(
            id = "maps_navi",
            providerId = id,
            label = "ナビ開始",
            actionType = ActionType.MAP_NAVIGATION,
            data = "google.navigation:q=",
            packageName = "com.google.android.apps.maps",
            priority = 2
        )
    )
}
