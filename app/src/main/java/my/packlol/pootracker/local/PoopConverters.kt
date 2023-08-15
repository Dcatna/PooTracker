package my.packlol.pootracker.local

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.ZoneOffset

object PoopConverters{

    @TypeConverter
    fun fromLongtoDaytime(number : Long) : LocalDateTime {
        return LocalDateTime.ofEpochSecond(number,0 , ZoneOffset.UTC)
    }

    @TypeConverter
    fun fromDaytimetoLong(daytime: LocalDateTime) : Long{
        return daytime.toEpochSecond(ZoneOffset.UTC)
    }
}