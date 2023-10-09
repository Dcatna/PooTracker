package my.packlol.pootracker.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker.Result.failure
import androidx.work.ListenableWorker.Result.success
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import my.packlol.pootracker.R
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.PoopLogDao
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class PoopPredictionNotifier(
    private val appContext: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val poopDao by inject<PoopLogDao>()
    private val dataStore by inject<DataStore>()

    private suspend fun sendNotificationIfAllowed(context: Context, hour: Int) {
        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notifId = dataStore.notifID().first()

            val builder = NotificationCompat.Builder(context, "POOP_PREDICT_CHANNEL_ID")
                .setSmallIcon(R.drawable.poop_emoji)
                .setContentTitle("Poop Expected")
                .setContentText("Based on previous logs a poop is expected at hour: $hour")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            createNotificationChannel()

            with(NotificationManagerCompat.from(appContext)) {
                notify(notifId, builder.build())
            }
            dataStore.updateNotifId(notifId + 1)
        }
    }

    private fun createNotificationChannel() {
        val name = "Poop_Predict_Channel"
        val descriptionText = "POOP_PREDICT_CHANNEL_Desc"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("POOP_PREDICT_CHANNEL_ID", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override suspend fun doWork(): Result {

        val notify = inputData.getBoolean("notify", false)
        val hourToNotify = inputData.getInt("hour", 0)

        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return failure()
        }

        if (notify) { sendNotificationIfAllowed(appContext, hourToNotify) }

        val today = LocalDate.now().dayOfWeek
        val lastUid = dataStore.lastUid().firstOrNull()

        val logsOnDayOfWeek = poopDao.getAll()
            .filter { log -> log.uid == null || log.uid == lastUid }
            .filter { it.loggedAt.dayOfWeek == today }

        val workManager =  WorkManager.getInstance(appContext)

        val sortedByLogsOnHour = logsOnDayOfWeek.groupBy { it.loggedAt.hour }
            .toList()
            .sortedByDescending { (_, logs) -> logs.size }


        val hour = sortedByLogsOnHour.take(3)
            .find { (hour, _) -> (hour - LocalTime.now().hour) > 1 }
            ?.first


        if (logsOnDayOfWeek.isEmpty() || hour == null) {
            workManager.enqueueUniqueWork(
                PoopPredictWorkName,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PoopPredictionNotifier>()
                    .setInitialDelay(Duration.ofHours(4))
                    .build()
            )
        } else {
            workManager.enqueueUniqueWork(
                PoopPredictWorkName,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PoopPredictionNotifier>()
                    .setInitialDelay(
                       Duration.ofHours((hour - LocalTime.now().hour).toLong()),
                    )
                    .setInputData(
                        Data.Builder()
                            .putBoolean("notify", true)
                            .putInt("hour", hour)
                            .build()
                    )
                    .build()
            )
        }

        return success()
    }

    companion object {
        const val PoopPredictWorkName = "PoopPredictWorkName"

        fun init() = OneTimeWorkRequestBuilder<PoopPredictionNotifier>()
            .setInputData(
                Data.Builder()
                    .putBoolean("notify", false)
                    .putInt("hour", 0)
                    .build()
            )
            .build()
    }
}