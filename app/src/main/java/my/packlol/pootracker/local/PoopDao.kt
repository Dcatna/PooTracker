package my.packlol.pootracker.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PoopDao {
    @Query("Select * FROM PoopLog")
    fun observeAll(): Flow<List<PoopLog>>

    @Query("Select * FROM PoopLog")
    fun getAll(): List<PoopLog>

    @Query("Select * FROM PoopLog WHERE collectionId = :cid")
    fun getAllByCid(cid: String): List<PoopLog>

    @Query("DELETE FROM PoopLog")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs : List<PoopLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(logs : PoopLog)

    @Delete
    suspend fun delete(poopLog: PoopLog)

    @Query("DELETE FROM PoopLog WHERE id =:id")
    suspend fun deleteById(id: String)

    @Update
    suspend fun updateLog(poopLog: PoopLog)

    @Query("SELECT * FROM PoopCollection WHERE uid = :uid")
    suspend fun getAllCollectionByUid(uid: String?): List<PoopCollection>
}