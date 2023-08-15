package my.packlol.pootracker.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.util.UUID

/**
 * [CoroutineWorker] that pulls data from Firestore and updates the [PoopDao] with new data.
 * This also deletes any data that was deleted in Firestore.
 * After syncing data with Firestore any un-synced data from [PoopDao] is pushed to the Firestore db.
 */
class FirebaseSyncer(
    applicationContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(applicationContext, workerParameters),  KoinComponent {

    private val poopDao by inject<PoopDao>()
    private val poopApi by inject<PoopApi>()
    private val dataStore by inject<DataStore>()

    private suspend fun syncLocalWithNetwork(network: FirebaseData, local: List<PoopLog>, localVersion: Int) {
        if (network.version > localVersion) {
            // Network version is higher
            // delete any ids synced and not saved to network anymore
            local
                .filter { it.synced }
                .map { it.id }
                .filter { it !in network.logs.map { log -> UUID.fromString(log.id) } }
                .forEach {
                    poopDao.deleteById(it)
                }
            // update local to match network
            dataStore.updateVersion(network.version)
            poopDao.upsertAll(
                network.logs.map { it.toPoopLog() }
            )
        }
    }

    override suspend fun doWork(): Result {

        val network = runCatching {
            poopApi.getPoopList()
        }
            .onFailure { it.printStackTrace() }
            .getOrElse { return Result.retry() }

        val local = poopDao.getAll()
        val version = dataStore.getVersion().firstOrNull() ?: -1

        val result = suspendRunCatching {
            if (network != null) {
                syncLocalWithNetwork(network, local, version)
            }
            // synced with network at this point update network with local changes
            // any changes that where local will be synced = false
            // versions are equal if local change not present increase version and update
            val localAfterSync = poopDao.getAll()
            val versionAfterSync = dataStore.getVersion().first()

            val anyUnsynced = localAfterSync.any { !it.synced }

            if (anyUnsynced) {
                // update network with local after sync and update version
                val data = poopApi.updatePoopList(
                    FirebaseData(
                        version = dataStore.updateVersion(versionAfterSync),
                        logs = localAfterSync.map { it.toFirebaseLog() }
                    )
                )
                    .first()
                // use result from the network update to
                // set the correct sync status for local logs
                poopDao.upsertAll(
                    data.logs.map { it.toPoopLog() }
                )
            }
        }
        // if the suspendRunCatching block didn't throw and exception
        // sync was successful otherwise retry sync of data
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        fun workRequest() = OneTimeWorkRequestBuilder<FirebaseSyncer>()
            .setConstraints(
                Constraints(
                    requiredNetworkType = NetworkType.CONNECTED
                )
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