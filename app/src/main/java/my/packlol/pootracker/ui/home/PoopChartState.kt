package my.packlol.pootracker.ui.home

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@Composable
fun rememberPoopChartState(
    poopLogs: List<UiPoopLog>,
    monthsPrev: Int = 12,
    startDate: LocalDateTime = remember { LocalDateTime.now() }
): PoopChartState {

    data class StateHolder(
        val dates: List<Long> = emptyList(),
        val months: List<Int> = emptyList(),
        val selected: PoopChartState.Selection = PoopChartState.Selection.Days
    )

    return rememberSaveable(
        poopLogs,
        startDate,
        saver = Saver(
            save = {value ->
                try {
                    Gson().toJson(
                        StateHolder(
                            dates = value.selectedDates.map { it.toEpochDay() },
                            months = value.selectedMonths.map { it.value },
                            selected = value.selecting
                        )
                    ).also {
                        Log.d("Saver", it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            },
            restore = { value ->
                val saved = try {
                    Gson().fromJson(value, StateHolder::class.java).also {
                        Log.d("Saver", "restored $it")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    StateHolder(emptyList(), emptyList(), PoopChartState.Selection.Days)
                }
                PoopChartState(
                    poopLogs = poopLogs,
                    initialMonths = saved.months.map { Month.of(it) },
                    initialDates = saved.dates.map { LocalDate.ofEpochDay(it) },
                    initialSelecting = saved.selected,
                    monthsPrev = monthsPrev,
                    startDate = startDate
                )
            }
        )
    ) {
        PoopChartState(
            poopLogs = poopLogs,
            startDate = startDate,
            monthsPrev = monthsPrev
        )
    }
}

class PoopChartState(
    val poopLogs: List<UiPoopLog>,
    val startDate: LocalDateTime,
    monthsPrev: Int,
    initialMonths: List<Month> = emptyList(),
    initialDates: List<LocalDate> = emptyList(),
    initialSelecting: Selection = Selection.Days
) {
    var selectedDates by mutableStateOf(initialDates)
        private set

    var selectedMonths by mutableStateOf(initialMonths)
        private set


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

    var selecting by mutableStateOf(initialSelecting)
        private set

    fun selectDate(date: LocalDate) {
        selectedDates = if (date in selectedDates) {
            selectedDates - date
        } else {
            selectedDates + date
        }
        selectedMonths = emptyList()
        selecting = Selection.Days
    }

    fun selectMonth(month: Month) {
        selectedMonths = if (month in selectedMonths) {
            selectedMonths - month
        } else {
            selectedMonths + month
        }
        selectedDates = emptyList()
        selecting = Selection.Months
    }

    fun clear() {
        selectedMonths = emptyList()
        selectedDates = emptyList()
    }

    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}