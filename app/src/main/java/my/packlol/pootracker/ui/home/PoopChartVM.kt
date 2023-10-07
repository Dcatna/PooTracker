package my.packlol.pootracker.ui.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.PoopCollection
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.repository.PoopLogRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import javax.annotation.concurrent.Immutable

class PoopChartVM(
    poopLogRepository: PoopLogRepository,
    authRepository: AuthRepository,
    dataStore: DataStore
): ViewModel() {

    private val monthsPrev = 12
    private val startDate = LocalDateTime.now()

    private val unselectedCollections = MutableStateFlow<List<String>>(emptyList())

    val collections = combine(
        poopLogRepository.observeAllCollections(),
        authRepository.authState(),
        dataStore.lastUid()
    ) { collections, authState, lastUid ->
        collections.filter { collection ->
            when (authState) {
                AuthState.LoggedOut, AuthState.Offline ->
                    collection.uid == null || collection.uid == lastUid

                is AuthState.LoggedIn ->
                    collection.uid == authState.user.uid ||
                            collection.uid == null || collection.uid == lastUid
            }
        }
            .map { collection -> collection.toUi() }
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val selectedCollections = combine(
        unselectedCollections,
        collections
    ) { unselected, collections ->
        collections.filter { collection -> collection.id !in unselected }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private val poopLogs = combine(
        poopLogRepository.observeAllPoopLogs(),
        selectedCollections
    ) { list, collections ->
        val collectionIds = collections.map { it.id }
        list.map { log -> log.toUi() }
            .filter { log -> log.collectionId in collectionIds }
    }

    private val mutableSelectedDates = MutableStateFlow<List<LocalDateTime>>(emptyList())
    val selectedDates = mutableSelectedDates.asStateFlow()


    private val mutableSelectedMonths = MutableStateFlow<List<Month>>(emptyList())
    val selectedMonths = mutableSelectedMonths.asStateFlow()

    val selecting = combine(selectedDates, selectedMonths) { dates, months ->
        if (dates.isNotEmpty()) Selection.Days else Selection.Months
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            Selection.Days
        )

    var reverseLayout by mutableStateOf(false)

    val filteredLogs = combine(
        poopLogs,
        mutableSelectedDates,
        mutableSelectedMonths,
        selecting
    ) { logs, days, months, selection ->
        val dates = days.map { it.toLocalDate() }
        logs.filter { log ->
            when (selection) {
                Selection.Days -> log.time.toLocalDate() in dates
                Selection.Months -> log.time.month in months
            }
        }
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    private val prevMonthsLength = List(monthsPrev) {
        if (it == 0) startDate.dayOfMonth
        else startDate.month.minus(it.toLong()).length(isLeapYear(startDate.year))
    }

    val totalDays = run {
        val total = prevMonthsLength.sum()
        // do this to start monday on the top
        val remainder =  total % 7
        total - (remainder - startDate.dayOfWeek.value)
    }

    fun current(days: Int): LocalDate {
        return startDate.minusDays((totalDays - days).toLong()).toLocalDate()
    }

    private val monthEndDates = (0..totalDays).mapNotNull { dayIdx ->

        val daysRange = totalDays - (dayIdx * 7) + 1..(totalDays - (dayIdx * 7) + 7)

        prevMonthsLength.mapIndexedNotNull { index, _ ->
            val sumOfPrevMonths = prevMonthsLength.take(index).sum()
            if (sumOfPrevMonths + prevMonthsLength[index] in daysRange) {
                dayIdx to startDate.month.minus(index.toLong())
            } else null
        }
            .firstOrNull()
    }

    val poopChartItems = combine(
        poopLogs,
        mutableSelectedDates,
        mutableSelectedMonths
    ) { logs, days, months ->
        buildList {
            (0..totalDays).forEach { i ->
                val month = monthEndDates.find { it.first == i }
               if (month != null) {
                    add(
                        PoopChartItem.MonthTag(
                            month = month.second,
                            amount = logs.count { it.time.month == month.second },
                            month.second in months
                        )
                    )
                } else if ((i * 7)  <= totalDays ) {
                    add(PoopChartItem.Blank)
                }
                for (j in 1..7) {
                    val currentDay = (i * 7) + j
                    if (currentDay <= totalDays) {
                        val day = startDate.minusDays((totalDays - currentDay).toLong())
                        val logsOnDay = logs.filter {
                            it.time.month == day.month &&
                            it.time.dayOfMonth == day.dayOfMonth &&
                            it.time.year == day.year
                        }
                        if (logsOnDay.isNotEmpty()) {
                            add(
                                PoopChartItem.DateWithLogs(
                                    day,
                                    logsOnDay.size,
                                    selected = day in days,
                                    start = day == startDate
                                )
                            )
                        } else {
                            add(
                                PoopChartItem.DateNoLogs(
                                    day,
                                    selected = day in days,
                                    start = day == startDate
                                )
                            )
                        }
                    }
                }
            }
        }
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun toggleMonth(month: Month) {
        mutableSelectedDates.update { emptyList() }
        mutableSelectedMonths.update {
            if (month in it) {
                it - month
            } else {
                it + month
            }
        }
    }

    fun toggleCollectionFilter(cid: String) {
        viewModelScope.launch {
            unselectedCollections.update {
                if (cid in it) {
                    it - cid
                } else {
                    it + cid
                }
            }
        }
    }

    fun toggleDate(date: LocalDateTime) {
        mutableSelectedMonths.update { emptyList() }
        mutableSelectedDates.update {
            if (date in it) {
                it - date
            } else {
                it + date
            }
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}

enum class Selection { Days, Months }

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

@Immutable
@Stable
sealed interface PoopChartItem {
    data class DateNoLogs(
        val day: LocalDateTime,
        val selected: Boolean,
        val start: Boolean
    ): PoopChartItem  {
        val dayOfMonth: String = day.dayOfMonth.toString()
    }

    data class DateWithLogs(
        val day: LocalDateTime,
        val amount: Int,
        val selected: Boolean,
        val start: Boolean,
    ): PoopChartItem
    data class MonthTag(val month: Month, val amount: Int, val selected: Boolean): PoopChartItem
    data object Blank: PoopChartItem
}
