package my.packlol.pootracker.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.PoopDao
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.local.UserPrefs
import my.packlol.pootracker.sync.FirebaseSyncManager

class PoopLogRepository(
    private val poopDao: PoopDao,
    private val poopSyncManager: FirebaseSyncManager,
    private val dataStore: DataStore,
    private val authRepository: AuthRepository
) {

    suspend fun updatePoopLog() = withContext(Dispatchers.IO) {

        val useOffline = dataStore.userPrefs().first().useOffline

        val uid =  when {
            useOffline -> null
            authRepository.currentUser?.uid != null -> authRepository.currentUser?.uid
            else -> dataStore.lastUid().firstOrNull()
        }
        poopDao.upsert(
            PoopLog(
                uid = uid,
                synced = false
            )
        )

        if (!useOffline && uid != null) {
            poopSyncManager.requestSync(uid)
        }
    }

    fun observeAllPoopLogs(): Flow<List<PoopLog>> {
        return poopDao.observeAll()
    }
}