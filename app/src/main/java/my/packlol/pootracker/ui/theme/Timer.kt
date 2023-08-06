package my.packlol.pootracker.ui.theme
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withRotation
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class ClockStyle (
    val hourHandLength: Dp = 58.dp,
    val minHandLength: Dp = 80.dp,
    val secondHandLength: Dp = 80.dp,
    val clockRadius: Dp = 100.dp,
    val hourHandColor: Color = Color.Black,
    val secondHandColor: Color = Color.Red,
    val minHandColor: Color = Color.Black,
    val thirtyMinLineLength: Dp = 25.dp,
    val fiveMinLineLength: Dp = 15.dp,
    val fiveMinLineColor: Color = Color.Black,
    val thirtyMinLineColor: Color = Color.Black
)

fun Float.toRadians() = this * (PI / 180f).toFloat()
fun Float.toDegrees() = this * (108f / PI).toFloat()

sealed class LineType {
    object FiveMin: LineType()
    object ThirtyMin: LineType()
    object None: LineType()
}

@Composable
fun CanvasClock(
    modifier: Modifier,
    clockStyle: ClockStyle = ClockStyle(),
    hours: Int,
    minutes: Int,
    seconds: Int
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    var angle by remember { mutableStateOf(0f) }
    val radius = clockStyle.clockRadius
    Canvas(modifier = modifier.border(12.dp, Color.Black)) {
        center = Offset(this.center.x, this.center.y)

        rotate(270f) {
            drawContext.canvas.nativeCanvas.apply {
                drawCircle(
                    center.x,
                    center.y,
                    radius.toPx(),
                    Paint().apply {
                        0
                        color = android.graphics.Color.WHITE
                        setStyle(Paint.Style.FILL)
                        setShadowLayer(
                            60f,
                            0f,
                            0f,
                            android.graphics.Color.argb(50, 0, 0, 0)
                        )
                    }
                )
            }
            //draw line
            //Formula for radians
            //x = radius * cos(angle in radians)
            //y = radius * sin(angle in radians)
            for (i in 1..60) {
                val lineType = when (i % 5 == 0) {
                    true -> LineType.ThirtyMin
                    else -> LineType.FiveMin
                }
                val lineLength = when (lineType) {
                    is LineType.ThirtyMin -> clockStyle.thirtyMinLineLength.toPx()
                    else -> clockStyle.fiveMinLineLength.toPx()
                }
                val lineWidth = when (lineType) {
                    is LineType.ThirtyMin -> 1.5.dp
                    else -> 0.5.dp
                }
                val lineColor = Color.Black
                val lineStart = Offset(
                    x = radius.toPx() * cos((i * 6).toFloat().toRadians()) + center.x,
                    y = radius.toPx() * sin((i * 6).toFloat().toRadians()) + center.y
                )
                val lineEnd = Offset(
                    x = (radius.toPx() - lineLength) * cos(
                        (i * 6).toFloat().toRadians()
                    ) + center.x,
                    y = (radius.toPx() - lineLength) * sin(
                        (i * 6).toFloat().toRadians()
                    ) + center.y,
                )
                drawLine(
                    lineColor,
                    lineStart,
                    lineEnd,
                    lineWidth.toPx()
                )
            }

            //draw Hour Min Second hands
            //seconds
            //x = radius * cos(angle in radians)
            //y = radius * sin(angle in radians)
            drawLine(
                color = clockStyle.secondHandColor,
                start = Offset(
                    center.x,
                    center.y
                ),
                end = Offset(
                    x = clockStyle.secondHandLength.toPx() * cos((seconds * 6f).toRadians()) + center.x,
                    y = clockStyle.secondHandLength.toPx() * sin((seconds * 6f).toRadians()) + center.y
                ),
                strokeWidth = 2.dp.toPx()
            )
            //minutes
            drawLine(
                color = clockStyle.minHandColor,
                start = Offset(
                    center.x,
                    center.y
                ),
                end = Offset(
                    x = clockStyle.minHandLength.toPx() * cos((minutes * 6f).toRadians()) + center.x,
                    y = clockStyle.minHandLength.toPx() * sin((minutes * 6f).toRadians()) + center.y
                ),
                strokeWidth = 3.dp.toPx()
            )
            //hours
            drawLine(
                color = clockStyle.hourHandColor,
                start = Offset(
                    center.x,
                    center.y
                ),
                end = Offset(
                    x = clockStyle.hourHandLength.toPx() * cos((hours * 30f).toRadians()) + center.x,
                    y = clockStyle.hourHandLength.toPx() * sin((hours * 30f).toRadians()) + center.y
                ),
                strokeWidth = 3.5.dp.toPx()
            )
        }
    }
}