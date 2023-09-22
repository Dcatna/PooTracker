package my.packlol.pootracker.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.SavedUser
import my.packlol.pootracker.sync.suspendRunCatching

class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val userDataStore: DataStore
) {
    var currentUser: FirebaseUser? = firebaseAuth.currentUser

    private val firebaseStateListener = callbackFlow {

        val authStateListener = AuthStateListener {
            currentUser = it.currentUser
            trySend(it)
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    fun authState(): Flow<AuthState> = combine(
        userDataStore.userPrefs(),
        userDataStore.savedUsers(),
        firebaseStateListener
    ) { userPrefs, savedUsers, authState ->
        when {
            userPrefs.useOffline -> AuthState.Offline
            authState.currentUser != null -> {
                savedUsers
                    .find { it.uid == authState.currentUser?.uid }
                    ?.let { AuthState.LoggedIn(it) }
                    ?: AuthState.LoggedOut
            }
            else -> AuthState.LoggedOut
        }
    }

    private suspend fun updateSavedUsers(authResult: AuthResult) {
        authResult.user?.let { user ->
            userDataStore.updateSavedUsers { users ->
                if(!users.any { it.uid == user.uid }) {
                    users + SavedUser(user.uid, user.displayName.toString())
                } else {
                    users
                }
            }
        }
    }

    suspend fun register(email: String, password: String) = suspendRunCatching {
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            .also { result ->
                updateSavedUsers(result)
            }
    }

    suspend fun login(email: String, password: String) = suspendRunCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
            .also { result ->
                updateSavedUsers(result)
            }
    }

    suspend fun logout() {
        firebaseAuth.signOut()
    }
}

sealed interface AuthState {
    data class LoggedIn(val user: SavedUser): AuthState
    data object LoggedOut: AuthState
    data object Offline: AuthState
}