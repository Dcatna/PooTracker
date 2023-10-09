package my.packlol.pootracker.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager

object NotificationStarter {

    fun start(context: Context) {
        WorkManager.getInstance(context)
            .beginUniqueWork(
                PoopPredictionNotifier.PoopPredictWorkName,
                ExistingWorkPolicy.KEEP,
                PoopPredictionNotifier.init()
            )
            .enqueue()
    }
}