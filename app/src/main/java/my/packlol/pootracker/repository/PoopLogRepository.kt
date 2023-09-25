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
import java.util.UUID

class PoopLogRepository(
    private val poopDao: PoopDao,
    private val poopSyncManager: FirebaseSyncManager,
    private val dataStore: DataStore,
    private val authRepository: AuthRepository
) {

    suspend fun updatePoopLog(
        collectionId: UUID
    ) = withContext(Dispatchers.IO) {

        val useOffline = dataStore.userPrefs().first().useOffline

        val uid =  when {
            useOffline -> null
            else -> authRepository.currentUser?.uid ?: dataStore.lastUid().firstOrNull()
        }

        poopDao.upsert(
            PoopLog(
                uid = uid,
                collectionId = collectionId.toString(),
                synced = false
            )
        )

        if (!useOffline && uid != null) {
            poopSyncManager.requestSync(collectionId.toString())
        }
    }

    fun observeAllPoopLogs(): Flow<List<PoopLog>> {
        return poopDao.observeAll()
    }
}