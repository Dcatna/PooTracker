package my.packlol.pootracker.ui.home

import PoopChart
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult.*
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.date_time.DateTimeDialog
import com.maxkeppeler.sheets.date_time.models.DateTimeConfig
import com.maxkeppeler.sheets.date_time.models.DateTimeSelection
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonHighlightAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.rememberBalloonBuilder
import com.skydoves.balloon.compose.setBackgroundColor
import com.skydoves.balloon.compose.setTextColor
import kotlinx.coroutines.launch
import my.packlol.pootracker.PoopAppState
import my.packlol.pootracker.PullRefresh
import my.packlol.pootracker.SettingsDialogThemeChooserRow
import my.packlol.pootracker.repository.AuthState
import my.packlol.pootracker.ui.auth.pooEmoji
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
        onCollectionFilterClick = { cid ->
            homeVM.toggleCollectionFilter(cid)
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    onCollectionFilterClick: (cid: String) -> Unit
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

    val poopChartState = rememberPoopChartState(poopLogs = state.logs)

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

        FilterBottomSheet(
            visible = filterBottomSheetVisible,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            reverseChartDirection = poopChartState.isReverseLayout,
            onChartDirectionChange = {
                poopChartState.reverseLayout(it)
            },
            byDateAsc = byDateAsc,
            onByDateAscChange = { byDateAsc = it },
            onDismiss = { filterBottomSheetVisible = false },
        )

        selectedDateTime?.let {
            LogPoopAlertDialog(
                selectedDateTime = it,
                collections = state.collections,
                onDismiss = { selectedDateTime = null },
                onConfirmButtonClick = { collection ->
                    logPoop(it, collection)
                    selectedDateTime = null
                }
            )
        }

        EditCollectionsDialog(
            visible = editCollectionsDialogVisible,
            onDismiss = { editCollectionsDialogVisible = false },
            collections = state.collections,
            onCollectionAdd = onCollectionAdd,
            onCollectionDelete = onCollectionDelete,
            onCollectionEdit = onCollectionEdit,
            signedIn = authState is AuthState.LoggedIn
        )

        var expanded by rememberSaveable {
            mutableStateOf(true)
        }

        if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded || windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium) {
            Row {
                Column(Modifier.fillMaxHeight().weight(1f)) {
                    LazyRow {
                        items(state.collections) { collection ->
                            FilterChip(
                                selected = collection in state.selectedCollections,
                                onClick = { onCollectionFilterClick(collection.id) },
                                label = { Text(text = collection.name) },
                            )
                        }
                    }
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
                                .weight(1f),
                            poopChartState = poopChartState,
                            onMonthClick = {
                                poopChartState.toggleMonth(it)
                            },
                            onDateClick = {
                                poopChartState.toggleDate(it)
                            }
                        )
                    }
                }
               Column(Modifier.fillMaxHeight().weight(1f)) {
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
                           Text("Log Poop     $pooEmoji")
                       }
                       EditCollectionsButton {
                           editCollectionsDialogVisible = true
                       }
                   }

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
                           .padding(horizontal = 32.dp),
                       verticalAlignment = Alignment.CenterVertically,
                       horizontalArrangement = Arrangement.SpaceBetween
                   ) {
                       Text(
                           text = "currently showing logs for.",
                           style = MaterialTheme.typography.labelLarge,
                       )
                       IconButton(onClick = { filterBottomSheetVisible = !filterBottomSheetVisible }) {
                           Icon(
                               imageVector = Icons.Outlined.FilterList,
                               contentDescription = "filter"
                           )
                       }
                   }
                   LazyRow(
                       modifier = Modifier.fillMaxWidth()
                   ) {
                       when (poopChartState.selecting) {
                           Days -> itemsIndexed(
                               items = poopChartState.selectedDates,
                               key = { _, v -> v }
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
                               key = { _, v -> v.value }
                           ) { i, month ->
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
                                       text = month.name.lowercase().replaceFirstChar { it.uppercase() },
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
                       val sortedLogs = remember(poopChartState.filteredLogs, byDateAsc) {
                           poopChartState.filteredLogs
                               .conditional(
                                   condition = byDateAsc,
                                   whatIf = { sortedBy { it.time } },
                                   whatElse = { sortedByDescending { it.time } }
                               )
                       }
                       if (
                           (poopChartState.selectedDates.isNotEmpty() ||
                                   poopChartState.selectedMonths.isNotEmpty()) &&
                           poopChartState.filteredLogs.isEmpty()
                       ) {
                           Box(modifier = Modifier.fillMaxSize()) {
                               Text(
                                   text = "No logs for selected dates",
                                   modifier = Modifier.align(Alignment.Center)
                               )
                           }
                       } else if (
                           poopChartState.selectedDates.isEmpty() &&
                           poopChartState.selectedMonths.isEmpty()
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
        } else {
            Column(
                Modifier.fillMaxSize()
            ) {
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
                                .fillMaxHeight(0.4f),
                            poopChartState = poopChartState,
                            onMonthClick = {
                                poopChartState.toggleMonth(it)
                            },
                            onDateClick = {
                                poopChartState.toggleDate(it)
                            }
                        )
                    }
                }
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
                        .clickable { expanded = !expanded }
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
            LazyRow(
                Modifier.padding(start = 8.dp)
            ) {
                items(state.collections) { collection ->
                    FilterChip(
                        selected = collection in state.selectedCollections,
                        onClick = { onCollectionFilterClick(collection.id) },
                        label = { Text(text = collection.name) }
                    )
                }
            }


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
                    Text("Log Poop     $pooEmoji")
                }
                EditCollectionsButton {
                    editCollectionsDialogVisible = true
                }
            }

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
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "currently showing logs for.",
                    style = MaterialTheme.typography.labelLarge,
                )
                IconButton(onClick = { filterBottomSheetVisible = !filterBottomSheetVisible }) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = "filter"
                    )
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                when (poopChartState.selecting) {
                    Days -> itemsIndexed(
                        items = poopChartState.selectedDates,
                        key = { _, v -> v }
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
                        key = { _, v -> v.value }
                    ) { i, month ->
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
                                text = month.name.lowercase().replaceFirstChar { it.uppercase() },
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
                val sortedLogs = remember(poopChartState.filteredLogs, byDateAsc) {
                    poopChartState.filteredLogs
                        .conditional(
                            condition = byDateAsc,
                            whatIf = { sortedBy { it.time } },
                            whatElse = { sortedByDescending { it.time } }
                        )
                }
                if (
                    (poopChartState.selectedDates.isNotEmpty() ||
                            poopChartState.selectedMonths.isNotEmpty()) &&
                    poopChartState.filteredLogs.isEmpty()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "No logs for selected dates",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else if (
                    poopChartState.selectedDates.isEmpty() &&
                    poopChartState.selectedMonths.isEmpty()
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditCollectionsButton(
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Balloon(
        builder = rememberBalloonBuilder {
            setText("Edit the collections that contain poop logs.")
            setArrowSize(10)
            setWidth(BalloonSizeSpec.WRAP)
            setHeight(BalloonSizeSpec.WRAP)
            setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            setArrowOrientation(ArrowOrientation.BOTTOM)
            setArrowPosition(0.5f)
            setBalloonAnimation(BalloonAnimation.FADE)
            setBalloonHighlightAnimation(BalloonHighlightAnimation.SHAKE)
            setPadding(8)
            setMarginHorizontal(8)
            setCornerRadius(8f)
            setTextColor(onPrimary)
            setBackgroundColor(primaryColor)
        }
    ) { balloonWindow ->
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(40.0.dp)
                .clip(CircleShape)
                .background(color = Color.Transparent)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { balloonWindow.showAlignTop() },
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(
                        bounded = false,
                        radius = 40.0.dp / 2
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                contentDescription = "edit",
                imageVector = Icons.Outlined.Edit,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditCollectionsDialog(
    visible: Boolean,
    signedIn: Boolean,
    onDismiss: () -> Unit,
    onCollectionAdd: (name: String, offline: Boolean) -> Unit,
    onCollectionDelete: (cid: String) -> Unit,
    onCollectionEdit: (cid: String, name: String) -> Unit,
    collections: List<UiCollection>,
) {
    if (visible) {
        val configuration = LocalConfiguration.current

        var selectedCollection by remember {
            mutableStateOf<UiCollection?>(null)
        }
        var addingCollection by remember {
            mutableStateOf(false)
        }

        var name by remember {
            mutableStateOf("")
        }

        var offlineCollection by remember {
            mutableStateOf(!signedIn)
        }

        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .padding(bottom = 128.dp)
                .widthIn(max = configuration.screenWidthDp.dp - 80.dp),
            onDismissRequest = onDismiss,
            text = {
                Column {
                    Divider()
                    AnimatedContent(
                        targetState = Pair(selectedCollection, addingCollection),
                        label = "collections-edit"
                    ) {(collection, adding) ->
                        Column {
                            if (collection != null) {

                                LaunchedEffect(key1 = Unit) {
                                    name = collection.name
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        maxLines = 1,
                                        label = { Text("collection name") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            onCollectionDelete(collection.id)
                                            onDismiss()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "delete"
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { selectedCollection = null }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "back"
                                    )
                                }
                            } else if (adding) {
                                LaunchedEffect(key1 = Unit) {
                                    name = ""
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        maxLines = 1,
                                        label = { Text("collection name") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "Save Collection For",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                )
                                Column(Modifier.selectableGroup()) {
                                    if (signedIn) {
                                        SettingsDialogThemeChooserRow(
                                            text = "Current User",
                                            selected = !offlineCollection,
                                            onClick = { offlineCollection = false },
                                        )
                                    }
                                    SettingsDialogThemeChooserRow(
                                        text = "Offline",
                                        selected = offlineCollection,
                                        onClick = { offlineCollection = true },
                                    )
                                }
                                IconButton(
                                    onClick = { addingCollection = false }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "back"
                                    )
                                }
                            } else {
                                Text(
                                    text = "Select a Collection to Edit",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                )
                                FlowRow {
                                    collections.fastForEach { collection ->
                                        AssistChip(
                                            onClick = {
                                                selectedCollection = collection
                                            },
                                            label = {
                                                Text(collection.name)
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = { addingCollection = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "add"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            title = {
                Text(
                    "Edit Collections",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            dismissButton = {
                Text(
                    text = "cancel",
                    Modifier.clickable {
                        onDismiss()
                    }
                )
            },
            confirmButton = {
                selectedCollection?.let {
                    Text(
                        text = "save",
                        Modifier.clickable {
                            onCollectionEdit(it.id, it.name)
                            onDismiss()
                        }
                    )
                }
                if (addingCollection) {
                    Text(
                        text = "save",
                        Modifier.clickable {
                            onCollectionAdd(name, offlineCollection)
                            onDismiss()
                        }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Reset(dismissState: DismissState, action: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = dismissState.dismissDirection) {
        scope.launch {
            dismissState.reset()
            action()
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

    when  {
        dismissState.isDismissed(DismissDirection.EndToStart) ->
            Reset(dismissState = dismissState) {
                swipeToDelete()
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
            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
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


fun formatTime(t: LocalTime) :String {
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