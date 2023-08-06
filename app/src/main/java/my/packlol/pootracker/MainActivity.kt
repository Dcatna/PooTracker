package my.packlol.pootracker

import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.packlol.pootracker.ui.theme.CanvasClock
import my.packlol.pootracker.ui.theme.DB

import my.packlol.pootracker.ui.theme.PooTrackerTheme
import my.packlol.pootracker.ui.theme.PoopLog
import my.packlol.pootracker.ui.theme.Timer
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

val starttime = System.currentTimeMillis()
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = viewModel<Collector>()
            val lists by viewModel.juicerlist.collectAsState()

            PooTrackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    color = MaterialTheme.colorScheme.background

                ) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Greeting(insert = {hour, minute, second->viewModel.insert(hour, minute, second)})

                        scrollingLog(lists = lists)
                    }

                }
            }
        }
    }
}

@Composable
fun scrollingLog(lists: List<PoopLog>) {
    LazyColumn(modifier = Modifier.height(1000.dp)){
        items(lists){
            item ->  Text(item.toString(), modifier = Modifier.padding(top = 12.dp))
        }
    }

}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Greeting(modifier: Modifier = Modifier, insert:(Int, Int, Int)->Unit) {
    var poopList by remember {
        mutableStateOf(emptyList<PoopLog>())
    }
    //var newTime by remember { mutableStateOf(LocalDateTime.now()) }
    //var id by remember { mutableStateOf(0) }
    var hour by remember { mutableStateOf(0)}
    var minute by remember { mutableStateOf(0)}
    var second by remember { mutableStateOf(0)}

    LaunchedEffect(key1 = true) {
        while(true){
            delay(1000)
            second+=1
            if(second >= 60){
                second = 0
                minute+=1
                if(minute>=60){
                    minute = 0
                    hour +=1
                }
            }
            //newTime = LocalDateTime.now(ZoneId.systemDefault())
        }
    }

    Column(Modifier.padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(fontSize = 24.sp, text = "Time Since Last Poop")

        Text(text = "${hour}:${minute}:${second}")



        Column() {
            Text(text = "Recent Logs")

        }

        }

    Popup(alignment = Alignment.BottomCenter) {
        Button(onClick = {
            insert(hour, minute, second)
            hour = 0
            minute = 0
            second = 0
        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Reset")
        }
    }

}

@Composable
fun stickyButton() {
    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier
            .verticalScroll(rememberScrollState())
            .weight(1f)) {
            Button(onClick = { /*TODO*/ }, modifier = Modifier
                .padding(vertical = 2.dp)
                .fillMaxWidth()) {
                Text(text = "Button")
            }
        }
    }
}


class Collector : ViewModel(){
    private val dao = DB.getDao()

    val juicerlist = dao.getAll().stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList())
    fun insert(hour:Int, minute:Int, second:Int){
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.insertAll(PoopLog(hour, minute, second))
            }
        }
    }
    init {
        viewModelScope.launch{
            withContext(Dispatchers.IO){
                dao.deleteAll()
            }
        }
    }

}
