package my.packlol.pootracker.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

// based on now in android app sync manager to observe work status
// https://github.com/android/nowinandroid/blob/153b34fd50c0eb98fb42b298be7525080a1e43bd/sync/work/src/main/java/com/google/samples/apps/nowinandroid/sync/status/WorkManagerSyncManager.kt#L34

/**
 * backed by [WorkInfo] from [WorkManager]
 * manages the [FirebaseSyncer]
 */
class FirebaseSyncManager(
    private val context: Context,
)  {
    /**
     * Emits boolean based on the [WorkInfo] state of the [FirebaseSyncer].
     */
    val isSyncing: Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(SyncStarter.FirebaseSyncWorkName)
            .map { workInfoList ->
                workInfoList.anyRunning()
            }
            .conflate()
    /**
     * Call after a poop-log is saved to sync with the network.
     * Only one sync request will run at a time this function will replace any
     * older sync requests.
     */
    fun requestSync(collectionId: String?) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            SyncStarter.FirebaseSyncWorkName,
            ExistingWorkPolicy.REPLACE,
            FirebaseSyncer.workRequest(collectionId, collectionId == null),
        )
    }

    private fun List<WorkInfo>.anyRunning() = any { it.state == WorkInfo.State.RUNNING }
}