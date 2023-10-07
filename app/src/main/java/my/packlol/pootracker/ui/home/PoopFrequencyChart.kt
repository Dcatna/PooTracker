
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import my.packlol.pootracker.ui.home.PoopChartItem
import my.packlol.pootracker.ui.home.PoopChartVM
import my.packlol.pootracker.ui.theme.conditional
import my.packlol.pootracker.ui.theme.isScrollingUp
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
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

@Composable
fun PoopChart(
    poopChartVM: PoopChartVM = koinViewModel(),
    modifier: Modifier,
) {
    Row(
        modifier,
    ) {
        val gridState =
            rememberLazyGridState(initialFirstVisibleItemIndex = poopChartVM.totalDays)

        val isScrollingUp by gridState.isScrollingUp()

        val hideSideText by remember {
            derivedStateOf {
                isScrollingUp && gridState.isScrollInProgress && poopChartVM.totalDays - gridState.firstVisibleItemIndex > 80
            }
        }

        val poopChartItems by poopChartVM.poopChartItems.collectAsState()

        val width by animateDpAsState(
            label = "days-anim",
            targetValue = if(hideSideText) { 0.dp } else { 40.dp },
            animationSpec = tween(200, delayMillis = 50)
        )

        if (poopChartVM.reverseLayout) {
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
            reverseLayout = poopChartVM.reverseLayout
        ) {
            items(poopChartItems) {
                when (it) {
                    PoopChartItem.Blank -> Text("")
                    is PoopChartItem.DateNoLogs -> DateNoLogsBox(
                        selected = it.selected,
                        onClick = { },
                        dayOfMonth = it.dayOfMonth,
                        start = it.start
                    )
                    is PoopChartItem.DateWithLogs -> DateWithLogsBox(
                        logsOnDay = it.amount,
                        current = it.day,
                        selected = it.selected,
                        start = it.start,
                        onClick = {}
                    )
                    is PoopChartItem.MonthTag -> MonthTag(
                        logsOnMonth = it.amount,
                        month = it.month,
                        selected = it.selected,
                        onMonthClick = {}
                    )
                }
            }
        }
        if (!poopChartVM.reverseLayout) {
            DaysSideText(
                Modifier
                    .fillMaxHeight()
                    .width(width)
            )
        }
    }
}

@Composable
fun DateNoLogsBox(
    selected: Boolean,
    onClick: () -> Unit,
    dayOfMonth: String,
    start: Boolean,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .padding(2.dp)
            .clip(
                RoundedCornerShape(12)
            )
            .conditional(selected) {
                border(2.dp, primaryColor, RoundedCornerShape(12))
            }
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable {
                onClick()
            }
    ) {
        Text(
            text = dayOfMonth,
            modifier = Modifier.align(Alignment.Center),
            color = if (start) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.background
            },
            fontWeight = if (start) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateWithLogsBox(
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onPrimary: Color = MaterialTheme.colorScheme.onPrimary,
    logsOnDay: Int,
    current: LocalDateTime,
    selected: Boolean,
    start: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
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
                    RoundedCornerShape(12)
                )
                .background(freqBgColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { balloonWindow.showAlignTop() }
                )
                .conditional(selected) {
                    border(2.dp, primaryColor, RoundedCornerShape(12))
                }
        ) {
            Text(
                text = current.dayOfMonth.toString(),
                modifier = Modifier.align(Alignment.Center),
                color = if (start) {
                    MaterialTheme.colorScheme.surface
                } else
                    MaterialTheme.colorScheme.background,
                fontWeight = if (start) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthTag(
    logsOnMonth: Int,
    month: Month,
    selected: Boolean,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onPrimary: Color = MaterialTheme.colorScheme.onPrimary,
    onMonthClick: (Month) -> Unit
) {
    val context = LocalContext.current
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
}
