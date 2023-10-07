package my.packlol.pootracker.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OfflineDeletedDao {

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

}