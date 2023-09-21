package my.packlol.pootracker.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class PoopLog (
    @ColumnInfo val synced: Boolean = false,
    @ColumnInfo val loggedAt: LocalDateTime = LocalDateTime.now(),
    @PrimaryKey val id: UUID = UUID.randomUUID()
)
