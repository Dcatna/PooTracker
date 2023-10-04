package my.packlol.pootracker.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.packlol.pootracker.firebase.PoopApi
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.OfflineDeleteLog
import my.packlol.pootracker.local.OfflineDeletedCollection
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
    private val authRepository: AuthRepository,
    private val poopApi: PoopApi
) {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            poopDao.getAllCollections().ifEmpty {
                poopDao.upsertCollection(
                    PoopCollection(
                        id = UUID.randomUUID().toString(),
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

        if (toDelete.synced && toDelete.uid != null) {
            poopDao.addOfflineDeletedLog(
                OfflineDeleteLog(
                    toDelete.id,
                    toDelete.collectionId,
                    toDelete.loggedAt,
                    toDelete.uid
                )
            )
            poopSyncManager.requestSync(toDelete.collectionId)
        }
        true
    }

    suspend fun undoDeletePoopLog(
        id: String,
        uid: String?,
        time: LocalDateTime,
        collectionId: String,
    ) = withContext(Dispatchers.IO) {

        poopDao.upsert(
            PoopLog(
                uid = uid,
                collectionId = collectionId,
                synced = false,
                loggedAt = time,
                id = id
            )
        )

        poopDao.getOfflineDeletedById(id)?.let {
            poopDao.removeFromOfflineDelete(it)
        }

        poopSyncManager.requestSync(collectionId)
    }

    suspend fun updatePoopLog(
        collectionId: String,
        time: LocalDateTime
    ) = withContext(Dispatchers.IO) {

        val useOffline = dataStore.userPrefs().first().useOffline

        val uid = when {
            useOffline -> null
            else -> authRepository.currentUser?.uid ?: dataStore.lastUid().firstOrNull()
        }

        poopDao.upsert(
            PoopLog(
                uid = uid,
                collectionId = collectionId,
                synced = false,
                loggedAt = time
            )
        )

        if (!useOffline && uid != null) {
            poopSyncManager.requestSync(collectionId)
        }
    }

    fun observeAllCollections(): Flow<List<PoopCollection>> {
        return poopDao.observeAllCollections()
    }

    suspend fun addCollection(
        name: String,
        offline: Boolean
    ) = withContext(Dispatchers.IO) {

        poopDao.addCollection(
            PoopCollection(
                name = name,
                uid = if (offline) null
                else authRepository.currentUser?.uid
            )
        )
        poopApi
    }

    suspend fun deleteCollection(
        id: String,
        onCantDeleteLast: () -> Unit
    ) = withContext(Dispatchers.IO) {

        val collections = poopDao.getAllCollections()

        if (collections.size <= 1) {
            onCantDeleteLast()
            return@withContext
        }

        val toDelete = collections.find { it.id == id } ?: return@withContext

        Log.d("DeleteCollection", "Going to delete $toDelete")

        poopDao.deleteCollection(toDelete)
        poopDao.getAllByCid(toDelete.id).forEach {
            poopDao.delete(it)
        }

        if (
            toDelete.uid != null &&
            toDelete.uid == authRepository.currentUser?.uid
        ) {
            Log.d("DeleteCollection", "Going to delete from firebase uid matched $toDelete")
            if(
                !poopApi.deleteCollection(toDelete.id, toDelete.uid)
            ){
                poopDao.addOfflineDeletedCollection(
                    OfflineDeletedCollection(toDelete.id, toDelete.uid)
                )
            }
        } else {
            Log.d("DeleteCollection", "current uid ${authRepository.currentUser?.uid} $toDelete")
        }
    }

    suspend fun updateCollection(
        id: String,
        update: (PoopCollection) -> PoopCollection
    ) = withContext(Dispatchers.IO) {
        poopDao.getCollectionById(id)?.let { collection ->
            poopDao.upsertCollection(
                update(
                    collection
                )
            )
        }
    }

    fun observeAllPoopLogs(): Flow<List<PoopLog>> {
        return poopDao.observeAll()
    }
}