package my.packlol.pootracker.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class PoopLog (
    @ColumnInfo val hour:Int,
    @ColumnInfo val minute:Int,
    @ColumnInfo val second:Int,
    @ColumnInfo val daytime: LocalDateTime = LocalDateTime.now(),
    @PrimaryKey(autoGenerate = true) val id: Long = 0
)
