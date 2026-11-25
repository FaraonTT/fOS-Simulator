package com.fworld.fos.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fworld.fos.R
import com.fworld.fos.lang
import kotlin.math.cos
import kotlin.math.sin

private class Clip(
    val user: String,
    val captionRu: String,
    val captionEn: String,
    val likes: Int,
    val comments: Int,
    val colors: List<Color>,
)

private val Clips = listOf(
    Clip("@fworld", "Первый ролик в fTok 🎉", "First clip on fTok 🎉", 12400, 320,
        listOf(Color(0xFF1B1464), Color(0xFFE05297))),
    Clip("@neon.cat", "Неоновый вайб ночью", "Neon night vibes", 8750, 140,
        listOf(Color(0xFF0F2027), Color(0xFF3FB6A8))),
    Clip("@sunset_daily", "Закат за 15 секунд", "Sunset in 15 seconds", 20100, 512,
        listOf(Color(0xFF6F0000), Color(0xFFFF8A4C))),
    Clip("@ocean.mood", "Море — это спокойствие", "The sea is calm", 5320, 77,
        listOf(Color(0xFF0B1B3A), Color(0xFF7FD6FF))),
    Clip("@pixel_dev", "Как я пишу на Kotlin", "How I code in Kotlin", 15900, 408,
        listOf(Color(0xFF111111), Color(0xFF7C4DFF))),
)

/** fTok: вертикальная лента коротких «видео» (анимированные фоны) */
@Composable
fun FTokApp(onClose: () -> Unit) {
    val pager = rememberPagerState(pageCount = { Clips.size })
    val liked = remember { mutableStateMapOf<Int, Boolean>() }
    val following = remember { mutableStateMapOf<Int, Boolean>() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(pager, Modifier.fillMaxSize()) { page ->
            ClipPage(
                clip = Clips[page],
                liked = liked[page] == true,
                following = following[page] == true,
                onLike = { liked[page] = liked[page] != true },
                onFollow = { following[page] = following[page] != true },
            )
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(R.drawable.ic_back, onClose, container = Color.Black.copy(alpha = 0.35f), tint = Color.White)
            Text("fTok", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 14.dp))
        }
    }
}

@Composable
private fun ClipPage(clip: Clip, liked: Boolean, following: Boolean, onLike: () -> Unit, onFollow: () -> Unit) {
    val l = lang()
    val t = rememberInfiniteTransition(label = "clip")
    val phase by t.animateFloat(
        0f, (Math.PI * 2).toFloat(),
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    var heart by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(clip.colors))
            .clickable { heart = !heart; if (!liked) onLike() }
    ) {
        // Анимированные «частицы» вместо видео
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until 7) {
                val a = phase + i * 0.9f
                val c = Offset(
                    size.width * (0.5f + 0.35f * cos(a + i)),
                    size.height * (0.5f + 0.25f * sin(a * 0.8f + i * 1.7f)),
                )
                drawCircle(Color.White.copy(alpha = 0.10f), radius = size.width * (0.12f + 0.03f * i), center = c)
            }
        }

        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                Text(clip.user.drop(1).take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(18.dp))
            Action(R.drawable.ic_favorite, fmt(clip.likes + if (liked) 1 else 0),
                if (liked) Color(0xFFFF4D6D) else Color.White, onLike)
            Box(Modifier.size(14.dp))
            Action(R.drawable.ic_chat, fmt(clip.comments), Color.White) {}
            Box(Modifier.size(14.dp))
            Action(R.drawable.ic_share, l("Поделиться", "Share"), Color.White) {}
        }

        Column(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 16.dp, end = 90.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(clip.user, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Box(
                    Modifier.padding(start = 10.dp).clip(CircleShape)
                        .background(if (following) Color.White.copy(alpha = 0.25f) else Color.White)
                        .clickable(onClick = onFollow)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (following) l("Вы подписаны", "Following") else l("Подписаться", "Follow"),
                        color = if (following) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    )
                }
            }
            Text(l(clip.captionRu, clip.captionEn), color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                FosIcon(R.drawable.ic_music, Color.White, 14.dp)
                Text(" ${l("Оригинальный звук", "Original sound")} — ${clip.user}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Action(icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        FosIcon(icon, tint, 34.dp)
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

private fun fmt(n: Int): String = if (n >= 1000) "%.1fK".format(n / 1000f) else n.toString()
