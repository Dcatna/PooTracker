package my.packlol.pootracker.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.OfflineDeleted
import my.packlol.pootracker.local.PoopCollection
import my.packlol.pootracker.local.PoopDao
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.sync.FirebaseSyncManager
import java.time.LocalDateTime
import java.util.UUID

class PoopLogRepository(
    private val poopDao: PoopDao,
    private val poopSyncManager: FirebaseSyncManager,
    private val dataStore: DataStore,
    private val authRepository: AuthRepository
) {
    private val defaultCollecitonId = "9b508294-1ec6-479b-9a08-9f0afdd0baad"

    init {
        CoroutineScope(Dispatchers.IO).launch {
            poopDao.getAllCollections().ifEmpty {
                poopDao.upsertCollection(
                    PoopCollection(
                        id = defaultCollecitonId,
                        name = "Default",
                        uid = null
                    )
                )
            }
        }
    }

    suspend fun deletePoopLog(id: String) = withContext(Dispatchers.IO) {
        val toDelete = poopDao.getById(id) ?: return@withContext false

        Log.d("PoopLog", "going to delete: $toDelete")

        poopDao.deleteById(toDelete.id)

        Log.d("PoopLog", "deleted: $toDelete")

        if (toDelete.synced) {
            poopDao.addOfflineDeletedLog(
                OfflineDeleted(
                    toDelete.id,
                    toDelete.collectionId,
                    toDelete.loggedAt,
                    toDelete.uid ?: ""
                )
            )
            poopSyncManager.requestSync(toDelete.collectionId)
        }
        true
    }

    suspend fun addPoopLog(
        id: String,
        uid: String?,
        time: LocalDateTime,
        synced: Boolean,
        collectionId: String,
    ) = withContext(Dispatchers.IO) {

        poopDao.upsert(
            PoopLog(
                uid = uid,
                collectionId = collectionId,
                synced = synced,
                loggedAt = time,
                id = id
            )
        )

        val offlineDeleted = poopDao.getOfflineDeletedById(id)

        if (offlineDeleted == null) {
            poopSyncManager.requestSync(collectionId)
        } else {
            poopDao.removeFromOfflineDelete(offlineDeleted)
        }
    }

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

    fun observeAllCollections(): Flow<List<PoopCollection>> {
        return poopDao.observeAllCollections()
    }

    fun observeAllPoopLogs(): Flow<List<PoopLog>> {
        return poopDao.observeAll()
    }
}