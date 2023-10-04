package my.packlol.pootracker.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PoopLog::class, PoopCollection::class, OfflineDeleteLog::class, OfflineDeletedCollection::class],
    version = 2
)
@TypeConverters(PoopConverters::class)
abstract class PoopBase : RoomDatabase() {

    abstract fun poopDao() : PoopDao
}