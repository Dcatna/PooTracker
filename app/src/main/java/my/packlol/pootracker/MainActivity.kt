package my.packlol.pootracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.packlol.pootracker.ui.theme.DB
import my.packlol.pootracker.ui.theme.PooTrackerTheme
import my.packlol.pootracker.ui.theme.PoopLog

val starttime = System.currentTimeMillis()

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val viewModel = viewModel<Collector>()
            val lists by viewModel.juicerList.collectAsState()

            var hour by remember { mutableStateOf(0) }
            var minute by remember { mutableStateOf(0) }
            var second by remember { mutableStateOf(0) }

            LaunchedEffect(key1 = true) {
                while (true) {
                    delay(1000)
                    second += 1
                    if (second >= 60) {
                        second = 0
                        minute += 1
                        if (minute >= 60) {
                            minute = 0
                            hour += 1
                        }
                    }
                }
            }


            PooTrackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Scaffold(
                        topBar = {
                            Greeting(
                                modifier = Modifier.fillMaxWidth(),
                                hour = hour,
                                minute = minute,
                                second = second,
                                insert = { hour, minute, second ->
                                    viewModel.insert(hour, minute, second)
                                }
                            )
                        },
                        bottomBar =  {
                            Button(onClick = {
                                viewModel.insert(hour, minute, second)
                                hour = 0
                                minute = 0
                                second = 0
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(text = "Reset")
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
            Text(poopLog.toString(), modifier = Modifier.padding(top = 12.dp))
        }
    }

}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    hour: Int,
    minute: Int,
    second: Int,
    insert:(Int, Int, Int)-> Unit
) {
    Column(
        modifier = modifier.padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(fontSize = 24.sp, text = "Time Since Last Poop")
        Text(text = "${hour}:${minute}:${second}")
        Column {
            Text(text = "Recent Logs")
        }
        Button(onClick = { insert(hour, minute, second)}) {
            Text(text = "update")
        }
    }
}




class Collector : ViewModel() {

    private val dao = DB.getDao()

    val juicerList = dao.observeAll()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )



    fun insert(hour:Int, minute:Int, second:Int){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.insertAll(PoopLog(hour, minute, second))
            }
        }
    }
}
