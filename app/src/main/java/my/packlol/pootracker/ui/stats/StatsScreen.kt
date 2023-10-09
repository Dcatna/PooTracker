package my.packlol.pootracker.ui.stats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.component.Component
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.text.TextComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import my.packlol.pootracker.PoopAppState
import my.packlol.pootracker.ui.home.CollectionsLazyRow
import my.packlol.pootracker.ui.stats.ChartType.*
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.Month
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    poopAppState: PoopAppState
) {
    val statsVM = koinViewModel<StatsVM>()

    val dayOfWeekEntries by statsVM.avgPoopsByDayOfWeekEntries.collectAsStateWithLifecycle()
    val monthEntries by statsVM.avgPoopsByMonth.collectAsStateWithLifecycle()
    val collections by statsVM.collections.collectAsStateWithLifecycle()
    val selectedCollections by statsVM.selectedCollections.collectAsStateWithLifecycle()

    LazyColumn {

        stickyHeader {
            Column {
                Text(
                    text = "filter by collections.",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                CollectionsLazyRow(
                    collections = collections,
                    selected = selectedCollections,
                    onCollectionClick = {
                        statsVM.toggleCollectionFilter(it)
                    }
                )   
            }
        }
        item {
            ProvideChartStyle(
                m3ChartStyle()
            ) {
                var dayOfWeekChartType by rememberSaveable {
                    mutableStateOf(Col)
                }

                AvgPoopsByDayOfWeekChart(
                    entries = dayOfWeekEntries,
                    chartType = dayOfWeekChartType,
                    modifier = Modifier.clickable {
                        dayOfWeekChartType = when(dayOfWeekChartType) {
                            Line -> Col
                            Col -> Line
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                var monthChartType by rememberSaveable {
                    mutableStateOf(Col)
                }

                AvgPoopsByMonthChart(
                    entries = monthEntries,
                    chartType = monthChartType,
                    modifier = Modifier.clickable {
                        monthChartType = when(monthChartType) {
                            Line -> Col
                            Col -> Line
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun rememberMarker(
    indicator: Component? = null,
    guideline: LineComponent? = null,
    labelBuilder: TextComponent.Builder.() -> Unit = {}
) = remember {
    MarkerComponent(
        TextComponent.Builder()
            .apply(labelBuilder)
            .build(),
        indicator,
        guideline
    )
}

enum class ChartType {
    Line, Col
}

@Composable
fun AvgPoopsByMonthChart(
    modifier: Modifier = Modifier,
    chartType: ChartType = Col,
    entries: ChartEntryModelProducer,
) {
    Column(modifier) {

        Text(
            text = "Avg. poops logged per month.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                bottom = 4.dp,
                start = 12.dp
            )
        )
        Chart(
            chart = when(chartType) {
                Line -> lineChart()
                Col -> columnChart()
            },
            chartModelProducer = entries,
            startAxis = rememberStartAxis(),
            marker = rememberMarker(),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ ->
                    Month.of(value.roundToInt()).toString().take(3)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AvgPoopsByDayOfWeekChart(
    modifier: Modifier = Modifier,
    chartType: ChartType = Col,
    entries: ChartEntryModelProducer
) {
    Column(modifier) {

        Text(
            text = "Avg. poops logged per day of week.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(
                bottom = 4.dp,
                start = 12.dp
            )
        )
        Chart(
            chart = when(chartType) {
                Line -> lineChart()
                Col -> columnChart()
            },
            chartModelProducer = entries,
            startAxis = rememberStartAxis(),
            marker = rememberMarker(),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ ->
                    DayOfWeek.of(value.roundToInt()).toString().take(3)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

