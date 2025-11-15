package com.kafka.launcher.domain.model

data class Settings(
    val showFavorites: Boolean = true,
    val appSort: AppSort = AppSort.NAME
)

enum class AppSort {
    NAME,
    USAGE
}
