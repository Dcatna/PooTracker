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
import my.packlol.pootracker.local.PoopDao
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.repository.AuthRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration

/**
 * [CoroutineWorker] that pulls data from Firestore and updates the [PoopDao] with new data.
 * This also deletes any data that was deleted in Firestore.
 * After syncing data with Firestore any un-synced data from [PoopDao] is pushed to the Firestore db.
 */
class FirebaseSyncer(
    applicationContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(applicationContext, workerParameters),  KoinComponent {

    private val TAG = "FirebaseSyncer"
    private val poopDao by inject<PoopDao>()
    private val poopApi by inject<PoopApi>()
    private val dataStore by inject<DataStore>()
    private val authRepository by inject<AuthRepository>()

    private suspend fun syncLocalWithNetwork(
        network: FirebaseData,
        local: List<PoopLog>,
        localVersion: Int,
        collectionId: String,
        uid: String
    ) {
        Log.d(TAG, "network version ${network.version} local version $localVersion")
        if (network.version > localVersion) {
            // Network version is higher
            // delete any ids synced and not saved to network anymore
            local
                .filter { it.synced }
                .map { it.id }
                .filter { id -> id !in network.logs.map { log -> log.id } }
                .also { Log.d(TAG, "deleting from uid $uid cid $collectionId ids $it") }
                .forEach {
                    poopDao.deleteById(it)
                }
            // update local to match network
            dataStore.updateVersion(collectionId, network.version).also {
                Log.d(TAG, "local version after update $it")
            }
            poopDao.upsertAll(
                network.logs.map {
                    it.toPoopLog(
                        uid = uid,
                        collectionId = collectionId
                    )
                }
                    .also { Log.d(TAG, "upserting $it") }
            )
        }
    }

    private suspend fun syncPoopLogs(uid: String, collectionId: String) = suspendRunCatching {

        val network = poopApi.getPoopList(uid, collectionId)
        Log.d(TAG, "firebase data for uid $uid cid $collectionId items: ${network.logs.size}")

        val local = poopDao.getAllByCid(collectionId)
        Log.d(TAG, "local data for cid $collectionId items: ${local.size}")

        val version = dataStore.version(collectionId).firstOrNull() ?: -1
        Log.d(TAG, "current version for cid $collectionId: $version")

        syncLocalWithNetwork(
            network = network,
            local = local,
            localVersion = version,
            uid = uid,
            collectionId = collectionId
        )
        // synced with network at this point update network with local changes
        // any changes that where local will be synced = false
        // versions are equal if local change not present increase version and update
        val localAfterSync = poopDao.getAllByCid(collectionId)
        Log.d(TAG, "local data for $collectionId after sync items: ${local.size}")

        val versionAfterSync = dataStore.version(collectionId).first()
        Log.d(TAG, "current version for $collectionId after sync: $version")

        val anyUnsynced = localAfterSync.any { !it.synced }
        Log.d(TAG, "anyUnsynced for $collectionId after sync: $anyUnsynced")

        if (anyUnsynced || network.version < version) {
            val updated = poopApi.updatePoopList(
                uid = uid,
                collectionId = collectionId,
                FirebaseData(
                    version = dataStore.updateVersion(collectionId, versionAfterSync + 1)
                        .also { Log.d(TAG, "updated network cid $collectionId, version $version") },
                    logs = localAfterSync.map { it.toFirebaseLog() },
                )
            )
            if (updated) {
                localAfterSync.forEach { log ->
                    poopDao.updateLog(
                        log.copy(synced = true)
                    )
                }
            }
        }
    }

    override suspend fun doWork(): Result {

        val syncAll = inputData.getBoolean("all", false)

        val uid = authRepository.currentUser?.uid ?: return Result.failure()

        val result = if (false) {
            buildSet {
                addAll(poopApi.getCollectionIdsForUser(uid))
                addAll(poopDao.getAllCollectionByUid(uid).map { it.id })
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
                collectionId = "9b508294-1ec6-479b-9a08-9f0afdd0baad" // inputData.getString("cid") ?: return Result.failure()
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