package my.packlol.pootracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.repository.PoopLogRepository
import my.packlol.pootracker.sync.FirebaseSyncManager
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.log

class HomeVM(
    firebaseSyncManager: FirebaseSyncManager,
    private val poopLogRepository: PoopLogRepository
): ViewModel() {

    val homeUiState = combine(
        firebaseSyncManager.isSyncing,
        poopLogRepository.observeAllPoopLogs()
    ) { syncing, poopLogs ->
       HomeUiState(
           syncing = syncing,
           logs = poopLogs.map { log -> log.toUi() }
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

data class UiPoopLog(
    val id: UUID,
    val synced: Boolean,
    val time: LocalDateTime
)

data class HomeUiState(
    val syncing: Boolean = true,
    val logs: List<UiPoopLog> = emptyList()
)