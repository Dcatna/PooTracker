package my.packlol.pootracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.packlol.pootracker.ui.shared.PullRefreshIndicator
import my.packlol.pootracker.ui.shared.pullRefresh
import my.packlol.pootracker.ui.shared.rememberPullRefreshState
import java.time.LocalDateTime
import java.time.ZoneOffset

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


@Composable
fun PullRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    indicatorOffset: Dp = 0.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
    content: @Composable () -> Unit,
) {
    val state = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = onRefresh,
        refreshingOffset = indicatorOffset,
    )


    Box(Modifier.pullRefresh(state, !refreshing)) {
        content()

        Box(
            Modifier
                .matchParentSize()
                .clipToBounds(),
        ) {
            PullRefreshIndicator(
                refreshing = refreshing,
                state = state,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = backgroundColor,
                contentColor = contentColor,
            )
        }
    }
}
