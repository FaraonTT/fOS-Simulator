package com.fworld.fos.ui

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fworld.fos.R
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Идентификаторы приложений fOS */
enum class AppId(val key: String) { STORE("fstore"), GRAM("fgram"), TOK("ftok"), SETTINGS("settings") }

/** Реальное время, обновляется раз в секунду */
@Composable
fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(1000)
    }
}

fun LocalDateTime.timeText(use24h: Boolean): String =
    format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm"))

fun LocalDateTime.dateText(ru: Boolean): String =
    format(DateTimeFormatter.ofPattern("EEEE, d MMMM", if (ru) Locale("ru") else Locale.ENGLISH))
        .replaceFirstChar { it.uppercase() }

/** Реальный заряд батареи устройства (0..100) и признак зарядки */
@Composable
fun rememberBattery(): State<Pair<Int, Boolean>> {
    val ctx = LocalContext.current
    return produceState(Pair(100, false)) {
        while (true) {
            val i: Intent? = ctx.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (i != null) {
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                value = Pair(if (scale > 0) level * 100 / scale else level, charging)
            }
            delay(15_000)
        }
    }
}

@Composable
fun FosIcon(res: Int, tint: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = modifier.size(size))
}

/** Иконка приложения: цветной «сквиркл» с SVG-иконкой */
@Composable
fun AppIcon(app: AppId, size: Dp = 60.dp, modifier: Modifier = Modifier) {
    val (res, colors) = when (app) {
        AppId.STORE -> R.drawable.ic_store to listOf(Color(0xFF3D5AFE), Color(0xFF00B0FF))
        AppId.GRAM -> R.drawable.ic_send to listOf(Color(0xFF229ED9), Color(0xFF1B6FD0))
        AppId.TOK -> R.drawable.ic_music to listOf(Color(0xFFFF0050), Color(0xFF7B1FA2))
        AppId.SETTINGS -> R.drawable.ic_settings to listOf(Color(0xFF607D8B), Color(0xFF37474F))
    }
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        FosIcon(res, Color.White, size * 0.52f)
    }
}

fun appLabel(app: AppId, ru: Boolean): String = when (app) {
    AppId.STORE -> "fStore"
    AppId.GRAM -> "fGram"
    AppId.TOK -> "fTok"
    AppId.SETTINGS -> if (ru) "Настройки" else "Settings"
}

/** Круглая кнопка-иконка без ripple-зависимостей */
@Composable
fun RoundIconButton(
    res: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = CircleShape,
) {
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        FosIcon(res, tint, size * 0.5f)
    }
}

@Composable
fun BatteryGlyph(percent: Int, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 22.dp, height = 11.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        drawRoundRect(
            color = tint.copy(alpha = 0.9f),
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
            size = androidx.compose.ui.geometry.Size(w - 3.dp.toPx() - stroke, h - stroke),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        val innerW = (w - 3.dp.toPx() - stroke * 3) * (percent.coerceIn(0, 100) / 100f)
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(stroke * 1.5f, stroke * 1.5f),
            size = androidx.compose.ui.geometry.Size(innerW, h - stroke * 3),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
        )
        drawRoundRect(
            color = tint.copy(alpha = 0.9f),
            topLeft = androidx.compose.ui.geometry.Offset(w - 2.5.dp.toPx(), h * 0.3f),
            size = androidx.compose.ui.geometry.Size(2.dp.toPx(), h * 0.4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
        )
    }
}

@Suppress("unused")
private fun unusedContext(c: Context) = c
