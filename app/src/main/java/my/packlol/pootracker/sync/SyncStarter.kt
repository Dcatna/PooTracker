package my.packlol.pootracker.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager


object SyncStarter {

    const val FirebaseSyncWorkName = "firebase_sync_work"

    /**
        Starts sync with Firebase if no sync work is currently running for [FirebaseSyncer].
     */
    fun start(context: Context) {
        WorkManager.getInstance(context)
            .beginUniqueWork(
                FirebaseSyncWorkName,
                ExistingWorkPolicy.KEEP,
                FirebaseSyncer.workRequest("", syncAll = true)
            )
            .enqueue()
    }
}