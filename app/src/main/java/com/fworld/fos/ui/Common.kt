package com.fworld.fos.ui

import android.content.Intent
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
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

/** Часы и минуты отдельно (для крупных часов в две строки) */
fun LocalDateTime.timeParts(use24h: Boolean): Pair<String, String> =
    format(DateTimeFormatter.ofPattern(if (use24h) "HH" else "h")) to format(DateTimeFormatter.ofPattern("mm"))

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

/** Иконка приложения: у каждого приложения своя фигура M3 Expressive и градиент */
@Composable
fun AppIcon(app: AppId, size: Dp = 60.dp, modifier: Modifier = Modifier) {
    val spec = when (app) {
        AppId.STORE -> Triple(R.drawable.ic_store, ShapeKind.Clover4, listOf(Color(0xFF536DFE), Color(0xFF00B0FF)))
        AppId.GRAM -> Triple(R.drawable.ic_send, ShapeKind.Cookie9, listOf(Color(0xFF29B6F6), Color(0xFF1976D2)))
        AppId.TOK -> Triple(R.drawable.ic_music, ShapeKind.SoftBurst, listOf(Color(0xFFFF4081), Color(0xFF7B1FA2)))
        AppId.SETTINGS -> Triple(R.drawable.ic_settings, ShapeKind.Cookie6, listOf(Color(0xFF78909C), Color(0xFF455A64)))
    }
    GradientShape(spec.second, spec.third, size, modifier) {
        FosIcon(spec.first, Color.White, size * 0.46f)
    }
}

fun appLabel(app: AppId, ru: Boolean): String = when (app) {
    AppId.STORE -> "fStore"
    AppId.GRAM -> "fGram"
    AppId.TOK -> "fTok"
    AppId.SETTINGS -> if (ru) "Настройки" else "Settings"
}

/** Круглая кнопка-иконка */
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
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(w - 3.dp.toPx() - stroke, h - stroke),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
        val innerW = (w - 3.dp.toPx() - stroke * 3) * (percent.coerceIn(0, 100) / 100f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(stroke * 1.5f, stroke * 1.5f),
            size = Size(innerW, h - stroke * 3),
            cornerRadius = CornerRadius(1.5.dp.toPx()),
        )
        drawRoundRect(
            color = tint.copy(alpha = 0.9f),
            topLeft = Offset(w - 2.5.dp.toPx(), h * 0.3f),
            size = Size(2.dp.toPx(), h * 0.4f),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
    }
}
