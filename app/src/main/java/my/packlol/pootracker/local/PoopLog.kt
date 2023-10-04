package my.packlol.pootracker.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class PoopLog (
    @PrimaryKey val id: String = randomUUIDString(),
    val uid: String? = null,
    val collectionId: String,
    val synced: Boolean = false,
    val loggedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
data class PoopCollection(
    @PrimaryKey val id: String = randomUUIDString(),
    val name: String,
    val uid: String?
)

@Entity
data class OfflineDeleteLog(
    @PrimaryKey val id: String,
    val collectionId: String,
    val loggedAt: LocalDateTime,
    val uid: String,
)

@Entity
data class OfflineDeletedCollection(
    @PrimaryKey
    val collectionId: String,
    val uid: String,
)

private fun randomUUIDString(): String {
    return UUID.randomUUID().toString()
}