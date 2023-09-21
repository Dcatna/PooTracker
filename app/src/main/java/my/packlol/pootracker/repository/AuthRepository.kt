package my.packlol.pootracker.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.sync.suspendRunCatching

class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val userDataStore: DataStore
) {

    private val firebaseStateListener = callbackFlow {

        val authStateListener = AuthStateListener {
            trySend(it)
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    fun authState(): Flow<Boolean> =
        userDataStore.userPrefs().combine(firebaseStateListener) { prefs, authState ->
            prefs.useOffline || authState.currentUser != null
        }

    suspend fun loginWithEmailAndPassword(email: String, password: String) =
        suspendRunCatching {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
        }
}