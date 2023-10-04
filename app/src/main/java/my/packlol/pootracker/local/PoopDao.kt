package my.packlol.pootracker.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PoopDao {
    @Query("Select * FROM PoopLog")
    fun observeAll(): Flow<List<PoopLog>>

    @Query("Select * FROM PoopLog")
    fun getAll(): List<PoopLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCollection(poopCollection: PoopCollection)

    @Query("Select * FROM PoopLog WHERE id = :id")
    fun getById(id: String): PoopLog?

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

    @Insert
    suspend fun addOfflineDeletedCollection(offlineDeleted: OfflineDeletedCollection)

    @Query("SELECT * FROM OfflineDeletedCollection")
    suspend fun getAllOfflineDeletedCollections(): List<OfflineDeletedCollection>

    @Delete
    suspend fun removeFromOfflineDeletedCollections(offlineDeleted: OfflineDeletedCollection)

    @Insert
    suspend fun addOfflineDeletedLog(offlineDeleted: OfflineDeleteLog)

    @Query("SELECT * FROM OfflineDeleteLog")
    suspend fun getAllOfflineDeletedLogs(): List<OfflineDeleteLog>

    @Query("SELECT * FROM OfflineDeleteLog WHERE id = :id")
    suspend fun getOfflineDeletedById(id: String): OfflineDeleteLog?

    @Delete
    suspend fun removeFromOfflineDelete(offlineDeleted: OfflineDeleteLog)

    @Query("SELECT * FROM PoopCollection WHERE uid = :uid")
    suspend fun getAllCollectionByUid(uid: String?): List<PoopCollection>

    @Query("SELECT * FROM PoopCollection WHERE id = :cid")
    suspend fun getCollectionById(cid: String): PoopCollection?

    @Query("SELECT * FROM PoopCollection")
    suspend fun getAllCollections(): List<PoopCollection>

    @Delete
    suspend fun deleteCollection(collection: PoopCollection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollection(collection: PoopCollection)

    @Query("SELECT * FROM PoopCollection")
    fun observeAllCollections(): Flow<List<PoopCollection>>
}