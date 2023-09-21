package my.packlol.pootracker.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStore(
    private val dataStore: DataStore<Preferences>
) {
    private fun Int?.toUserTheme(): UserTheme {
        return UserTheme.values().getOrElse(this ?: -1) { UserTheme.DeviceTheme }
    }

    fun userPrefs(): Flow<UserPrefs> {
        return dataStore.data.map { prefs ->
            UserPrefs(
                theme = prefs[themeKey].toUserTheme(),
                onboarded = prefs[onboardedKey] ?: false,
                useOffline = prefs[useOfflineKey] ?: false
            )
        }
    }

    suspend fun updateUserPrefs(userPrefs: UserPrefs): UserPrefs {
        val updated = dataStore.edit { mutablePrefs ->
            mutablePrefs.apply {
                this[onboardedKey] = userPrefs.onboarded
                this[themeKey] = userPrefs.theme.ordinal
                this[useOfflineKey] = userPrefs.useOffline
            }
        }
        return UserPrefs(
            theme = updated[themeKey].toUserTheme(),
            onboarded = updated[onboardedKey] ?: false,
            useOffline = updated[useOfflineKey] ?: false
        )
    }

    fun version(): Flow<Int> {
        return dataStore.data.map { prefs ->
            prefs[versionKey] ?: -1
        }
    }

    suspend fun updateVersion(version: Int): Int {
        return dataStore.edit { mutablePrefs ->
            mutablePrefs[versionKey] = version
        }[versionKey] ?: -1
    }

    companion object {
        val versionKey = intPreferencesKey("version_key")
        val themeKey = intPreferencesKey("theme_key")
        val onboardedKey = booleanPreferencesKey("onboarded_key")
        val useOfflineKey = booleanPreferencesKey("use_offline_key")
    }
}

enum class UserTheme {
    DarkTheme, LightTheme, DeviceTheme
}

data class UserPrefs(
    val theme: UserTheme = UserTheme.DeviceTheme,
    val onboarded: Boolean = false,
    val useOffline: Boolean = false,
)