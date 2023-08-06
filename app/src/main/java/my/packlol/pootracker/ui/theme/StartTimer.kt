package my.packlol.pootracker.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Timer(hour: Int, minute:Int, second:Int){
    var currTime = LocalDateTime.now(ZoneId.systemDefault())

    var newHour = currTime.hour - hour
    var newMin = currTime.minute - minute
    var newSec = currTime.second - second
    Text("$newHour:${newMin}:${newSec}")
}
