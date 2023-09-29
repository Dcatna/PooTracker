package my.packlol.pootracker.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class PoopApi(
    private val db: FirebaseFirestore
) {
    private fun log(msg: String) { Log.d("API", msg) }

    private suspend fun getCollection(path: String) = suspendCancellableCoroutine<QuerySnapshot> { cont ->
        db.collection(path)
            .get()
            .addOnSuccessListener { result ->
                cont.resume(result)
            }
            .addOnFailureListener { exception ->
                cont.resumeWithException(exception)
            }
    }

    suspend fun getCollectionIdsForUser(uid: String) = suspendCancellableCoroutine { cont ->
        db.collection(
            "${FBConstants.UserId}/$uid/${FBConstants.PoopListCollection}"
        )
            .get()
            .addOnSuccessListener { snapshot ->
                cont.resume(
                    buildList {
                        snapshot.documents.forEach { document ->
                            add(document.id)
                        }
                    }
                )
            }
            .addOnFailureListener { e ->
                log( "Error adding document ${e.message}")
                cont.resumeWithException(e)
            }
    }

    suspend fun updatePoopList(
        uid: String,
        collectionId: String,
        firebaseData: FirebaseData
    ): Boolean = suspendCancellableCoroutine { cont ->
        db.collection("${FBConstants.UserId}/$uid/${FBConstants.PoopListCollection}")
            .document(collectionId)
            .set(firebaseData.toMap())
            .addOnSuccessListener {
                cont.resume(true)
            }
            .addOnFailureListener { e ->
                log( "Error adding document ${e.message}")
                cont.resumeWithException(e)
            }
    }

    suspend fun getPoopList(uid: String, collectionId: String) = withContext(Dispatchers.IO) {
        getCollection("${FBConstants.UserId}/$uid/${FBConstants.PoopListCollection}")
               .documents
               .find { it.id == collectionId }
               ?.toObject<FirebaseData>()!!
    }
}
