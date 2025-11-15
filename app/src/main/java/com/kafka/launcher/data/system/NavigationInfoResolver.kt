package com.kafka.launcher.data.system

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.NavigationInfo
import com.kafka.launcher.domain.model.NavigationMode
import java.util.Locale

class NavigationInfoResolver(private val context: Context) {
    fun resolve(): NavigationInfo {
        val manufacturer = Build.MANUFACTURER
        val brand = Build.BRAND
        val lowerManufacturer = manufacturer.lowercase(Locale.getDefault())
        val lowerBrand = brand.lowercase(Locale.getDefault())
        val restricted = LauncherConfig.gestureUnsupportedManufacturers.any {
            lowerManufacturer.contains(it) || lowerBrand.contains(it)
        }
        val navValue = Settings.Secure.getInt(
            context.contentResolver,
            LauncherConfig.navigationModeKey,
            LauncherConfig.navigationModeThreeButtonValue
        )
        val mode = if (!restricted && navValue == LauncherConfig.navigationModeGestureValue) {
            NavigationMode.GESTURE
        } else {
            NavigationMode.THREE_BUTTON
        }
        return NavigationInfo(mode = mode, isOemRestricted = restricted, manufacturer = manufacturer, brand = brand)
    }
}
