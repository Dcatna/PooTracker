package my.packlol.pootracker.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PoopCollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCollection(poopCollection: PoopCollection)

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