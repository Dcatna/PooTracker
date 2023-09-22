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
    private fun log(msg: String) { Log.d("API", msg) }

    private fun getCollection(path: String) = callbackFlow<QuerySnapshot> {
        db.collection(path)
            .get()
            .addOnSuccessListener { result ->
                trySend(result)
            }
            .addOnFailureListener { exception ->
                close(exception)
            }
        awaitClose()
    }

    suspend fun updatePoopList(
        uid: String,
        firebaseData: FirebaseData
    ) = callbackFlow {
        db.collection("${FBConstants.UserId}/$uid/${FBConstants.PoopListCollection}")
            .add(
                firebaseData.toMap()
            )
            .addOnSuccessListener { documentReference -> log("DocumentSnapshot added with ID: ${documentReference.id}")
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

    suspend fun getPoopList(uid: String) = withContext(Dispatchers.IO) {
       getCollection("${FBConstants.UserId}/$uid/${FBConstants.PoopListCollection}")
           .first()
           .documents
           .firstNotNullOfOrNull { snapshot ->
               snapshot.toObject<FirebaseData>()
           }
    }
}
