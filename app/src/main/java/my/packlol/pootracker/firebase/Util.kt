package my.packlol.pootracker.firebase

import my.packlol.pootracker.local.PoopLog
import java.time.LocalDateTime
import java.time.ZoneOffset

object FBConstants {
    const val PoopListCollection = "poop_list_collection"
}

fun LocalDateTime.epochSecond() = this.toEpochSecond(ZoneOffset.UTC)

fun FirebaseData.toMap(): Map<String, Any> {
    return mapOf(
        "version" to version,
        "logs" to logs.map(::toMap)
    )
}

fun toMap(firebaseLog: FirebaseData.FirebaseLog): Map<String, Any> =
    mapOf(
        "id" to firebaseLog.id,
        "loggedAt" to firebaseLog.loggedAt
    )

fun PoopLog.toMap() = mapOf<String, Any>(
    "id" to id,
    "loggedAt" to loggedAt
)

data class FirebaseData(
    val version: Int = 0,
    val logs: List<FirebaseLog> = emptyList()
) {

    data class FirebaseLog(
        val loggedAt: Long = 0L,
        val id: String = ""
    )
}
