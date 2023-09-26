package my.packlol.pootracker.ui.home

import PoopChart
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.calendar.CalendarView
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import my.packlol.pootracker.ui.navigation.Screen
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    navigate: (Screen) -> Unit = {},
    state: HomeUiState,
    onLogButtonClick: () -> Unit,
) {
    var selectedDate by rememberSaveable {
        mutableStateOf(LocalDate.now())
    }

    var logs by remember {
        mutableStateOf(emptyList<UiPoopLog>())
    }

    LaunchedEffect(key1 = selectedDate) {
        logs = emptyList()
        logs = state.logsByUser.flatMap { it.value }
            .filter { it.time.toLocalDate() == selectedDate }
    }

    Column(Modifier.fillMaxSize()) {
        PoopChart(
            monthsPrev = 12,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f),
            dateBox = { date ->

                val backGround by remember(date, state.logsByUser) {
                    derivedStateOf {
                        state.logsByUser.flatMap { it.value }.count {
                            it.time.month == date.month && it.time.dayOfMonth == date.dayOfMonth
                        }.let {
                            when(it) {
                                0 -> Color.LightGray
                                1 -> Color.Green.copy(alpha = 0.2f)
                                2 -> Color.Green.copy(alpha = 0.4f)
                                3 -> Color.Green.copy(alpha = 0.6f)
                                4 -> Color.Green.copy(alpha = 0.8f)
                                else -> Color.Green.copy(alpha = 1f)
                            }
                        }
                    }
                }

                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { selectedDate = date.toLocalDate() }
                    .background(backGround)
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(logs) {
                Text(it.toString())
            }
        }
    }
}
