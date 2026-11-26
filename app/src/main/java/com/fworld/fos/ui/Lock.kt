package com.fworld.fos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fworld.fos.LockType
import com.fworld.fos.LocalFos
import com.fworld.fos.Phase
import com.fworld.fos.R
import com.fworld.fos.lang
import com.fworld.fos.wallpaperBrush
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Цифровая клавиатура для PIN-кода */
@Composable
fun PinPad(
    value: String,
    onChange: (String) -> Unit,
    maxLen: Int = 8,
    keyColor: Color = Color.White.copy(alpha = 0.14f),
    textColor: Color = Color.White,
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "<")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { k ->
                    if (k.isEmpty()) {
                        Spacer(Modifier.size(70.dp))
                    } else {
                        Box(
                            Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(keyColor)
                                .clickable {
                                    if (k == "<") {
                                        if (value.isNotEmpty()) onChange(value.dropLast(1))
                                    } else if (value.length < maxLen) {
                                        onChange(value + k)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (k == "<") FosIcon(R.drawable.ic_backspace, textColor, 26.dp)
                            else Text(k, color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinDots(filled: Int, total: Int, color: Color = Color.White) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (i < filled) color else color.copy(alpha = 0.25f))
            )
        }
    }
}

/** Графический ключ 3x3 */
@Composable
fun PatternPad(
    onComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    color: Color = Color.White,
) {
    var path by remember { mutableStateOf(listOf<Int>()) }
    var cur by remember { mutableStateOf<Offset?>(null) }
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    val shownColor = if (error) MaterialTheme.colorScheme.error else color

    fun dot(i: Int): Offset {
        val cell = sizePx.width / 3f
        return Offset(cell * (i % 3) + cell / 2, cell * (i / 3) + cell / 2)
    }

    fun hit(o: Offset) {
        val cell = sizePx.width / 3f
        for (i in 0..8) {
            if (i !in path && (dot(i) - o).getDistance() < cell * 0.3f) {
                path = path + i
            }
        }
    }

    Canvas(
        modifier
            .size(280.dp)
            .onSizeChanged { sizePx = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { o ->
                        path = emptyList()
                        cur = o
                        hit(o)
                    },
                    onDrag = { change, _ ->
                        cur = change.position
                        hit(change.position)
                    },
                    onDragEnd = {
                        cur = null
                        if (path.isNotEmpty()) onComplete(path)
                    },
                    onDragCancel = { cur = null },
                )
            }
    ) {
        val strokeW = 5.dp.toPx()
        for (k in 0 until path.size - 1) {
            drawLine(shownColor, dot(path[k]), dot(path[k + 1]), strokeW, StrokeCap.Round)
        }
        val c = cur
        if (c != null && path.isNotEmpty()) {
            drawLine(shownColor.copy(alpha = 0.6f), dot(path.last()), c, strokeW, StrokeCap.Round)
        }
        for (i in 0..8) {
            val sel = i in path
            drawCircle(
                color = if (sel) shownColor else color.copy(alpha = 0.55f),
                radius = if (sel) 11.dp.toPx() else 7.dp.toPx(),
                center = dot(i),
            )
            if (sel) {
                drawCircle(
                    color = shownColor.copy(alpha = 0.35f),
                    radius = 24.dp.toPx(),
                    center = dot(i),
                    style = Stroke(2.dp.toPx()),
                )
            }
        }
    }
}

/** Экран блокировки: часы, свайп вверх → ввод PIN / пароля / графического ключа */
@Composable
fun LockScreen() {
    val fos = LocalFos.current
    val l = lang()
    val now by rememberNow()
    var drag by remember { mutableFloatStateOf(0f) }
    var authing by remember { mutableStateOf(false) }

    BackHandler(enabled = authing) { authing = false; drag = 0f }

    Box(
        Modifier
            .fillMaxSize()
            .background(wallpaperBrush(fos.wallpaper))
            .pointerInput(authing) {
                if (!authing) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (drag < -220f) authing = true else drag = 0f
                        },
                        onDragCancel = { drag = 0f },
                    ) { _, dy -> drag = (drag + dy).coerceAtMost(0f) }
                }
            }
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 90.dp)
                .graphicsLayer {
                    translationY = drag * 0.4f
                    alpha = if (authing) 0f else (1f + drag / 600f).coerceIn(0f, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FosIcon(R.drawable.ic_lock, Color.White.copy(alpha = 0.85f), 22.dp)
            val (hh, mm) = now.timeParts(fos.use24h)
            Text(hh, color = Color.White, fontSize = 112.sp, fontWeight = FontWeight.Bold, lineHeight = 100.sp, modifier = Modifier.padding(top = 6.dp))
            Text(mm, color = Color.White.copy(alpha = 0.85f), fontSize = 112.sp, fontWeight = FontWeight.Bold, lineHeight = 100.sp)
            Text(now.dateText(fos.langRu), color = Color.White.copy(alpha = 0.9f), fontSize = 17.sp)
            if (fos.notifs.isNotEmpty()) {
                Text(
                    l("Уведомлений: ${fos.notifs.size}", "Notifications: ${fos.notifs.size}"),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }

        if (!authing) {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 54.dp)
                    .graphicsLayer { translationY = drag * 0.2f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    l("Проведите вверх для разблокировки", "Swipe up to unlock"),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                )
                Box(
                    Modifier
                        .padding(top = 14.dp)
                        .size(width = 120.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f))
                )
            }
        }

        AnimatedVisibility(
            visible = authing,
            enter = slideInVertically(tween(300)) { it / 3 } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(200)) { it / 3 } + fadeOut(tween(200)),
        ) {
            AuthPanel(
                onSuccess = { fos.phase = Phase.DESKTOP },
            )
        }
    }
}

@Composable
private fun AuthPanel(onSuccess: () -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    var pin by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var errorTick by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }

    fun fail() {
        error = true
        errorTick++
    }

    LaunchedEffect(errorTick) {
        if (errorTick > 0) {
            repeat(3) {
                shake.animateTo(18f, tween(45))
                shake.animateTo(-18f, tween(45))
            }
            shake.animateTo(0f, tween(45))
            delay(500)
            error = false
            pin = ""
            pass = ""
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 80.dp, bottom = 40.dp)
                .offset { IntOffset(shake.value.roundToInt(), 0) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShapeBadge(ShapeKind.Cookie9, Color.White.copy(alpha = 0.18f), 72.dp) {
                FosIcon(R.drawable.ic_person, Color.White, 36.dp)
            }
            if (fos.userName.isNotBlank()) {
                Text(fos.userName, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 10.dp))
            }
            val hint = when (fos.lockType) {
                LockType.PIN -> l("Введите PIN-код", "Enter PIN")
                LockType.PASSWORD -> l("Введите пароль", "Enter password")
                LockType.PATTERN -> l("Нарисуйте графический ключ", "Draw your pattern")
                LockType.NONE -> ""
            }
            Text(
                if (error) l("Неверно, попробуйте снова", "Wrong, try again") else hint,
                color = if (error) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 14.dp, bottom = 22.dp),
            )

            when (fos.lockType) {
                LockType.PIN -> {
                    PinDots(pin.length, fos.pinLen, if (error) MaterialTheme.colorScheme.error else Color.White)
                    Spacer(Modifier.height(34.dp))
                    PinPad(
                        value = pin,
                        maxLen = fos.pinLen,
                        onChange = {
                            if (error) return@PinPad
                            pin = it
                            if (it.length == fos.pinLen) {
                                if (fos.checkLock(it)) onSuccess() else fail()
                            }
                        },
                    )
                }
                LockType.PASSWORD -> {
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        singleLine = true,
                        isError = error,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (fos.checkLock(pass)) onSuccess() else fail()
                        }),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
                    )
                    Button(
                        onClick = { if (fos.checkLock(pass)) onSuccess() else fail() },
                        modifier = Modifier.padding(top = 20.dp),
                    ) { Text(l("Разблокировать", "Unlock")) }
                }
                LockType.PATTERN -> {
                    PatternPad(
                        error = error,
                        onComplete = { p ->
                            if (error) return@PatternPad
                            if (p.size >= 4 && fos.checkLock(p.joinToString("-"))) onSuccess() else fail()
                        },
                    )
                }
                LockType.NONE -> LaunchedEffect(Unit) { onSuccess() }
            }
        }
    }
}
