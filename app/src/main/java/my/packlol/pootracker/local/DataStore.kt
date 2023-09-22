package my.packlol.pootracker.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStore(
    private val dataStore: DataStore<Preferences>,
    private val gson: Gson
) {
    private fun Preferences.getVersion(
        uid: String,
        gson: Gson
    ): Int {
        return this[versionKey]?.let { s ->
            gson.fromJson(s, SavedVersions::class.java).versions
                .find { it.uid == uid }
                ?.version
        }
            ?: -1
    }

    private fun Preferences.savedVersions(gson: Gson): SavedVersions {
        return this[versionKey]?.let { s ->
            gson.fromJson(s, SavedVersions::class.java)
        }
            ?: SavedVersions(emptyList())
    }

    private fun Preferences.savedUsers(gson: Gson): List<SavedUser> {
        return this[savedUsersKey]?.let {
            gson.fromJson(it, SavedUsers::class.java).users
        }
            ?: emptyList()
    }

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

    suspend fun updateSavedUsers(
        savedUsers: (prev: List<SavedUser>) -> List<SavedUser>
    ): List<SavedUser> {
        val updated = dataStore.edit { mutablePrefs ->
            val new = savedUsers(
                mutablePrefs.savedUsers(gson)
            )
            mutablePrefs[savedUsersKey] = gson.toJson(SavedUsers(new))
        }
        return updated.savedUsers(gson)
    }

    fun savedUsers(): Flow<List<SavedUser>> {
        return dataStore.data.map { prefs ->
            prefs.savedUsers(gson)
        }
    }

    suspend fun updateUserPrefs(
        userPrefs: (prev: UserPrefs) -> UserPrefs
    ): UserPrefs {
        val updated = dataStore.edit { mutablePrefs ->
            val new = userPrefs(
                UserPrefs(
                    theme = mutablePrefs[themeKey].toUserTheme(),
                    onboarded = mutablePrefs[onboardedKey] ?: false ,
                    useOffline = mutablePrefs[useOfflineKey] ?: false
                )
            )
            mutablePrefs.apply {
                this[onboardedKey] = new.onboarded
                this[themeKey] = new.theme.ordinal
                this[useOfflineKey] = new.useOffline
            }
        }
        return UserPrefs(
            theme = updated[themeKey].toUserTheme(),
            onboarded = updated[onboardedKey] ?: false,
            useOffline = updated[useOfflineKey] ?: false
        )
    }

    fun version(uid: String): Flow<Int> {
        return dataStore.data.map { prefs ->
            prefs.getVersion(uid, gson)
        }
    }

    suspend fun updateVersion(uid: String, version: Int): Int {
        return dataStore.edit { mutablePrefs ->
            gson.toJson(
                SavedVersions(
                    mutablePrefs.savedVersions(gson)
                        .versions
                        .filter { it.uid != uid } + Version(uid, version)
                )
            )
        }
            .getVersion(uid, gson)
    }

    fun lastUid(): Flow<String?> {
        return dataStore.data.map { prefs -> prefs[lastUidKey] }
    }

    suspend fun updateLastUid(uid: String): String? {
        return dataStore.edit { mutablePreferences ->
            mutablePreferences[lastUidKey] = uid
        }[lastUidKey]
    }

    companion object {
        val versionKey = stringPreferencesKey("version_key")
        val themeKey = intPreferencesKey("theme_key")
        val onboardedKey = booleanPreferencesKey("onboarded_key")
        val useOfflineKey = booleanPreferencesKey("use_offline_key")
        val savedUsersKey = stringPreferencesKey("saved_users_key")
        val lastUidKey = stringPreferencesKey("last_uid_key")
    }
}

enum class UserTheme {
    DarkTheme, LightTheme, DeviceTheme
}

data class SavedUsers(
    val users: List<SavedUser>
)

data class SavedVersions(
    val versions: List<Version>
)

data class Version(
    val uid: String,
    val version: Int,
)

data class SavedUser(
    val uid: String,
    val name: String,
)

data class UserPrefs(
    val theme: UserTheme = UserTheme.DeviceTheme,
    val onboarded: Boolean = false,
    val useOffline: Boolean = false,
)