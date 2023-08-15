package my.packlol.pootracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.packlol.pootracker.local.PoopLog

data class Time(
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
    val never: Boolean = false
)

@Composable
fun PoopScreen(
    logs: List<PoopLog>,
    timeSinceLastPoop: Time,
    onInsert: (Int, Int, Int) -> Unit,
    navigate: (route: String) -> Unit
) {
    Scaffold(
        topBar = {
            Greeting(
                modifier = Modifier.fillMaxWidth(),
                time = timeSinceLastPoop,
                settingsIconClick = {
                    navigate("settings")
                }
            )
        },
        bottomBar =  {
            Button(
                onClick = {
                    onInsert(
                        timeSinceLastPoop.hour,
                        timeSinceLastPoop.minute,
                        timeSinceLastPoop.second
                    )
                }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "New Log")
            }
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PoopLogLazyList(
                logs = logs,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}


@Composable
fun PoopLogLazyList(
    modifier: Modifier = Modifier,
    logs: List<PoopLog>,
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        items(
            items = logs,
            key = { it.id }
        ){ poopLog ->
            Text(poopLog.toString(), modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
        }
    }

}


@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    time: Time,
    settingsIconClick: () -> Unit
) {
    Box(
        modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (time.never) {
                Text(fontSize = 24.sp, text = "Log a poop to start")
            } else {
                Text(fontSize = 24.sp, text = "Time Since Last Poop")
                Text(text = "${time.hour}:${time.minute}:${time.second}")
                Text(text = "Recent Logs")
            }
        }
        IconButton(
            onClick = settingsIconClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "settings",
            )
        }
    }
}


