package my.packlol.pootracker.ui.home

import PoopChart
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.SnackbarResult.Dismissed
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.date_time.DateTimeDialog
import com.maxkeppeler.sheets.date_time.models.DateTimeConfig
import com.maxkeppeler.sheets.date_time.models.DateTimeSelection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.packlol.pootracker.PoopAppState
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.ui.LocalDateTimeSaver
import my.packlol.pootracker.ui.PullRefresh
import my.packlol.pootracker.ui.auth.pooEmoji
import my.packlol.pootracker.ui.home.Selection.*
import my.packlol.pootracker.ui.shared.BannerAd
import my.packlol.pootracker.ui.theme.conditional
import my.packlol.pootracker.ui.theme.drawEndDivider
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

@Composable
fun HomeScreen(
    poopAppState: PoopAppState,
) {
    val homeVM = koinViewModel<HomeVM>()

    val state by homeVM.homeUiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = Unit) {
        homeVM.errors.collect {
            when(it) {
                HomeError.FailedToAdd -> poopAppState.showSnackbar("Failed to add poop log")
                HomeError.FailedToDelete -> poopAppState.showSnackbar("Failed to delete poop log")
                HomeError.FailedToUpdateCollection -> poopAppState.showSnackbar("Failed to update collection")
            }
        }
    }

    HomeScreen(
        state = state,
        logPoop = { time, collection ->
            homeVM.logPoop(
                time, collection.id
            )
        },
        windowSizeClass = poopAppState.windowSizeClass,
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
        authState = poopAppState.authState,
        refresh = {
            homeVM.refresh()
        },
        onCollectionEdit = { cid, name ->
            homeVM.editCollection(name, cid)
        },
        onCollectionDelete = {cid ->
            homeVM.deleteCollection(cid)
        },
        onCollectionAdd = { name, offline ->
            homeVM.addCollection(name, offline)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: HomeUiState,
    authState: AuthState,
    windowSizeClass: WindowSizeClass,
    logPoop: (time: LocalDateTime, collection: UiCollection) -> Unit,
    deleteLog: (UiPoopLog) -> Unit,
    refresh: () -> Unit,
    onCollectionAdd: (name: String, offline: Boolean) -> Unit,
    onCollectionDelete: (cid: String) -> Unit,
    onCollectionEdit: (cid: String, name: String) -> Unit,
    poopChartVM: PoopChartVM = koinViewModel()
) {


    val clockState = rememberClockState()

    var dateTimeDialogVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var editCollectionsDialogVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedDateTime by rememberSaveable(saver = LocalDateTimeSaver) {
        mutableStateOf(null)
    }

    var filterBottomSheetVisible by rememberSaveable {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        onPositiveClick = { newDateTime ->
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

        var byDateAsc by rememberSaveable {
            mutableStateOf(false)
        }

        val collections by poopChartVM.collections.collectAsStateWithLifecycle()
        val selectedCollections by poopChartVM.selectedCollections.collectAsStateWithLifecycle()

        FilterBottomSheet(
            visible = filterBottomSheetVisible,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            reverseChartDirection = poopChartVM.reverseLayout,
            onChartDirectionChange = {
                poopChartVM.reverseLayout = it
            },
            byDateAsc = byDateAsc,
            onByDateAscChange = { byDateAsc = it },
            onDismiss = { filterBottomSheetVisible = false },
        )

        val useSideBySide =
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded ||
                    windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium

        selectedDateTime?.let {
            LogPoopAlertDialog(
                selectedDateTime = it,
                collections = collections,
                onDismiss = { selectedDateTime = null },
                onConfirmButtonClick = { collection ->
                    logPoop(it, collection)
                    selectedDateTime = null
                },
                vertical = !useSideBySide
            )
        }

        EditCollectionsDialog(
            visible = editCollectionsDialogVisible,
            onDismiss = { editCollectionsDialogVisible = false },
            collections = collections,
            onCollectionAdd = onCollectionAdd,
            onCollectionDelete = onCollectionDelete,
            onCollectionEdit = onCollectionEdit,
            signedIn = authState is AuthState.LoggedIn
        )

        var expanded by rememberSaveable {
            mutableStateOf(true)
        }

        val selecting by poopChartVM.selecting.collectAsStateWithLifecycle()
        val selectedMonths by poopChartVM.selectedMonths.collectAsStateWithLifecycle()
        val selectedDates by poopChartVM.selectedDates.collectAsStateWithLifecycle()
        val filteredLogs by poopChartVM.filteredLogs.collectAsStateWithLifecycle()

        if (useSideBySide) {
            Row {
                Column(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    CollectionsLazyRow(
                        collections = collections,
                        selected = selectedCollections,
                        onCollectionClick = {
                            poopChartVM.toggleCollectionFilter(it)
                        }
                    )
                    AnimatedVisibility(
                        visible = expanded,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically(animationSpec = tween(500)) { -it } + fadeOut()
                    ) {
                        PoopChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            poopChartVM = poopChartVM
                        )
                    }
                }
                LogPoopContentWithLogs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    clockState = clockState,
                    state = state,
                    logPoopButtonClick = {
                        selectedDateTime = clockState.time
                    },
                    toggleEditCollectionsVisibility = { editCollectionsDialogVisible = !editCollectionsDialogVisible },
                    toggleDateTimeDialog = { dateTimeDialogVisible = !dateTimeDialogVisible },
                    toggleFilterBottomSheet = { filterBottomSheetVisible = !filterBottomSheetVisible },
                    refresh = refresh,
                    deleteLog = deleteLog,
                    byDateAsc = byDateAsc,
                    selecting = selecting,
                    filteredLogs = filteredLogs,
                    toggleMonth = poopChartVM::toggleMonth,
                    toggleDate = poopChartVM::toggleDate,
                    selectedDates = selectedDates,
                    selectedMonths = selectedMonths
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize()
            ) {
                BannerAd()
                Column {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically(
                            animationSpec = tween(500)
                        ) { -it } + fadeOut()
                    ) {
                        PoopChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.4f)
                        )
                    }
                }
                FilterCollectionsTagCalenderHide(
                    toggleCalendar = { expanded = !expanded },
                    expanded = expanded
                )
                CollectionsLazyRow(
                    collections = collections,
                    selected = selectedCollections,
                    onCollectionClick = {
                        poopChartVM.toggleCollectionFilter(it)
                    }
                )
                LogPoopContentWithLogs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    clockState = clockState,
                    state = state,
                    logPoopButtonClick = {
                        selectedDateTime = clockState.time
                    },
                    toggleEditCollectionsVisibility = { editCollectionsDialogVisible = !editCollectionsDialogVisible },
                    toggleDateTimeDialog = { dateTimeDialogVisible = !dateTimeDialogVisible },
                    toggleFilterBottomSheet = { filterBottomSheetVisible = !filterBottomSheetVisible },
                    refresh = refresh,
                    deleteLog = deleteLog,
                    byDateAsc = byDateAsc,
                    selecting = selecting,
                    filteredLogs = filteredLogs,
                    toggleMonth = poopChartVM::toggleMonth,
                    toggleDate = poopChartVM::toggleDate,
                    selectedDates = selectedDates,
                    selectedMonths = selectedMonths
                )
            }
        }
    }
}

@Composable
fun CollectionsLazyRow(
    collections: List<UiCollection>,
    selected:  List<UiCollection>,
    onCollectionClick: (String) -> Unit,
) {
    LazyRow(
        Modifier.padding(start = 8.dp)
    ) {
        items(collections) { collection ->
            var toggleState by remember(collections, selected) {
                mutableStateOf(ToggleableState(collection in selected))
            }

            LaunchedEffect(key1 = toggleState) {
                if (toggleState == ToggleableState.Indeterminate) {
                    delay(2000)
                    toggleState = ToggleableState(collection in selected)
                }
            }

            TriStateFilterChip(
                state = toggleState,
                toggleState = {
                    when (it) {
                        ToggleableState.On -> {
                            toggleState = it
                            onCollectionClick(collection.id)
                        }
                        ToggleableState.Off -> {
                            toggleState = it
                            onCollectionClick(collection.id)
                        }
                        ToggleableState.Indeterminate -> {
                            toggleState = it
                        }
                    }
                },
                name = collection.name
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun FilterCollectionsTagCalenderHide(
    toggleCalendar: () -> Unit,
    expanded: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        val colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
        Text(
            text = "filter by collections.",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { toggleCalendar() }
        ) {
            Icon(
                imageVector = if (expanded)
                    Icons.Filled.KeyboardArrowUp
                else
                    Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .background(
                        Brush.radialGradient(colors = colors.asReversed())
                    )
            )
            Text(
                text = if (expanded)
                    "hide calendar"
                else
                    "show calendar",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogPoopContentWithLogs(
    modifier: Modifier, // fill max weight 1f
    selecting: Selection,
    selectedDates: List<LocalDateTime>,
    selectedMonths: List<Month>,
    clockState: ClockState,
    state: HomeUiState,
    toggleEditCollectionsVisibility: () -> Unit,
    toggleDateTimeDialog: () -> Unit,
    toggleFilterBottomSheet: () -> Unit,
    logPoopButtonClick: () -> Unit,
    refresh: () -> Unit,
    deleteLog: (UiPoopLog) -> Unit,
    byDateAsc: Boolean,
    toggleDate: (date: LocalDateTime) -> Unit,
    toggleMonth: (month: Month) -> Unit,
    filteredLogs: List<UiPoopLog>
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Button(
                onClick = {
                    logPoopButtonClick()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text("Log Poop     $pooEmoji")
            }
            EditCollectionsButton {
                toggleEditCollectionsVisibility()
            }
        }

        DateWithChangeableTime(
            state = clockState,
            onTimeIconClick = {
                clockState.pauseTimeAt(clockState.time)
                toggleDateTimeDialog()
            }
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "currently showing logs for.",
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(onClick = {
                toggleFilterBottomSheet()
            }) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "filter"
                )
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            when (selecting) {
                Days -> itemsIndexed(
                    items = selectedDates,
                    key = { _, v -> v }
                ) { i, date ->
                    Row(Modifier
                        .conditional(
                            when (i) {
                                0 -> selectedDates.lastIndex != 0
                                else -> i != selectedDates.lastIndex
                            }
                        ) {
                            drawEndDivider(0.8f)
                        }
                        .animateItemPlacement()
                        .clickable {
                            toggleDate(date)
                        }
                    ) {
                        Text(
                            text = formatDate(date.toLocalDate()),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                Months -> itemsIndexed(
                    items = selectedMonths,
                    key = { _, v -> v.value }
                ) { i, month ->
                    Row(
                        Modifier
                            .conditional(
                                when (i) {
                                    0 -> selectedMonths.lastIndex != 0
                                    else -> i != selectedMonths.lastIndex
                                }
                            ) {
                                drawEndDivider(0.8f)
                            }
                            .animateItemPlacement()
                            .clickable {
                                toggleMonth(month)
                            }
                    ) {
                        Text(
                            text = month.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
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
            val sortedLogs = remember(filteredLogs, byDateAsc) {
               filteredLogs
                    .conditional(
                        condition = byDateAsc,
                        whatIf = { sortedBy { it.time } },
                        whatElse = { sortedByDescending { it.time } }
                    )
            }
            if (
                (selectedDates.isNotEmpty() || selectedMonths.isNotEmpty())
                && filteredLogs.isEmpty()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "No logs for selected dates",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else if (
               selectedDates.isEmpty() &&
               selectedMonths.isEmpty()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Click a day to see poop logs.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = sortedLogs,
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
}

private fun formatDate(d: LocalDate): String {
    return "${d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}," +
            " ${d.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}, ${d.year}"
}