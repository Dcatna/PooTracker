package my.packlol.pootracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.packlol.pootracker.local.PoopLog
import my.packlol.pootracker.ui.MainVM
import my.packlol.pootracker.ui.theme.PooTrackerTheme
import org.koin.androidx.viewmodel.ext.android.getViewModel


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = getViewModel<MainVM>()
            val lists by viewModel.juicerList.collectAsState()
            val timeSinceLastPoop by viewModel.timeSinceLastPoop.collectAsState()

            PooTrackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Scaffold(
                        topBar = {
                            Greeting(
                                modifier = Modifier.fillMaxWidth(),
                                time = timeSinceLastPoop
                            )
                        },
                        bottomBar =  {
                            Button(onClick = {
                                viewModel.insert(
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
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            PoopLogLazyList(
                                logs = lists,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
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
    time: Time
) {
    Column(
        modifier = modifier.padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (time.never) {
            Text(fontSize = 24.sp, text = "Log a poop to start")
        } else {
            Text(fontSize = 24.sp, text = "Time Since Last Poop")
            Text(text = "${time.hour}:${time.minute}:${time.second}")
            Column {
                Text(text = "Recent Logs")
            }
        }
    }
}


data class Time(
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
    val never: Boolean = false
)


