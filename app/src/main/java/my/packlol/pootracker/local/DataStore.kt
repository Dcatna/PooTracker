package my.packlol.pootracker.local

import android.content.Context
import androidx.datastore.core.DataStore
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
    private fun getVersion(
        cid: String,
        prefValue: String?,
    ): Int {
        return prefValue?.let { s ->
            gson.fromJson(s, SavedVersions::class.java)
                .versions
                .find { it.cid == cid }
                ?.version
        }
            ?: -1
    }

    private fun savedVersions(prefValue: String?): SavedVersions {
        return prefValue?.let { s ->
            gson.fromJson(s, SavedVersions::class.java)
        }
            ?: SavedVersions(emptyList())
    }

    private fun Preferences.savedUsers(): List<SavedUser> {
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
                darkThemePreference = prefs[themeKey].toUserTheme(),
                onboarded = prefs[onboardedKey] ?: false,
                useOffline = prefs[useOfflineKey] ?: false,
                useDynamicTheme = prefs[useDynamicTheme] ?: false
            )
        }
    }

    suspend fun updateSavedUsers(
        savedUsers: (prev: List<SavedUser>) -> List<SavedUser>
    ): List<SavedUser> {
        val updated = dataStore.edit { mutablePrefs ->
            val new = savedUsers(
                mutablePrefs.savedUsers()
            )
            mutablePrefs[savedUsersKey] = gson.toJson(SavedUsers(new))
        }
        return updated.savedUsers()
    }

    fun savedUsers(): Flow<List<SavedUser>> {
        return dataStore.data.map { prefs ->
            prefs.savedUsers()
        }
    }

    suspend fun updateUserPrefs(
        userPrefs: (prev: UserPrefs) -> UserPrefs
    ): UserPrefs {
        val updated = dataStore.edit { mutablePrefs ->
            val new = userPrefs(
                UserPrefs(
                    darkThemePreference = mutablePrefs[themeKey].toUserTheme(),
                    onboarded = mutablePrefs[onboardedKey] ?: false,
                    useOffline = mutablePrefs[useOfflineKey] ?: false,
                    useDynamicTheme = mutablePrefs[useDynamicTheme] ?: false
                )
            )
            mutablePrefs.apply {
                this[onboardedKey] = new.onboarded
                this[themeKey] = new.darkThemePreference.ordinal
                this[useOfflineKey] = new.useOffline
                this[useDynamicTheme] = new.useDynamicTheme
            }
        }
        return UserPrefs(
            darkThemePreference = updated[themeKey].toUserTheme(),
            onboarded = updated[onboardedKey] ?: false,
            useOffline = updated[useOfflineKey] ?: false,
            useDynamicTheme = updated[useDynamicTheme] ?: false
        )
    }

    fun version(cid: String): Flow<Int> {
        return dataStore.data.map { prefs ->
            getVersion(cid, prefs[versionKey])
        }
    }

    suspend fun updateVersion(
        collectionId: String,
        version: Int
    ): Int {
        val updated = dataStore.edit { mutablePrefs ->
            mutablePrefs[versionKey] = gson.toJson(
                SavedVersions(
                    savedVersions(mutablePrefs[versionKey])
                        .versions
                        .filter { it.cid != collectionId } + Version(collectionId, version)
                )
            )
        }
        return getVersion(collectionId, updated[versionKey])
    }

    fun lastUid(): Flow<String?> {
        return dataStore.data.map { prefs -> prefs[lastUidKey]?.ifEmpty { null } }
    }

    suspend fun updateLastUid(uid: String?): String? {
        return dataStore.edit { mutablePreferences ->
            mutablePreferences[lastUidKey] = uid ?: ""
        }[lastUidKey]
    }

    companion object {
        val versionKey = stringPreferencesKey("version_key")
        val themeKey = intPreferencesKey("theme_key")
        val onboardedKey = booleanPreferencesKey("onboarded_key")
        val useOfflineKey = booleanPreferencesKey("use_offline_key")
        val savedUsersKey = stringPreferencesKey("saved_users_key")
        val lastUidKey = stringPreferencesKey("last_uid_key")
        val useDynamicTheme = booleanPreferencesKey("use_dynamic_key")
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
    val cid: String,
    val version: Int,
)

data class SavedUser(
    val uid: String,
    val name: String,
)

data class UserPrefs(
    val darkThemePreference: UserTheme = UserTheme.DeviceTheme,
    val onboarded: Boolean = false,
    val useOffline: Boolean = false,
    val useDynamicTheme: Boolean = false
)