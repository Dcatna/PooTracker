package my.packlol.pootracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.repository.PoopLogRepository
import my.packlol.pootracker.ui.home.toUi
import java.time.LocalDate
import java.time.Month

class StatsVM(
    private val poopLogRepository: PoopLogRepository,
    private val authRepository: AuthRepository,
    private val datastore: DataStore
): ViewModel() {

    private val unselectedCollectionIds = MutableStateFlow(emptyList<String>())

    val collections = combine(
        poopLogRepository.observeAllCollections(),
        authRepository.authState(),
        datastore.lastUid()
    ) { collections, auth, lastUid ->
        when (auth) {
            is AuthState.LoggedIn -> collections.filter {
                it.uid == auth.user.uid || it.uid == null
            }
            else -> collections.filter {
                it.uid == null || it.uid == lastUid
            }
        }
            .map { it.toUi() }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )


    val selectedCollections = combine(
        collections,
        unselectedCollectionIds
    ) { collections, unselected ->
        collections.filter { it.id !in unselected }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val poopLogs = combine(
        poopLogRepository.observeAllPoopLogs(),
        selectedCollections
    ) { logs, collections ->
        logs
            .filter { log -> log.collectionId in collections.map { it.id } }
            .map { it.toUi() }
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )


    val avgPoopsByMonth = poopLogs.map { logs ->

        val needToAdd = mutableSetOf<Int>().also { it.addAll(1..12) }

        val avgPoopsByMonth = logs
            .groupBy { it.time.month }.toList()
            .map { (month, logs) ->

                val seenMonths = mutableSetOf<Pair<Month, Int>>()

                val totalMonths = logs
                    .filter { it.time.month == month }
                    .count { seenMonths.add(it.time.month to it.time.year) }

                needToAdd.remove(month.value)
                entryOf(month.value, logs.count() / totalMonths)
            }

        ChartEntryModelProducer(
            avgPoopsByMonth + needToAdd.map { entryOf(it, 0) }
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ChartEntryModelProducer(entryCollections = emptyList())
        )


    val avgPoopsByDayOfWeekEntries = poopLogs.map { logs ->

        val needToAdd = mutableSetOf<Int>().also { it.addAll(1..7) }

        val avgPoopsByDOW = logs
            .groupBy { it.time.dayOfWeek }.toList()
            .map { (dow, logs) ->

                val seenDays = mutableSetOf<LocalDate>()

                val totalDays = logs
                    .filter { it.time.dayOfWeek == dow }
                    .count { seenDays.add(it.time.toLocalDate()) }

                needToAdd.remove(dow.value)
                entryOf(dow.value, logs.count() / totalDays)
            }

        ChartEntryModelProducer(
            avgPoopsByDOW + needToAdd.map { entryOf(it, 0) }
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ChartEntryModelProducer(entryCollections = emptyList())
        )

    fun toggleCollectionFilter(cid: String) {
        viewModelScope.launch {
            unselectedCollectionIds.update {
                if (cid in it) {
                    it - cid
                } else {
                    it + cid
                }
            }
        }
    }
}