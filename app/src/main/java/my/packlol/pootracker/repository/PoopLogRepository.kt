package my.packlol.pootracker.repository

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.packlol.pootracker.firebase.PoopApi
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.OfflineDeleteLog
import my.packlol.pootracker.local.OfflineDeletedCollection
import my.packlol.pootracker.local.OfflineDeletedDao
import my.packlol.pootracker.local.PoopCollection
import my.packlol.pootracker.local.PoopCollectionDao
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.local.PoopLogDao
import my.packlol.pootracker.sync.FirebaseSyncManager
import java.time.LocalDateTime
import java.util.UUID

class PoopLogRepository(
    private val poopDao: PoopLogDao,
    private val collectionDao: PoopCollectionDao,
    private val offlineDeletedDao: OfflineDeletedDao,
    private val poopSyncManager: FirebaseSyncManager,
    private val authRepository: AuthRepository,
    private val poopApi: PoopApi,
    private val dataStore: DataStore,
) {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            if(collectionDao.getAllCollections().isEmpty()) {
                collectionDao.addCollection(
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
            offlineDeletedDao.addOfflineDeletedLog(
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

        offlineDeletedDao.getOfflineDeletedById(id)?.let {
            offlineDeletedDao.removeFromOfflineDelete(it)
        }

        poopSyncManager.requestSync(collectionId)
    }

    suspend fun updatePoopLog(
        collectionId: String,
        time: LocalDateTime
    ) = withContext(Dispatchers.IO) {

        val collection = collectionDao.getCollectionById(collectionId)
            ?: return@withContext

        if (collection.uid == null) {
            poopDao.upsert(
                PoopLog(
                    uid = null,
                    collectionId = collection.id,
                    synced = false,
                    loggedAt = time
                )
            )
        } else {
            poopDao.upsert(
                PoopLog(
                    uid = collection.uid,
                    collectionId = collection.id,
                    synced = false,
                    loggedAt = time
                )
            )
            Log.d("PoopLog", "going to sync ${collection.id}")
            poopSyncManager.requestSync(collection.id)
        }
    }

    fun observeAllCollections(): Flow<List<PoopCollection>> {
        return collectionDao.observeAllCollections()
    }

    suspend fun addCollection(
        name: String,
        offline: Boolean
    ) = withContext(Dispatchers.IO) {

        val collection = PoopCollection(
            name = name,
            uid = if (offline) null else authRepository.currentUser?.uid,
            id = UUID.randomUUID().toString()
        )

        collectionDao.addCollection(collection)

        if (collection.uid != null) {
            dataStore.updateVersion(collection.id, 1)
        }
    }

    suspend fun deleteCollection(
        id: String,
        onCantDeleteLast: () -> Unit
    ) = withContext(Dispatchers.IO) {

        val collections = collectionDao.getAllCollections()

        if (collections.size <= 1) {
            onCantDeleteLast()
            return@withContext
        }

        val toDelete = collections.find { it.id == id } ?: return@withContext

        Log.d("DeleteCollection", "Going to delete $toDelete")

        collectionDao.deleteCollection(toDelete)
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
                offlineDeletedDao.addOfflineDeletedCollection(
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
        collectionDao.getCollectionById(id)?.let { collection ->
            collectionDao.upsertCollection(
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