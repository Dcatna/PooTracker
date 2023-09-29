package my.packlol.pootracker.ui.home

import PoopChart
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult.*
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.date_time.DateTimeDialog
import com.maxkeppeler.sheets.date_time.models.DateTimeConfig
import com.maxkeppeler.sheets.date_time.models.DateTimeSelection
import kotlinx.coroutines.launch
import my.packlol.pootracker.PoopAppState
import my.packlol.pootracker.PullRefresh
import my.packlol.pootracker.ui.home.PoopChartState.Selection.Days
import my.packlol.pootracker.ui.home.PoopChartState.Selection.Months
import my.packlol.pootracker.ui.theme.conditional
import my.packlol.pootracker.ui.theme.drawEndDivider
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun HomeScreen(
    poopAppState: PoopAppState,
) {
    val homeVM = koinViewModel<HomeVM>()

    val state by homeVM.homeUiState.collectAsState()

    val scope = rememberCoroutineScope()

    HomeScreen(
        state = state,
        logPoop = {

        },
        deleteLog = { log ->
            homeVM.deleteLog(log)
            scope.launch {
                val result = poopAppState.showSnackbarWithAction(
                    message = "Undo delete",
                    withDismissAction = true,
                    actionLabel = "undo",
                    duration = SnackbarDuration.Short
                )
                when (result) {
                    Dismissed -> Unit
                    ActionPerformed -> homeVM.undoDelete(log)
                }
            }
        },
        refresh = {
            homeVM.refresh()
        }
    )
}

object LocalDateTimeSaver: Saver<MutableState<LocalDateTime?>, Long> {
    override fun restore(value: Long): MutableState<LocalDateTime?> {
        return if (value == -1L) {
            mutableStateOf(null)
        } else {
            mutableStateOf(LocalDateTime.ofEpochSecond(value, 0, ZoneOffset.UTC))
        }
    }

    override fun SaverScope.save(value: MutableState<LocalDateTime?>): Long {
        return value.value?.toEpochSecond(ZoneOffset.UTC) ?: -1L
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun HomeScreen(
    state: HomeUiState,
    logPoop: (collection: UiCollection) -> Unit,
    deleteLog: (UiPoopLog) -> Unit,
    refresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {

        val clockState = rememberClockState()

        var dateTimeDialogVisible by rememberSaveable {
            mutableStateOf(false)
        }

        var selectedDateTime by rememberSaveable(saver = LocalDateTimeSaver) {
            mutableStateOf<LocalDateTime?>(null)
        }

        if (dateTimeDialogVisible) {
            Popup {
                DateTimeDialog(
                    state = rememberUseCaseState(
                        visible = true,
                        embedded = true,
                        onDismissRequest = {
                            clockState.resume()
                            dateTimeDialogVisible = false
                        }
                    ),
                    config = DateTimeConfig(
                        minYear = LocalDateTime.now().year - 1,
                        maxYear = LocalDateTime.now().year,
                    ),
                    selection = DateTimeSelection.DateTime(
                        selectedTime = clockState.time.toLocalTime(),
                        selectedDate = clockState.time.toLocalDate(),
                        onPositiveClick =  { newDateTime ->
                            clockState.pauseTimeAt(newDateTime)
                            dateTimeDialogVisible = false
                        },
                        onNegativeClick = {
                            clockState.resume()
                            dateTimeDialogVisible = false
                        }
                    ),
                )
            }
        }

        val configuration = LocalConfiguration.current

        selectedDateTime?.let {
            var selectedCollection by remember {
                mutableStateOf(state.collections.firstOrNull())
            }

            AlertDialog(
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .padding(bottom = 128.dp)
                    .widthIn(max = configuration.screenWidthDp.dp - 80.dp),
                onDismissRequest = {
                    selectedDateTime = null
                },
                title = {
                    Column {
                        Text(
                            "Log Poop At",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = formatDate(it.toLocalDate()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatTime(it.toLocalTime()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                text = {
                    Column {
                        Divider()
                        Text(
                            text = "Log To Collection",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                        FlowRow {
                            state.collections.fastForEach { collection ->
                                AssistChip(
                                    onClick = {
                                        selectedCollection = collection
                                    },
                                    label = {
                                        Text(collection.name)
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Text(
                        text = "Log",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                selectedDateTime = null
                                selectedCollection?.let {
                                    logPoop(it)
                                }
                            },
                    )
                }
            )
        }

        val poopChartState = rememberPoopChartState(poopLogs = state.logs)

        PoopChart(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f),
            poopChartState = poopChartState,
            onMonthClick = {
                poopChartState.toggleMonth(it)
            },
            onDateClick = {
                poopChartState.toggleDate(it)
            }
        )
        DateWithChangeableTime(
            state = clockState,
            onTimeIconClick = {
                clockState.pauseTimeAt(clockState.time)
                dateTimeDialogVisible = !dateTimeDialogVisible
            }
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Button(
                onClick = {
                    selectedDateTime = clockState.time
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
        LazyRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            when (poopChartState.selecting) {
                Days -> itemsIndexed(
                    items = poopChartState.selectedDates,
                    key = { i, v -> v }
                ) { i, date ->
                    Row(Modifier
                        .conditional(
                            when (i) {
                                0 -> poopChartState.selectedDates.lastIndex != 0
                                else -> i != poopChartState.selectedDates.lastIndex
                            }
                        ) {
                            drawEndDivider(0.8f)
                        }
                        .animateItemPlacement()
                        .clickable {
                            poopChartState.toggleDate(date)
                        }
                    ) {
                        Text(
                            text = formatDate(date),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                Months -> itemsIndexed(
                    items = poopChartState.selectedMonths,
                    key = { i, v ->  v.value }
                ) {i, month ->
                    Row(
                        Modifier
                            .conditional(
                                when (i) {
                                    0 -> poopChartState.selectedMonths.lastIndex != 0
                                    else -> i != poopChartState.selectedMonths.lastIndex
                                }
                            ) {
                                drawEndDivider(0.8f)
                            }
                            .animateItemPlacement()
                            .clickable {
                                poopChartState.toggleMonth(month)
                            }
                    ) {
                        Text(
                            text =  month.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .padding(4.dp)
                                .animateItemPlacement()
                        )
                    }
                }
            }
        }
        PullRefresh(
            refreshing = state.syncing,
            onRefresh = { refresh() }
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = poopChartState.filteredLogs,
                    key = { it.id }
                ) { log ->
                    PoopListItem(
                        log = log,
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        deleteLog(log)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoopListItem(
    log: UiPoopLog,
    modifier: Modifier,
    swipeToDelete: () -> Unit
) {
    val dismissState = rememberDismissState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(dismissState.isDismissed(DismissDirection.EndToStart)) {
        if (dismissState.isDismissed(DismissDirection.EndToStart)) {
            swipeToDelete()
            scope.launch {
                dismissState.reset()
            }
        }
    }

    SwipeToDismiss(
        state = dismissState,
        modifier = modifier,
        directions = setOf(DismissDirection.EndToStart),
        background = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "delete"
                        )
                        Text("delete log")
                    }
                }
            }
        },
        dismissContent = {
            Surface(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center) {
                    Text(
                        text = formatDate(log.time.toLocalDate()),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                    )
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text(
                            text = formatTime(log.time.toLocalTime()),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = daysSinceCreated(
                                (LocalDate.now().toEpochDay() - log.time.toLocalDate().toEpochDay()).toInt()
                            ),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Divider()
                }
            }
        }
    )
}

fun daysSinceCreated(days: Int): String {
    return if (days >= 365) {
        val yearsAgo = days / 365
        if (yearsAgo <= 1.0) {
            "last year"
        } else {
            "${days / 365} years ago"
        }
    } else {
        if (days <= 0.9) {
            "today"
        } else {
            "$days days ago"
        }
    }
}


private fun formatTime(t: LocalTime) :String {
    val hour = when {
        t.hour > 12 -> (t.hour - 12)
        t.hour == 0 -> 12
        else -> t.hour
    }
    val minute = if(t.minute < 10) {
        "0${t.minute}"
    } else t.minute.toString()
    return "$hour : $minute ${if (t.hour >= 12) "pm" else "am"}"
}

private fun formatDate(d: LocalDate): String {
    return "${d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}," +
            " ${d.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}, ${d.year}"
}