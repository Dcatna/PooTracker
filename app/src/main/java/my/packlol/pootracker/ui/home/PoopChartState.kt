package my.packlol.pootracker.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@Composable
fun rememberPoopChartState(
    poopLogs: List<UiPoopLog>,
    monthsPrev: Int = 12,
    startDate: LocalDateTime = remember { LocalDateTime.now() },
): PoopChartState {

    var selectedMonths by rememberSaveable {
        mutableStateOf(emptyList<Int>())
    }

    var selectedDates by rememberSaveable {
        mutableStateOf(emptyList<Long>())
    }

    var selected by rememberSaveable {
        mutableStateOf(PoopChartState.Selection.Days)
    }

    return remember(
        poopLogs,
        selectedDates,
        selectedMonths
    ) {
        PoopChartState(
            poopLogs = poopLogs,
            startDate = startDate,
            monthsPrev = monthsPrev,
            months = selectedMonths,
            days = selectedDates,
            selecting = selected,
            toggleDate = { date ->
                selectedDates = if (date.toEpochDay() in selectedDates) {
                    selectedDates - date.toEpochDay()
                } else {
                    selectedDates + date.toEpochDay()
                }
                selectedMonths = emptyList()
                selected = PoopChartState.Selection.Days
            },
            toggleMonth = { month ->
                selectedMonths = if (month.value in selectedMonths) {
                    selectedMonths - month.value
                } else {
                    selectedMonths + month.value
                }
                selectedDates = emptyList()
                selected = PoopChartState.Selection.Months
            },
            clear = {
                selectedMonths = emptyList()
                selectedDates = emptyList()
            }
        )
    }
}

class PoopChartState(
    val poopLogs: List<UiPoopLog>,
    val startDate: LocalDateTime,
    monthsPrev: Int,
    months: List<Int> = emptyList(),
    days: List<Long> = emptyList(),
    val selecting: Selection,
    val toggleDate: (LocalDate) -> Unit,
    val toggleMonth: (Month) -> Unit,
    val clear: () -> Unit
) {
    val selectedDates by derivedStateOf {
        days.map { LocalDate.ofEpochDay(it) }
    }

    val selectedMonths by derivedStateOf {
        months.map { Month.of(it) }
    }


    val filteredLogs by derivedStateOf {
        poopLogs.filter {
            when (selecting) {
                Selection.Days -> it.time.toLocalDate() in selectedDates
                Selection.Months -> it.time.month in selectedMonths
            }
        }
    }

    private val prevMonthsLength by derivedStateOf {
        List(monthsPrev) {
            if (it == 0) startDate.dayOfMonth
            else startDate.month.minus(it.toLong()).length(isLeapYear(startDate.year))
        }
    }

    val monthsPrev by derivedStateOf {
        List(monthsPrev) {
            if (it == 0) startDate.dayOfMonth
            else startDate.month.minus(it.toLong()).length(isLeapYear(startDate.year))
        }
    }

    val totalDays by derivedStateOf {
        val total = prevMonthsLength.sum()
        // do this to start monday on the top
        val remainder =  total % 7
        total - (remainder - startDate.dayOfWeek.value)
    }

    fun current(days: Int): LocalDate {
        return startDate.minusDays((totalDays - days).toLong()).toLocalDate()
    }

    fun monthEndDateInCol(dayIdx: Int): Month? {
        val daysRange = totalDays - (dayIdx * 7) + 1..(totalDays - (dayIdx * 7) + 7)
        prevMonthsLength.forEachIndexed { index, _ ->
            val sumOfPrevMonths = prevMonthsLength.take(index).sum()
            if(sumOfPrevMonths + prevMonthsLength[index] in daysRange) {
                return startDate.month.minus(index.toLong())
            }
        }
        return null
    }

    enum class Selection { Days, Months }

    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}