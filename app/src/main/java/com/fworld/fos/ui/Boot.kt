package com.fworld.fos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fworld.fos.FosConfig
import com.fworld.fos.lang
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Bootanimation #1: «fworld» по центру, «Powered by fworld» снизу, 7 секунд */
@Composable
fun FworldBoot(onDone: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.85f) }
    val inf = rememberInfiniteTransition(label = "glow")
    val glow by inf.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha",
    )

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(1500)) }
        launch { scale.animateTo(1f, tween(2000, easing = FastOutSlowInEasing)) }
        delay(FosConfig.FWORLD_BOOT_MS)
        onDone()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(Modifier.align(Alignment.Center).size(320.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF7C5CFF).copy(alpha = glow * alpha.value), Color.Transparent)
                ),
            )
        }
        Text(
            text = "fworld",
            color = Color.White,
            fontSize = 54.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 6.sp,
            modifier = Modifier.align(Alignment.Center).graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
            },
        )
        Text(
            text = "Powered by fworld",
            color = Color(0xFF8E8E99),
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp).graphicsLayer {
                this.alpha = alpha.value
            },
        )
    }
}

/** Bootanimation-os: анимация загрузки fOS (30 с при первом запуске, 10 с после настройки) */
@Composable
fun OsBoot(durationMs: Long, firstBoot: Boolean, onDone: () -> Unit) {
    val l = lang()
    val progress = remember { Animatable(0f) }
    val inf = rememberInfiniteTransition(label = "osboot")
    val rot by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "rot",
    )
    val morph by inf.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "morph",
    )
    val pulse by inf.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMs.toInt(), easing = LinearEasing))
        onDone()
    }

    val p = progress.value
    val stage = when {
        p < 0.15f -> l("Запуск ядра…", "Starting kernel…")
        p < 0.35f -> l("Монтирование разделов…", "Mounting partitions…")
        p < 0.60f -> l("Запуск системных служб…", "Starting system services…")
        p < 0.85f -> if (firstBoot) l("Оптимизация приложений…", "Optimizing apps…") else l("Загрузка интерфейса…", "Loading interface…")
        else -> l("Завершение загрузки…", "Finishing boot…")
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0B0716), Color(0xFF1A1033), Color(0xFF0B0716)))
        )
    ) {
        Box(Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(240.dp)) {
                val c = center
                val cols = listOf(Color(0xFFB69CFF), Color(0xFFFF7EB6), Color(0xFF6FD6FF))
                cols.forEachIndexed { i, col ->
                    val s = size.minDimension * (0.92f - i * 0.16f) * pulse
                    val dir = if (i % 2 == 0) 1f else -1f
                    rotate(rot * dir + i * 40f, pivot = c) {
                        drawRoundRect(
                            color = col.copy(alpha = 0.8f),
                            topLeft = Offset(c.x - s / 2, c.y - s / 2),
                            size = Size(s, s),
                            cornerRadius = CornerRadius(s * morph),
                        )
                    }
                }
            }
            Text("fOS", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 48.dp).padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stage, color = Color(0xFFD6D0E8), fontSize = 14.sp)
            Box(
                Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2E2547))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(p.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFB69CFF), Color(0xFFFF7EB6))))
                )
            }
            Text(
                "fOS 1.0.26.73",
                color = Color(0xFF6E6685),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}
