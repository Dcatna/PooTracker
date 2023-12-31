package my.packlol.pootracker.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import my.packlol.pootracker.firebase.FirebaseData
import my.packlol.pootracker.firebase.PoopApi
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.OfflineDeletedDao
import my.packlol.pootracker.local.PoopCollection
import my.packlol.pootracker.local.PoopCollectionDao
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.local.PoopLogDao
import my.packlol.pootracker.repository.AuthRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

// TODO(Clean up all of the logic that is required for sync and do this more efficiently)

/**
 * [CoroutineWorker] that pulls data from Firestore and updates the [PoopLogDao] with new data.
 * This also deletes any data that was deleted in Firestore.
 * After syncing data with Firestore any un-synced data from [PoopLogDao] is pushed to the Firestore db.
 */
class FirebaseSyncer(
    applicationContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(applicationContext, workerParameters),  KoinComponent {

    private val TAG = "FirebaseSyncer"
    private val poopDao by inject<PoopLogDao>()
    private val poopApi by inject<PoopApi>()
    private val dataStore by inject<DataStore>()
    private val collectionDao by inject<PoopCollectionDao>()
    private val offlineDeletedDao by inject<OfflineDeletedDao>()
    private val authRepository by inject<AuthRepository>()

    private suspend fun syncLocalWithNetwork(
        network: FirebaseData,
        local: List<PoopLog>,
        localVersion: Int,
        collection: PoopCollection,
    ) {
        Log.d(TAG, "network version ${network.version} local version $localVersion")
        if (network.version > localVersion) {
            // Network version is higher
            // delete any ids synced and not saved to network anymore
            collectionDao.upsertCollection(
                collection.copy(
                    name = network.name
                )
            )

            local
                .filter { it.synced }
                .map { it.id }
                .filter { id -> id !in network.logs.map { log -> log.id } }
                .also { Log.d(TAG, "deleting from uid ${collection.uid} cid ${collection.id} ids $it") }
                .forEach {
                    poopDao.deleteById(it)
                }
            // update local to match network
            dataStore.updateVersion(collection.id, network.version).also {
                Log.d(TAG, "local version after update $it")
            }

            poopDao.upsertAll(
                network.logs.map {
                    it.toPoopLog(
                        uid = collection.uid!!,
                        collectionId = collection.id
                    )
                }
                    .also { Log.d(TAG, "upserting $it") }
            )
        }
    }

    private suspend fun getNetworkOrAddEmptyCollectionForFirstTime(uid: String, collectionId: String): FirebaseData {
        var networkList = runCatching {
            poopApi.getPoopList(uid, collectionId)
        }
            .getOrNull()

        Log.d(TAG, "firebase data for uid $uid cid $collectionId items: ${networkList?.logs?.size}")

        if (networkList == null) {
            val c = collectionDao.getCollectionById(collectionId) ?: error("no collection network or local")
            poopApi.updatePoopList(
                uid, collectionId, FirebaseData(-99, false, emptyList(), c.name)
            ).let {
                if (it) {
                    networkList = FirebaseData(-99, false, emptyList(), c.name)
                }
            }
        }
        return networkList!!
    }

    private suspend fun getLocalCollectionOrAddEmpty(collectionId: String, uid: String, network: FirebaseData): PoopCollection {
        val collection = collectionDao.getCollectionById(collectionId)
        if (collection == null) {
            collectionDao.upsertCollection(
                PoopCollection(
                    id = collectionId,
                    name = network.name,
                    uid = uid
                )
            )
            dataStore.updateVersion(collectionId, -1)
            return collectionDao.getCollectionById(collectionId)!!
        }
        return collection
    }

    private suspend fun syncPoopLogs(uid: String, collectionId: String) = suspendRunCatching {

        val network = getNetworkOrAddEmptyCollectionForFirstTime(uid, collectionId)

        if (network.deleted) {
            for (poopLog in poopDao.getAllByCid(collectionId)) {
                poopDao.delete(poopLog)
            }
            return@suspendRunCatching
        }

        val collection = getLocalCollectionOrAddEmpty(collectionId, uid, network)

        val local = poopDao.getAllByCid(collectionId)
        Log.d(TAG, "local data for cid $collectionId items: ${local.size}")

        val version = dataStore.version(collectionId).firstOrNull() ?: -1
        Log.d(TAG, "current version for cid $collectionId: $version")

        syncLocalWithNetwork(
            network = network,
            local = local,
            localVersion = version,
            collection = collection
        )
        // synced with network at this point update network with local changes
        // any changes that where local will be synced = false
        // versions are equal if local change not present increase version and update

        val localAfterSync = poopDao.getAllByCid(collectionId)
        val toDelete = offlineDeletedDao.getAllOfflineDeletedLogs()
        val versionAfterSync = dataStore.version(collectionId).first()
        val anyUnsynced = localAfterSync.any { !it.synced }

        Log.d(TAG, "local data for $collectionId after sync items: ${local.size}")
        Log.d(TAG, "toDelete data for $collectionId ids: ${toDelete.map { it.id }}")
        Log.d(TAG, "current version for $collectionId after sync: $version")
        Log.d(TAG, "anyUnsynced for $collectionId after sync: $anyUnsynced")


        if (anyUnsynced || network.version < version || toDelete.isNotEmpty()) {
            val successful = poopApi.updatePoopList(
                uid = uid,
                collectionId = collectionId,
                FirebaseData(
                    version = dataStore.updateVersion(collectionId, versionAfterSync + 1)
                        .also { Log.d(TAG, "updated network cid $collectionId, version $version") },
                    logs = localAfterSync.map { it.toFirebaseLog() },
                    deleted = false,
                    name = collectionDao.getCollectionById(collectionId)!!.name
                )
            )
            if (successful) {
                localAfterSync.forEach { log ->
                    poopDao.updateLog(
                        log.copy(synced = true)
                    )
                }
                toDelete.forEach { deleted ->
                    if (deleted.id !in localAfterSync.map { it.id }) {
                        offlineDeletedDao.removeFromOfflineDelete(deleted)
                    }
                }
            }
        }
    }

    override suspend fun doWork(): Result {

        val syncAll = inputData.getBoolean("all", false)

        val uid = authRepository.currentUser?.uid ?: return Result.failure()

        for (collection in offlineDeletedDao.getAllOfflineDeletedCollections()) {
            if (
                collection.uid == uid &&
                poopApi.deleteCollection(collection.collectionId, uid)
            ) {
                offlineDeletedDao.removeFromOfflineDeletedCollections(collection)
            }
        }

        val result = if (syncAll) {
            Log.d(TAG, "=========== Syncing all ===========")
            buildSet {
                addAll(poopApi.getCollectionIdsForUser(uid))
                addAll(collectionDao.getAllCollections().filter { it.uid == uid }.map { it.id })
            }
                .map { cid ->
                    syncPoopLogs(
                        uid = uid,
                        collectionId = cid
                    )
                        .isSuccess
                }
                .all { it }
        } else {
            syncPoopLogs(
                uid = uid,
                collectionId = inputData.getString("cid") ?: return Result.failure()
            )
                .isSuccess
        }

        return if (result) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return applicationContext.createForegroundInfo(
            notificationId = 0,
            notificationChannelName = "sync_logs_notification_name",
            notificationChannelId = "sync_logs_notification_id",
            description = "syncing poop logs with the cloud save.",
            title = "syncing poop logs with the cloud save."
        )
    }

    companion object {
        fun workRequest(
            collectionId: String?,
            syncAll: Boolean = false
        ) = OneTimeWorkRequestBuilder<FirebaseSyncer>()
            .setConstraints(
                Constraints(
                    requiredNetworkType = NetworkType.CONNECTED
                )
            )
            .setInputData(
                Data.Builder()
                    .putString("cid", collectionId)
                    .putBoolean("all", syncAll)
                    .build()
            )
            .setBackoffCriteria(
                duration = Duration.ofSeconds(15),
                backoffPolicy = BackoffPolicy.EXPONENTIAL
            )
            .setExpedited(
                policy = OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
            )
            .build()
    }
}