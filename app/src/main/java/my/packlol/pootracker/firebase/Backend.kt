package my.packlol.pootracker.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.packlol.pootracker.ui.theme.PoopLog
import java.time.LocalDateTime
import java.time.ZoneOffset


private object FBConstants {
    const val PoopListCollection = "poop_list_collection"
}

private fun log(msg: String) {
    Log.d("API", msg)
}

fun LocalDateTime.epochSecond() = this.toEpochSecond(ZoneOffset.UTC)

fun PoopLog.toMap(): Map<String, Any> {
    return mapOf(
        "savedAt" to daytime.epochSecond(),
    )
}

data class FirebaseLog(
    val savedAt: Long = 0L,
)

private fun FirebaseLog.toPoopLog(): PoopLog {
    val time = LocalDateTime.ofEpochSecond(savedAt, 0, ZoneOffset.UTC)
    return PoopLog(
        hour = time.hour,
        minute = time.minute,
        second = time.second,
        daytime = time,
    )
}

class PoopApi(
    private val db: FirebaseFirestore
) {

    private fun getCollection(c: String) = callbackFlow<QuerySnapshot> {
        db.collection(c)
            .get()
            .addOnSuccessListener { result ->
                trySend(result)
            }
            .addOnFailureListener { exception ->
                close(exception)
            }
        awaitCancellation()
    }

    suspend fun updatePoopList(poopLog: PoopLog) {
        db.collection(FBConstants.PoopListCollection)
            .add(
                poopLog.toMap()
            )
            .addOnSuccessListener { documentReference ->
               log("DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                log( "Error adding document ${e.message}")
            }
    }

    suspend fun getPoopList() = withContext(Dispatchers.IO) {
        val collection = getCollection(FBConstants.PoopListCollection).first()
        collection.documents.mapNotNull {
            it.toObject<FirebaseLog>()?.toPoopLog()
        }
    }
}


/*
@Entity
data class PoopLog (
    @ColumnInfo val hour:Int,
    @ColumnInfo val minute:Int,
    @ColumnInfo val second:Int,
    @ColumnInfo val daytime: LocalDateTime = LocalDateTime.now(),
    @PrimaryKey(autoGenerate = true) val id: Long = 0
)
 */