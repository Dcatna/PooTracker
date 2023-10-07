package my.packlol.pootracker.ui.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.repository.PoopLogRepository
import my.packlol.pootracker.sync.FirebaseSyncManager
import java.time.LocalDateTime
import javax.annotation.concurrent.Immutable

class HomeVM(
    private val firebaseSyncManager: FirebaseSyncManager,
    private val poopLogRepository: PoopLogRepository,
    private val authRepository: AuthRepository,
): ViewModel() {

    private val authState = authRepository.authState().distinctUntilChanged()

    private val errorChannel = Channel<HomeError>()
    val errors = errorChannel.receiveAsFlow()

    val homeUiState = combine(
        firebaseSyncManager.isSyncing,
        authState
    ) { syncing, authState ->
       HomeUiState(
           syncing = syncing,
       )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeUiState()
        )

    fun addCollection(name: String, offline: Boolean) {
        viewModelScope.launch {
            runCatching {
                poopLogRepository.addCollection(name, offline)
            }
        }
    }

    fun logPoop(
        time: LocalDateTime,
        collectionId: String,
    ) {
        viewModelScope.launch {
            runCatching {
                poopLogRepository.updatePoopLog(
                    collectionId,
                    time
                )
            }
                .onFailure {
                    errorChannel.send(HomeError.FailedToAdd)
                }
        }
    }

    fun deleteCollection(cid: String) {
        viewModelScope.launch {
            runCatching {
                poopLogRepository.deleteCollection(
                    id = cid,
                    onCantDeleteLast = {
                        errorChannel.trySend(HomeError.FailedToDelete)
                    }
                )
            }
                .onFailure {
                    it.printStackTrace()
                    errorChannel.trySend(HomeError.FailedToDelete)
                }
        }
    }

    fun editCollection(name: String, cid: String) {
        viewModelScope.launch {
        runCatching {
            poopLogRepository.updateCollection(cid) { collection ->
                collection.copy(
                    name = name
                )
            }
        }.onFailure {
            errorChannel.send(HomeError.FailedToUpdateCollection)
        }}
    }

    fun refresh() {
        viewModelScope.launch {
            firebaseSyncManager.requestSync(null)
        }
    }

    fun undoDelete(poopLog: UiPoopLog) {
        viewModelScope.launch {
            runCatching {
                poopLogRepository.undoDeletePoopLog(
                    id = poopLog.id,
                    time = poopLog.time,
                    uid = poopLog.uid,
                    collectionId = poopLog.collectionId
                )
            }
                .onFailure {
                    errorChannel.send(HomeError.FailedToAdd)
                }
        }
    }

    fun deleteLog(poopLog: UiPoopLog) {
        viewModelScope.launch {
            runCatching {
                poopLogRepository.deletePoopLog(poopLog.id)
            }
                .onFailure {
                    errorChannel.send(HomeError.FailedToDelete)
                }
        }
    }
}

fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> = combine(
    combine(flow, flow2, flow3, ::Triple),
    combine(flow4, flow5, flow6, ::Triple),
) { t1, t2 ->
    transform(
        t1.first,
        t1.second,
        t1.third,
        t2.first,
        t2.second,
        t2.third,
    )
}

sealed interface HomeError {
    data object FailedToDelete: HomeError
    data object FailedToAdd: HomeError
    data object FailedToUpdateCollection: HomeError
}

@Stable
@Immutable
data class HomeUiState(
    val syncing: Boolean = true,
)