package com.kafka.launcher.domain.model

import com.kafka.launcher.config.LauncherConfig

data class Settings(
    val showFavorites: Boolean = true,
    val appSort: AppSort = LauncherConfig.defaultAppSort
)

enum class AppSort {
    NAME,
    USAGE
}
