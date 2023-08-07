package my.packlol.pootracker.ui.theme

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import my.packlol.pootracker.PoopConverters
import java.time.LocalDateTime
import java.time.ZoneOffset


@Entity
data class PoopLog (
    @ColumnInfo val hour:Int,
    @ColumnInfo val minute:Int,
    @ColumnInfo val second:Int,
    @ColumnInfo val daytime: LocalDateTime = LocalDateTime.now(),
    @PrimaryKey(autoGenerate = true) val id: Long = 0
)

@Dao
interface PoopDao {
    @Query("Select * FROM PoopLog")
    fun observeAll(): Flow<List<PoopLog>>

    @Query("SELECT * FROM PoopLog WHERE id =:id")
    fun loadALLByIds(id:Long) : Flow<PoopLog>

    @Query("DELETE FROM PoopLog")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg id : PoopLog)

    @Delete
    suspend fun delete(pooplog:PoopLog)

    @Query("DELETE FROM PoopLog WHERE id =:id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [PoopLog::class], version = 1)
@TypeConverters(PoopConverters::class)
abstract class PoopBase : RoomDatabase() {
    abstract fun poopDao() : PoopDao
}

object DB{
    private var db : PoopBase? = null
    fun getDao() : PoopDao{
        return db?.poopDao()!!
    }

    fun createDB(context : Context){
        if(db==null){
            db = Room.databaseBuilder(context, PoopBase::class.java, "Poop.db").fallbackToDestructiveMigration().build()
        }
    }


}