import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.packlol.pootracker.ui.home.UiPoopLog
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.ceil

@Composable
private fun DaysSideText() {
    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
        listOf("", "mon", "tue", "wed", "thurs", "fri", "sat", "sun").forEach {
            Text(text = it)
        }
    }
}

@Composable
fun PoopChart(
    monthsPrev: Int,
    modifier: Modifier,
    dateBox: @Composable (LocalDateTime) -> Unit
) {
    val currentDate = rememberSaveable(
        saver = Saver(
            save = { it.toEpochSecond(ZoneOffset.UTC) },
            restore = { LocalDateTime.ofEpochSecond(it,0, ZoneOffset.UTC) }
        )
    ) { LocalDateTime.now() }

    val prevMonthsLength = remember(currentDate) {
        List(monthsPrev) {
            if (it == 0) currentDate.dayOfMonth
            else currentDate.month.minus(it.toLong()).length(isLeapYear(currentDate.year))
        }
    }

    val totalDays = remember(prevMonthsLength) {
        val total = prevMonthsLength.sum()
        val remainder =  total % 7
        total - (remainder - currentDate.dayOfWeek.value)
    }

    Row(modifier) {
        DaysSideText()
        LazyHorizontalGrid(
            rows = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentPadding = PaddingValues(4.dp),
            reverseLayout = true
        ) {
            for (i in 0.. totalDays) {
                item {
                    val daysRange = totalDays - (i * 7) + 1..(totalDays - (i * 7) + 7)
                    prevMonthsLength.forEachIndexed { index, l ->
                        val sumOfPrevMonths = prevMonthsLength.take(index).sum()
                        if (sumOfPrevMonths + prevMonthsLength[index] in daysRange) {
                            Text(text = currentDate.month.minus(index.toLong()).name.take(3))
                        } else {
                            Text(text = "")
                        }
                    }
                }
                for (j in 1..7) {
                    item {
                        val days = (i * 7) + j
                        if (days <= totalDays) {
                            Box(
                                Modifier.aspectRatio(1f)
                            ) {
                                dateBox(
                                    remember(currentDate){ currentDate.minusDays((totalDays - days).toLong()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isLeapYear(year: Int): Boolean {
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}
