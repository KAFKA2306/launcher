package com.kafka.launcher.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kafka.launcher.config.LauncherConfig
import com.kafka.launcher.domain.model.AppSort
import com.kafka.launcher.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val showFavoritesKey = booleanPreferencesKey("show_favorites")
    private val appSortKey = stringPreferencesKey("app_sort")

    val settings: Flow<Settings> = dataStore.data.map {
        val sort = it[appSortKey]?.let(AppSort::valueOf) ?: LauncherConfig.defaultAppSort
        Settings(
            showFavorites = it[showFavoritesKey] ?: true,
            appSort = sort
        )
    }

    suspend fun setShowFavorites(enabled: Boolean) {
        dataStore.edit { it[showFavoritesKey] = enabled }
    }

    suspend fun setAppSort(sort: AppSort) {
        dataStore.edit { it[appSortKey] = sort.name }
    }
}
