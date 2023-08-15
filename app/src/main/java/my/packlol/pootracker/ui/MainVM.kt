package my.packlol.pootracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.packlol.pootracker.firebase.PoopApi
import my.packlol.pootracker.local.PoopDao
import my.packlol.pootracker.sync.FirebaseSyncManager
import my.packlol.pootracker.ui.screens.Time
import java.time.Duration
import java.time.LocalDateTime

class MainVM(
    private val dao: PoopDao,
    private val poopApi: PoopApi,
    private val syncManager: FirebaseSyncManager
) : ViewModel() {


    val juicerList = dao.observeAll()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val currentTime = flow {
        while (true) {
            emit(
                LocalDateTime.now()
            )
            delay(100)
        }
    }



    val timeSinceLastPoop = currentTime.combine(juicerList) {
            time, poopLogs ->
        val latest = poopLogs.maxByOrNull {
            it.loggedAt
        }
            ?.loggedAt
            ?: return@combine Time(0, 0, 0, never = true)

        val timeSince = Duration.between(latest, time)


        Time(timeSince.toHours().toInt(), timeSince.toMinutes().toInt(), timeSince.seconds.toInt())
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Time()
        )

    fun insert(hour: Int, minute: Int, second: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {

            }
        }
    }
}