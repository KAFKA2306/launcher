package com.kafka.launcher.domain.model

data class NavigationInfo(
    val mode: NavigationMode = NavigationMode.GESTURE,
    val isOemRestricted: Boolean = false,
    val manufacturer: String = "",
    val brand: String = ""
)

enum class NavigationMode {
    GESTURE,
    THREE_BUTTON
}
