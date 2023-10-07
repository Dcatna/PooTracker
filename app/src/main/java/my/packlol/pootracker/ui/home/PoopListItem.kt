package my.packlol.pootracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

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