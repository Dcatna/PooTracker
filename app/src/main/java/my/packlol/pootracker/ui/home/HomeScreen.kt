package my.packlol.pootracker.ui.home

import PoopChart
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.date_time.DateTimeDialog
import com.maxkeppeler.sheets.date_time.models.DateTimeConfig
import com.maxkeppeler.sheets.date_time.models.DateTimeSelection
import my.packlol.pootracker.ui.home.PoopChartState.Selection.Days
import my.packlol.pootracker.ui.home.PoopChartState.Selection.Months
import my.packlol.pootracker.ui.navigation.Screen
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun HomeScreen(
    navigate: (Screen) -> Unit
) {
    val homeVM = koinViewModel<HomeVM>()

    val state by homeVM.homeUiState.collectAsState()

    HomeScreen(
        navigate = navigate,
        state = state,
        onLogButtonClick = homeVM::logPoop
    )
}

@Composable
private fun HomeScreen(
    navigate: (Screen) -> Unit = {},
    state: HomeUiState,
    onLogButtonClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {

        var selectedLogDate by remember {
            mutableStateOf<LocalDateTime>(LocalDateTime.now())
        }
        var dateTimeDialogVisible by rememberSaveable {
            mutableStateOf(false)
        }

        if (dateTimeDialogVisible) {
            Popup {
                DateTimeDialog(
                    state = rememberUseCaseState(
                        visible = true,
                        embedded = true,
                        onDismissRequest = {
                            dateTimeDialogVisible = false
                        }
                    ),
                    config = DateTimeConfig(),
                    selection = DateTimeSelection.DateTime(
                        selectedTime = selectedLogDate.toLocalTime(),
                        selectedDate = selectedLogDate.toLocalDate(),
                        onPositiveClick =  { newDateTime ->
                            selectedLogDate = newDateTime
                            dateTimeDialogVisible = false
                        },
                        onNegativeClick = {
                            dateTimeDialogVisible = false
                        }
                    ),
                )
            }
        }

        val poopChartState = rememberPoopChartState(poopLogs = state.logs)

        PoopChart(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f),
            poopChartState = poopChartState,
            onMonthClick = {
                poopChartState.selectMonth(it)
            },
            onDateClick = {
                poopChartState.selectDate(it)
            }
        )


        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)) {
            Button(
                onClick = {
                    dateTimeDialogVisible = true
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text("Log Poop")
            }
        }

        Text(
            text = "currently showing logs for.",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = when (poopChartState.selecting) {
                Days -> poopChartState.selectedDates.joinToString {
                    formatDate(it)
                }
                Months -> poopChartState.selectedMonths.joinToString { month ->
                    month.name.lowercase().replaceFirstChar { it.uppercase() }
                }
            },
            style = MaterialTheme.typography.labelSmall
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(poopChartState.filteredLogs) { log ->
                Row {
                    Text(
                        "$log"
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (log.synced) Color.Green
                                else Color.Red
                            )
                    )
                }
            }
        }
    }
}

private fun formatDate(d: LocalDate): String {
    return "${d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}," +
            " ${d.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}, ${d.year}"
}