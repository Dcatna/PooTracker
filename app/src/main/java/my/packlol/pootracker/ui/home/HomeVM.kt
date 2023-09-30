package my.packlol.pootracker.ui.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.PoopCollection
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.local.SavedUser
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.repository.PoopLogRepository
import my.packlol.pootracker.sync.FirebaseSyncManager
import java.time.LocalDateTime
import java.util.UUID
import javax.annotation.concurrent.Immutable

class HomeVM(
    private val firebaseSyncManager: FirebaseSyncManager,
    private val poopLogRepository: PoopLogRepository,
    private val authRepository: AuthRepository,
    userDataStore: DataStore
): ViewModel() {

    private val authState = authRepository.authState()
        .distinctUntilChanged()
        .onEach {
            when(it) {
                is AuthState.LoggedIn -> firebaseSyncManager.requestSync(null)
                else -> Unit
            }
        }

    val homeUiState = combine(
        firebaseSyncManager.isSyncing,
        userDataStore.savedUsers(),
        poopLogRepository.observeAllPoopLogs(),
        poopLogRepository.observeAllCollections(),
        authState,
    ) { syncing, savedUsers, poopLogs, collections, authState ->
       HomeUiState(
           syncing = syncing,
           logsByUser = buildMap {
               savedUsers.forEach { user ->
                   put(
                       key = user,
                       value = poopLogs
                           .filter { it.uid == user.uid }
                           .map { it.toUi() }
                   )
               }
               put(
                   SavedUser("offline", "offline"),
                   poopLogs.filter {
                       it.uid == authState.loggedIn?.user?.uid || it.uid == null
                   }
                       .map { it.toUi() }
               )
           },
           collections = collections.filter {
               it.uid == authState.loggedIn?.user?.uid || it.uid == null
           }
               .map { it.toUi() }
       )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeUiState()
        )

    fun logPoop() {
        viewModelScope.launch {
            poopLogRepository.updatePoopLog(
                UUID.fromString("9b508294-1ec6-479b-9a08-9f0afdd0baad")
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            firebaseSyncManager.requestSync(null)
        }
    }

    fun undoDelete(poopLog: UiPoopLog) {
        viewModelScope.launch {
            poopLogRepository.addPoopLog(
                id = poopLog.id,
                time = poopLog.time,
                synced =  poopLog.synced,
                uid = poopLog.uid,
                collectionId = poopLog.collectionId
            )
        }
    }

    fun deleteLog(poopLog: UiPoopLog): Boolean {
        viewModelScope.launch {
            poopLogRepository.deletePoopLog(poopLog.id)
        }
        return true
    }
}

fun PoopLog.toUi(): UiPoopLog {
    return UiPoopLog(
        id = this.id,
        synced = this.synced,
        uid = this.uid,
        collectionId = this.collectionId,
        time = this.loggedAt,
    )
}

fun PoopCollection.toUi(): UiCollection {
    return UiCollection(
        name = this.name,
        id = this.id,
        uid = uid
    )
}

@Stable
@Immutable
data class UiPoopLog(
    val id: String,
    val uid: String?,
    val collectionId: String,
    val synced: Boolean,
    val time: LocalDateTime
)

@Stable
@Immutable
data class UiCollection(
    val name: String,
    val id: String,
    val uid: String?,
)


@Stable
@Immutable
data class HomeUiState(
    val syncing: Boolean = true,
    val logsByUser: Map<SavedUser, List<UiPoopLog>> = emptyMap(),
    val collections: List<UiCollection> = emptyList()
) {

    val logs: List<UiPoopLog> = logsByUser.values.flatten()
}