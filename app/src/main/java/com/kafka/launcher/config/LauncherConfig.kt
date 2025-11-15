package com.kafka.launcher.config

object LauncherConfig {
    const val statsLimit = 50
    const val recommendationFallbackCount = 4
    const val favoritesLimit = 5
    const val navigationModeKey = "navigation_mode"
    const val navigationModeGestureValue = 2
    const val navigationModeThreeButtonValue = 0
    val gestureUnsupportedManufacturers = setOf("xiaomi", "redmi", "poco", "blackshark")
    const val appUsagePrefix = "app:"
}
