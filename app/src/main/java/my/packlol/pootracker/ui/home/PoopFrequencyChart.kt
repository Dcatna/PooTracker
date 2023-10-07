
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonHighlightAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.setBackgroundColor
import com.skydoves.balloon.compose.setTextColor
import my.packlol.pootracker.ui.home.PoopChartState
import my.packlol.pootracker.ui.theme.conditional
import my.packlol.pootracker.ui.theme.isScrollingUp
import java.time.LocalDate
import java.time.Month

@Composable
private fun DaysSideText(
    modifier: Modifier
) {

    Column(
        modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf("", "mon", "tue", "wed", "thur", "fri", "sat", "sun").forEach {
            Box(modifier = Modifier.weight(1f, true), contentAlignment = Alignment.Center) {
                Text(
                    text = it,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                )
            }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PoopChart(
    modifier: Modifier,
    poopChartState: PoopChartState,
    onDateClick: (LocalDate) -> Unit,
    onMonthClick: (Month) -> Unit
) {
    Row(
        modifier,
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val onPrimary = MaterialTheme.colorScheme.onPrimary
        val context = LocalContext.current

        val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = poopChartState.totalDays)

        val isScrollingUp by gridState.isScrollingUp()

        val hideSideText by remember {
            derivedStateOf {
                isScrollingUp && gridState.isScrollInProgress && poopChartState.totalDays - gridState.firstVisibleItemIndex > 80
            }
        }

        val width by animateDpAsState(
            label = "days-anim",
            targetValue = if(hideSideText) {
                0.dp
            } else {
                40.dp
            },
            animationSpec = tween(
                200,
                delayMillis = 50
            )
        )

        if (poopChartState.isReverseLayout) {
            DaysSideText(
                Modifier
                    .fillMaxHeight()
                    .width(width)
            )
        }
        LazyHorizontalGrid(
            rows = GridCells.Fixed(8),
            state = gridState,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f, true),
            contentPadding = PaddingValues(4.dp),
            reverseLayout = poopChartState.isReverseLayout
        ) {
            for (i in 0..poopChartState.totalDays) {
                item(
                    contentType = "MONTH_HEADER",
                    key = i
                ) {

                    val month = remember { poopChartState.monthEndDates.find { it.first == i } }

                    month?.let { (_, month) ->

                        val logsOnMonth by remember(poopChartState.poopLogs){
                            derivedStateOf {
                                poopChartState.poopLogs.count { it.time.month == month }
                            }
                        }

                        val builder = remember(logsOnMonth) {
                            Balloon.Builder(context).apply {
                                setText("$logsOnMonth poops on " + month.name.lowercase().replaceFirstChar { it.uppercase() })
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
                                setTextColor(onPrimary) // set text color with compose color.
                                setBackgroundColor(primaryColor) // set background color with compose color.
                            }
                        }
                        val selected = month in poopChartState.selectedMonths

                        Balloon(builder = builder, key = builder) { balloonWindow ->
                            Text(
                                text = month.name.take(3),
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) primaryColor else Color.Unspecified,
                                modifier = Modifier.combinedClickable(
                                    onClick = { onMonthClick(month) },
                                    onLongClick = { balloonWindow.showAlignTop() }
                                ),
                                textDecoration = if (selected) TextDecoration.Underline else TextDecoration.None
                            )
                        }
                    } ?: Text("")
                }
                for (j in 1..7) {

                    val days = (i * 7) + j
                    val current = poopChartState.current(days)

                    item(
                        key = current,
                        contentType = "DAY"
                    ) {
                        if (days <= poopChartState.totalDays) {
                            val logsOnDay by remember(poopChartState.poopLogs){
                                derivedStateOf {
                                    poopChartState.poopLogs.count {
                                        it.time.month == current.month &&
                                        it.time.dayOfMonth == current.dayOfMonth &&
                                        it.time.year == current.year
                                    }
                                }
                            }
                            val isStart = current == poopChartState.startDate.toLocalDate()

                            if (logsOnDay == 0) {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxSize()
                                        .padding(2.dp)
                                        .clip(
                                            RoundedCornerShape(8.dp)
                                        )
                                        .conditional(current in poopChartState.selectedDates) {
                                            border(2.dp, primaryColor, RoundedCornerShape(8.dp))
                                        }
                                        .background(MaterialTheme.colorScheme.onBackground)
                                        .clickable {
                                            onDateClick(current)
                                        }
                                ) {
                                    Text(
                                        text = current.dayOfMonth.toString(),
                                        modifier = Modifier.align(Alignment.Center),
                                        color = if (isStart) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.background
                                        },
                                        fontWeight = if (isStart) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            } else {
                                val builder = remember(logsOnDay) {
                                        Balloon.Builder(context).apply {
                                            setText(
                                                "$logsOnDay poops on ${
                                                    current.dayOfWeek.name.lowercase()
                                                        .replaceFirstChar { it.uppercase() }
                                                },"
                                                        + " ${
                                                    current.month.name.lowercase()
                                                        .replaceFirstChar { it.uppercase() }
                                                } ${current.dayOfMonth}, ${current.year}"
                                            )
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
                                            setTextColor(onPrimary) // set text color with compose color.
                                            setBackgroundColor(primaryColor) // set background color with compose color.
                                    }
                                }

                                Balloon(
                                    builder = builder,
                                    key = builder
                                ) { balloonWindow ->
                                    val freqBgColor = when (logsOnDay) {
                                        1 -> Color(0xffbbf7d0)
                                        2 -> Color(0xff86efac)
                                        3 -> Color(0xff4ade80)
                                        4 -> Color(0xff22c55e)
                                        else -> Color(0xff16a34a)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clip(
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(freqBgColor)
                                            .combinedClickable(
                                                onClick = { onDateClick(current) },
                                                onLongClick = { balloonWindow.showAlignTop() }
                                            )
                                            .conditional(current in poopChartState.selectedDates) {
                                                border(2.dp, primaryColor, RoundedCornerShape(8.dp))
                                            }
                                    ) {
                                        Text(
                                            text = current.dayOfMonth.toString(),
                                            modifier = Modifier.align(Alignment.Center),
                                            color = if (isStart) {
                                                MaterialTheme.colorScheme.surface
                                            } else
                                                MaterialTheme.colorScheme.background,
                                            fontWeight = if (isStart) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!poopChartState.isReverseLayout) {
            DaysSideText(
                Modifier
                    .fillMaxHeight()
                    .width(width)
            )
        }
    }
}