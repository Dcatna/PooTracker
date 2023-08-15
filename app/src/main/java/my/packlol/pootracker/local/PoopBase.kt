package my.packlol.pootracker.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import my.packlol.pootracker.PoopConverters

@Database(entities = [PoopLog::class], version = 1)
@TypeConverters(PoopConverters::class)
abstract class PoopBase : RoomDatabase() {
    abstract fun poopDao() : PoopDao
}