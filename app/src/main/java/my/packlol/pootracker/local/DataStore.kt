package my.packlol.pootracker.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStore(
    private val dataStore: DataStore<Preferences>
) {

    fun getVersion(): Flow<Int> {
        return dataStore.data.map { prefs ->
            prefs[versionKey] ?: -1
        }
            .flowOn(Dispatchers.IO)
    }

    suspend fun updateVersion(version: Int): Int {
        return dataStore.edit { mutablePrefs ->
            mutablePrefs[versionKey] = version
        }[versionKey] ?: -1
    }

    companion object {
        val versionKey = intPreferencesKey("version_key")
    }
}