package my.packlol.pootracker.ui.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.local.SavedUser
import my.packlol.pootracker.repository.PoopLogRepository
import my.packlol.pootracker.sync.FirebaseSyncManager
import java.time.LocalDateTime
import java.util.UUID
import javax.annotation.concurrent.Immutable
import kotlin.math.log

class HomeVM(
    firebaseSyncManager: FirebaseSyncManager,
    private val poopLogRepository: PoopLogRepository,
    userDataStore: DataStore
): ViewModel() {

    val homeUiState = combine(
        firebaseSyncManager.isSyncing,
        userDataStore.savedUsers(),
        poopLogRepository.observeAllPoopLogs()
    ) { syncing, savedUsers, poopLogs ->
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
                   poopLogs.filter { it.uid == null }.map { it.toUi() }
               )
           }
       )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeUiState()
        )

    fun logPoop() {
        viewModelScope.launch {
            poopLogRepository.updatePoopLog()
        }
    }
}

fun PoopLog.toUi(): UiPoopLog {
    return UiPoopLog(
        id = this.id,
        synced = this.synced,
        time = this.loggedAt,
    )
}

@Stable
@Immutable
data class UiPoopLog(
    val id: UUID,
    val synced: Boolean,
    val time: LocalDateTime
)

@Stable
@Immutable
data class HomeUiState(
    val syncing: Boolean = true,
    val logsByUser: Map<SavedUser, List<UiPoopLog>> = emptyMap()
)