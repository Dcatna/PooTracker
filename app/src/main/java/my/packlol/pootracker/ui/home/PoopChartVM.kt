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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import my.packlol.pootracker.repository.PoopLogRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import javax.annotation.concurrent.Immutable

class PoopChartVM(
    poopLogRepository: PoopLogRepository
): ViewModel() {

    private val monthsPrev = 12
    private val startDate = LocalDateTime.now()

    private val poopLogs = poopLogRepository.observeAllPoopLogs()
        .map { list ->
            list.map { it.toUi() }
        }

    private val mutableSelectedDates = MutableStateFlow<List<LocalDateTime>>(emptyList())
   val selectedDates = mutableSelectedDates.asStateFlow()


    private val mutableSelectedMonths = MutableStateFlow<List<Month>>(emptyList())
    val selectedMonths = mutableSelectedMonths.asStateFlow()

    private val mutableSelecting = MutableStateFlow<Selection>(Selection.Days)
    val selecting = mutableSelecting.asStateFlow()

    var reverseLayout by mutableStateOf(false)

    val filteredLogs = combine(
        poopLogs,
        mutableSelectedDates,
        mutableSelectedMonths,
        mutableSelecting
    ) { logs, days, months, selection ->
        logs.filter { log ->
            when (selection) {
                Selection.Days -> log.time in days
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
        mutableSelectedMonths.update {
            if (month in it) {
                it - month
            } else {
                it + month
            }
        }
    }
    fun toggleDate(date: LocalDateTime) {
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
    ): PoopChartItem {
        val dayOfMonth: String = day.dayOfMonth.toString()
    }
    data class MonthTag(val month: Month, val amount: Int, val selected: Boolean): PoopChartItem
    data object Blank: PoopChartItem
}
