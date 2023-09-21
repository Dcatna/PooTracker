package my.packlol.pootracker.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import my.packlol.pootracker.local.PoopDao
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.local.UserPrefs
import my.packlol.pootracker.sync.FirebaseSyncManager

class PoopLogRepository(
    private val poopDao: PoopDao,
    private val poopSyncManager: FirebaseSyncManager,
    private val userPrefs: UserPrefs,
) {

    suspend fun updatePoopLog() = withContext(Dispatchers.IO) {

        poopDao.upsert(
            PoopLog(synced = false)
        )

        if (!userPrefs.useOffline) {
            poopSyncManager.requestSync()
        }
    }

    fun observeAllPoopLogs(): Flow<List<PoopLog>> {
        return poopDao.observeAll()
    }
}