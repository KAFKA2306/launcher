package com.kafka.launcher.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kafka.launcher.config.LauncherConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PinnedAppsRepository(private val dataStore: DataStore<Preferences>) {
    private val pinnedKey = stringSetPreferencesKey(LauncherConfig.pinnedAppsKey)

    val pinnedApps: Flow<Set<String>> = dataStore.data.map { it[pinnedKey] ?: emptySet() }

    suspend fun pin(packageName: String) {
        dataStore.edit { prefs ->
            val updated = prefs[pinnedKey]?.toMutableSet() ?: mutableSetOf()
            updated.add(packageName)
            prefs[pinnedKey] = updated
        }
    }

    suspend fun unpin(packageName: String) {
        dataStore.edit { prefs ->
            val updated = prefs[pinnedKey]?.toMutableSet() ?: mutableSetOf()
            updated.remove(packageName)
            prefs[pinnedKey] = updated
        }
    }
}
