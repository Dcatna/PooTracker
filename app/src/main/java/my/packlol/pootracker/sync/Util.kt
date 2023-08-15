package my.packlol.pootracker.sync

import android.util.Log
import kotlinx.coroutines.CancellationException
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

fun FirebaseData.FirebaseLog.toPoopLog() =
    PoopLog(
        synced = true,
        loggedAt = LocalDateTime.ofEpochSecond(this.loggedAt, 0, ZoneOffset.UTC),
        id = UUID.fromString(id)
    )

fun PoopLog.toFirebaseLog() =
    FirebaseData.FirebaseLog(
        loggedAt = this.loggedAt.epochSecond(),
        id = id.toString()
    )
