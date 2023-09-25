package my.packlol.pootracker.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import kotlinx.coroutines.CancellationException
import my.packlol.pootracker.R
import my.packlol.pootracker.firebase.FirebaseData
import my.packlol.pootracker.firebase.epochSecond
import my.packlol.pootracker.local.PoopLog
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Attempts [block], returning a successful [Result] if it succeeds, otherwise a [Result.Failure]
 * taking care not to break structured concurrency
 */
suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (exception: Exception) {
    Log.i(
        "suspendRunCatching",
        "Failed to evaluate a suspendRunCatchingBlock. Returning failure Result",
        exception,
    )
    Result.failure(exception)
}

fun FirebaseData.FirebaseLog.toPoopLog(
    uid: String,
    collectionId: String
) = PoopLog(
        synced = true,
        loggedAt = LocalDateTime.ofEpochSecond(this.loggedAt, 0, ZoneOffset.UTC),
        id = id,
        uid = uid,
        collectionId = collectionId
    )

fun PoopLog.toFirebaseLog() =
    FirebaseData.FirebaseLog(
        loggedAt = this.loggedAt.epochSecond(),
        id = id.toString()
    )

fun Context.createForegroundInfo(
    notificationId: Int,
    notificationChannelName: String,
    notificationChannelId: String,
    description: String,
    title: String
): ForegroundInfo {

    return ForegroundInfo(
        notificationId,
        createNotification(
            notificationChannelId,
            notificationChannelName,
            description,
            title
        )
    )
}

/**
 * Notification displayed on lower API levels when sync workers are being
 * run with a foreground service
 */
private fun Context.createNotification(
    notificationChannelId: String,
    notificationChannelName: String,
    description: String,
    title: String
): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            notificationChannelId,
            notificationChannelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            this.description = description
        }
        // Register the channel with the system
        val notificationManager: NotificationManager? =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        notificationManager?.createNotificationChannel(channel)
    }

    return NotificationCompat.Builder(
        this,
        notificationChannelId,
    )
        .setSmallIcon(
            R.drawable.ic_launcher_foreground,
        )
        .setContentTitle(title)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}