package my.packlol.pootracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.repository.PoopLogRepository

class StatsVM(
    private val poopLogRepository: PoopLogRepository,
    private val authRepository: AuthRepository,
    private val datastore: DataStore
): ViewModel() {

    val poopLogs = combine(
        poopLogRepository.observeAllPoopLogs(),
        authRepository.authState()
    ) { logs, auth ->
        when (auth) {
            is AuthState.LoggedIn ->
                logs.filter { log -> log.uid == auth.user.uid || log.uid == null }
            else -> logs.filter { log ->
                log.uid == null || log.uid == datastore.lastUid().firstOrNull()
            }
        }
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

}