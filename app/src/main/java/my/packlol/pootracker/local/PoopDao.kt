package my.packlol.pootracker.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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