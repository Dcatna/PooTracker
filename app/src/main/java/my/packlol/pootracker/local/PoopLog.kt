package my.packlol.pootracker.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class PoopLog (
    @ColumnInfo val uid: String? = null,
    @ColumnInfo val collectionId: String,
    @ColumnInfo val synced: Boolean = false,
    @ColumnInfo val loggedAt: LocalDateTime = LocalDateTime.now(),
    @PrimaryKey val id: String = UUID.randomUUID().toString()
)

@Entity
data class PoopCollection(
    @PrimaryKey val id: String,
    @ColumnInfo val name: String,
    @ColumnInfo val uid: String
)