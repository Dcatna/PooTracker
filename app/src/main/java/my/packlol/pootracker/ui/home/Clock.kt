package my.packlol.pootracker.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonHighlightAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.rememberBalloonBuilder
import com.skydoves.balloon.compose.setBackgroundColor
import com.skydoves.balloon.compose.setTextColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Composable
fun rememberClockState(
    scope: CoroutineScope = rememberCoroutineScope()
) =  rememberSaveable(
    saver = Saver(
        save = {
            listOf(it.time.toEpochSecond(ZoneOffset.UTC), if(it.playing) 1L else 0L, )
        },
        restore = {
            ClockState(
                scope = scope,
                startTime = LocalDateTime.ofEpochSecond(it[0], 0, ZoneOffset.UTC),
                playing = it[1] == 1L
            )
        }
    )
) {
    ClockState(scope)
}

class ClockState(
    private val scope: CoroutineScope,
    playing: Boolean = true,
    startTime: LocalDateTime = LocalDateTime.now()
) {

    var playing by mutableStateOf(playing)
        private set

    var time by mutableStateOf(startTime)
        private set

    val hour by derivedStateOf {
        if(time.hour > 12) {
            (time.hour - 12).toString()
        } else if (time.hour == 0) {
            "12"
        } else {
            time.hour.toString()
        }
    }

    val minute by derivedStateOf {
        if (time.minute < 10) {
            "0${time.minute}"
        } else time.minute.toString()
    }

    val second by derivedStateOf {
        if (time.second < 10) {
            "0${time.second}"
        } else time.second.toString()
    }

    private var timerJob: Job? = null

    init {
        if (playing) { resume() }
    }

    fun resume() {
        playing = true
        if (timerJob?.isActive == true) { return }
        timerJob = scope.launch {
            while (true) {
                time = LocalDateTime.now()
                delay(50)
            }
        }
    }

    fun pauseTimeAt(time: LocalDateTime) {
        playing = false
        timerJob?.cancel()
        this.time = time
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateWithChangeableTime(
    state: ClockState = rememberClockState(),
    onTimeIconClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = remember(state.time.toLocalDate()) {
                    formatDate(state.time.toLocalDate())
                },
                style = MaterialTheme.typography.titleMedium
            )
            Clock(state)
        }
        ClockControls(
            resume = { state.resume() },
            changeTime = onTimeIconClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockControls(
    resume: () -> Unit,
    changeTime: () -> Unit,
) {

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Balloon(
            builder = rememberBalloonBuilder {
                setText("set timer to current time")
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
                setBackgroundColor(primaryColor)
            }
        ) { balloonWindow ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = "resume",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = resume,
                                onLongClick = {
                                    balloonWindow.showAlignTop()
                                }
                            )
                    )
                    Text(
                        text = "resume",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Balloon(
            builder = rememberBalloonBuilder {
                setText("change log time")
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
                setBackgroundColor(primaryColor)
            }
        ) { balloonWindow ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = changeTime,
                                onLongClick = {
                                    balloonWindow.showAlignTop()
                                }
                            )
                    )
                    Text(
                        text = "change time",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun Clock(
    state: ClockState
) {
    Row(
        Modifier
            .fillMaxWidth(0.5f)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedNumber(
            number = state.hour,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
        )
        Text(
            text = ":",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        AnimatedNumber(
            number = state.minute,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        Text(
            text = ":",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        AnimatedNumber(
            number = state.second,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        Text(
            text = if (state.time.hour >= 12) { "pm" } else "am"
        )
    }
}

@Composable
private fun AnimatedNumber(
    modifier: Modifier = Modifier,
    number: String
) {
    Box(
        modifier = modifier
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
     AnimatedContent(
         targetState = number,
         label = "animated-number",
         transitionSpec = {
             slideInVertically { -it } togetherWith slideOutVertically { it }
         }
    ) { num ->
            Text(
                text = num,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun formatDate(d: LocalDate): String {
    return "${d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}," +
            " ${d.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}, ${d.year}"
}