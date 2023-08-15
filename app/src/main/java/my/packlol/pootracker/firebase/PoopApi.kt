package my.packlol.pootracker.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext


class PoopApi(
    private val db: FirebaseFirestore
) {
    private fun log(msg: String) {
        Log.d("API", msg)
    }

    private fun getCollection(c: String) = callbackFlow<QuerySnapshot> {
        db.collection(c)
            .get()
            .addOnSuccessListener { result ->
                trySend(result)
            }
            .addOnFailureListener { exception ->
                close(exception)
            }
        awaitClose()
    }

    suspend fun updatePoopList(firebaseData: FirebaseData) = callbackFlow<FirebaseData> {
        db.collection(FBConstants.PoopListCollection)
            .add(
                firebaseData.toMap()
            )
            .addOnSuccessListener { documentReference ->
               log("DocumentSnapshot added with ID: ${documentReference.id}")
                documentReference.get()
                    .addOnSuccessListener { documentSnapshot ->
                        trySend(
                            documentSnapshot.toObject<FirebaseData>()!!
                        )
                    }
                    .addOnFailureListener { e ->
                        close(e)
                    }
            }
            .addOnFailureListener { e ->
                log( "Error adding document ${e.message}")
                close(e)
            }
        awaitClose()
    }

    suspend fun getPoopList() = withContext(Dispatchers.IO) {
        val collection = getCollection(FBConstants.PoopListCollection).first()
        collection.documents.firstNotNullOfOrNull {
            it.toObject<FirebaseData>()
        }
    }
}
