package com.fworld.fos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fworld.fos.FosNotif
import com.fworld.fos.FosState
import com.fworld.fos.LocalFos
import com.fworld.fos.R
import com.fworld.fos.lang
import com.fworld.fos.wallpaperBrush
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Режим верхней шторки: закрыта → уведомления → панель управления */
private enum class Shade { CLOSED, NOTIFS, CONTROLS }

private val AppsOrder = listOf(AppId.STORE, AppId.GRAM, AppId.TOK, AppId.SETTINGS)

@Composable
fun Desktop() {
    val fos = LocalFos.current
    var openApp by remember { mutableStateOf<AppId?>(null) }
    var drawer by remember { mutableStateOf(false) }
    var shade by remember { mutableStateOf(Shade.CLOSED) }

    BackHandler(enabled = shade != Shade.CLOSED || drawer || openApp != null) {
        when {
            shade == Shade.CONTROLS -> shade = Shade.NOTIFS
            shade == Shade.NOTIFS -> shade = Shade.CLOSED
            drawer -> drawer = false
            else -> openApp = null
        }
    }

    Box(Modifier.fillMaxSize().background(wallpaperBrush(fos.wallpaper))) {
        // Декор обоев: большие полупрозрачные фигуры
        ShapeBadge(ShapeKind.Cookie12, Color.White.copy(alpha = 0.06f), 420.dp, Modifier.offset(x = (-150).dp, y = 380.dp)) {}
        ShapeBadge(ShapeKind.Clover8, Color.White.copy(alpha = 0.05f), 340.dp, Modifier.align(Alignment.TopEnd).offset(x = 140.dp, y = (-60).dp)) {}

        // Главный экран и жесты
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(openApp, drawer, shade) {
                    if (openApp != null || drawer || shade != Shade.CLOSED) return@pointerInput
                    var total = 0f
                    var startY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f; startY = it.y },
                        onDragEnd = {
                            if (total > 120f && startY < size.height * 0.55f) shade = Shade.NOTIFS
                            else if (total < -120f) drawer = true
                        },
                    ) { _, dy -> total += dy }
                }
        ) {
            HomeScreen(onOpen = { openApp = it })
        }

        StatusBar(Modifier.align(Alignment.TopCenter))

        // Меню приложений (снизу вверх)
        AnimatedVisibility(
            visible = drawer && openApp == null,
            enter = slideInVertically(tween(300)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(),
        ) {
            AppDrawer(onOpen = { openApp = it; drawer = false }, onClose = { drawer = false })
        }

        // Открытое приложение
        AnimatedVisibility(
            visible = openApp != null,
            enter = scaleIn(tween(280), initialScale = 0.88f) + fadeIn(tween(280)),
            exit = scaleOut(tween(200), targetScale = 0.88f) + fadeOut(tween(200)),
        ) {
            val app = openApp
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (app) {
                    AppId.STORE -> FStoreApp(onClose = { openApp = null })
                    AppId.GRAM -> FGramApp(onClose = { openApp = null })
                    AppId.TOK -> FTokApp(onClose = { openApp = null })
                    AppId.SETTINGS -> SettingsApp(onClose = { openApp = null })
                    null -> {}
                }
                // Жест «домой» снизу
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(20.dp)
                        .pointerInput(Unit) {
                            var t = 0f
                            detectVerticalDragGestures(
                                onDragStart = { t = 0f },
                                onDragEnd = { if (t < -60f) openApp = null },
                            ) { _, dy -> t += dy }
                        }
                )
            }
        }

        // Шторки
        ShadeLayer(shade = shade, onShade = { shade = it })
    }
}

@Composable
private fun StatusBar(modifier: Modifier = Modifier) {
    val fos = LocalFos.current
    val now by rememberNow()
    val bat by rememberBattery()
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 22.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(now.timeText(fos.use24h), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(Modifier.weight(1f))
        if (fos.dnd) FosIcon(R.drawable.ic_dnd, Color.White, 15.dp, Modifier.padding(end = 6.dp))
        if (fos.airplane) FosIcon(R.drawable.ic_airplane, Color.White, 15.dp, Modifier.padding(end = 6.dp))
        else if (fos.wifiOn && fos.wifiName.isNotEmpty()) FosIcon(R.drawable.ic_wifi, Color.White, 15.dp, Modifier.padding(end = 6.dp))
        if (fos.bluetoothOn && !fos.airplane) FosIcon(R.drawable.ic_bluetooth, Color.White, 15.dp, Modifier.padding(end = 6.dp))
        BatteryGlyph(bat.first, Color.White)
        Text("${bat.first}%", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

private const val ROWS = 5

/** Рабочий стол 5x4 (5 рядов × 4 колонки): виджет часов 2x4 + приложения */
@Composable
private fun HomeScreen(onOpen: (AppId) -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val topPad = 36.dp
        val bottomPad = 28.dp
        val cellH = (maxHeight - topPad - bottomPad) / ROWS
        Column(Modifier.fillMaxSize().padding(top = topPad, start = 8.dp, end = 8.dp)) {
            // Виджет 2 ряда × 4 колонки
            ClockWidget(Modifier.fillMaxWidth().height(cellH * 2))
            // Ряд 3: приложения
            Row(Modifier.fillMaxWidth().height(cellH)) {
                AppsOrder.forEach { app ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onOpen(app) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        AppIcon(app, 62.dp)
                        Text(
                            appLabel(app, fos.langRu),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 7.dp),
                        )
                    }
                }
            }
        }
        Text(
            l("Проведите вверх — приложения", "Swipe up for apps"),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun ClockWidget(modifier: Modifier = Modifier) {
    val fos = LocalFos.current
    val now by rememberNow()
    val loc = if (fos.langRu) Locale("ru") else Locale.ENGLISH
    val dow = now.format(DateTimeFormatter.ofPattern("EEE", loc)).replaceFirstChar { it.uppercase() }
    val month = now.format(DateTimeFormatter.ofPattern("MMMM", loc)).replaceFirstChar { it.uppercase() }
    Row(
        modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(Color.Black.copy(alpha = 0.30f))
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                now.timeText(fos.use24h),
                color = Color.White,
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            Text(month, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
        }
        ShapeBadge(ShapeKind.Cookie12, Color.White.copy(alpha = 0.92f), 92.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${now.dayOfMonth}", color = Color(0xFF2B1F7B), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(dow, color = Color(0xFF2B1F7B).copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AppDrawer(onOpen: (AppId) -> Unit, onClose: () -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    var query by remember { mutableStateOf("") }
    val all = AppsOrder.filter { it == AppId.STORE || it == AppId.SETTINGS || it.key in fos.installed }
    val shown = all.filter { query.isBlank() || appLabel(it, fos.langRu).contains(query, ignoreCase = true) }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 28.dp)
                .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp))
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    var t = 0f
                    detectVerticalDragGestures(
                        onDragStart = { t = 0f },
                        onDragEnd = { if (t > 140f) onClose() },
                    ) { _, dy -> t += dy }
                }
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 44.dp, height = 5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(l("Поиск приложений", "Search apps")) },
                leadingIcon = { FosIcon(R.drawable.ic_search, MaterialTheme.colorScheme.onSurfaceVariant, 20.dp) },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                items(shown) { app ->
                    Column(
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onOpen(app) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIcon(app, 62.dp)
                        Text(
                            appLabel(app, fos.langRu),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Верхняя шторка: свайп вниз → уведомления, ещё раз → панель управления */
@Composable
private fun ShadeLayer(shade: Shade, onShade: (Shade) -> Unit) {
    // Узкая зона у верхнего края для жеста вниз из любого приложения
    if (shade == Shade.CLOSED) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .pointerInput(Unit) {
                    var t = 0f
                    detectVerticalDragGestures(
                        onDragStart = { t = 0f },
                        onDragEnd = { if (t > 50f) onShade(Shade.NOTIFS) },
                    ) { _, dy -> t += dy }
                }
        )
    }
    AnimatedVisibility(
        visible = shade != Shade.CLOSED,
        enter = slideInVertically(tween(280)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(220)) { -it } + fadeOut(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onShade(Shade.CLOSED) }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .pointerInput(shade) {
                        var t = 0f
                        detectVerticalDragGestures(
                            onDragStart = { t = 0f },
                            onDragEnd = {
                                if (t > 70f && shade == Shade.NOTIFS) onShade(Shade.CONTROLS)
                                else if (t < -70f) onShade(if (shade == Shade.CONTROLS) Shade.NOTIFS else Shade.CLOSED)
                            },
                        ) { _, dy -> t += dy }
                    }
                    .statusBarsPadding()
                    .padding(top = 30.dp, bottom = 16.dp)
            ) {
                AnimatedContent(targetState = shade, label = "shadeContent") { s ->
                    if (s == Shade.CONTROLS) ControlPanel() else NotifPanel()
                }
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 44.dp, height = 5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
    }
}

@Composable
private fun NotifPanel() {
    val fos = LocalFos.current
    val l = lang()
    Column(Modifier.fillMaxWidth().height(430.dp).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                l("Уведомления", "Notifications"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (fos.notifs.isNotEmpty()) {
                FilledTonalButton(onClick = { fos.notifs.clear() }) { Text(l("Очистить", "Clear all")) }
            }
        }
        if (fos.notifs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShapeBadge(ShapeKind.Cookie9, MaterialTheme.colorScheme.secondaryContainer, 84.dp) {
                        FosIcon(R.drawable.ic_notifications, MaterialTheme.colorScheme.onSecondaryContainer, 38.dp)
                    }
                    Text(
                        l("Нет уведомлений", "No notifications"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fos.notifs, key = { it.id }) { n -> NotifCard(n, fos) }
            }
        }
        Text(
            l("Потяните вниз — панель управления", "Pull down for quick settings"),
            color = MaterialTheme.colorScheme.outline,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
        )
    }
}

@Composable
private fun NotifCard(n: FosNotif, fos: FosState) {
    val time = Instant.ofEpochMilli(n.time).atZone(ZoneId.systemDefault()).toLocalDateTime().timeText(fos.use24h)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeBadge(ShapeKind.Cookie6, MaterialTheme.colorScheme.primaryContainer, 44.dp) {
            FosIcon(R.drawable.ic_notifications, MaterialTheme.colorScheme.onPrimaryContainer, 22.dp)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("${n.app} · $time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(n.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(n.text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FosIcon(
            R.drawable.ic_close, MaterialTheme.colorScheme.onSurfaceVariant, 20.dp,
            Modifier.clickable { fos.notifs.remove(n) }.padding(4.dp),
        )
    }
}

private class Tile(val icon: Int, val label: String, val on: Boolean, val toggle: () -> Unit)

@Composable
private fun QsTile(tile: Tile, modifier: Modifier = Modifier) {
    val corner by animateDpAsState(if (tile.on) 40.dp else 22.dp, label = "tileCorner")
    val bg by animateColorAsState(
        if (tile.on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "tileBg",
    )
    val fg = if (tile.on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .clickable(onClick = tile.toggle)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FosIcon(tile.icon, fg, 24.dp)
        Text(tile.label, fontSize = 11.sp, maxLines = 1, color = fg, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ControlPanel() {
    val fos = LocalFos.current
    val l = lang()
    val bat by rememberBattery()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.padding(start = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                l("Панель управления", "Quick settings"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            ShapeBadge(ShapeKind.Pill, MaterialTheme.colorScheme.secondaryContainer, 0.dp, Modifier.size(width = 76.dp, height = 36.dp)) {
                Text("${bat.first}%" + if (bat.second) " ⚡" else "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        val tiles = listOf(
            Tile(R.drawable.ic_wifi, "Wi-Fi", fos.wifiOn && !fos.airplane) { fos.update { wifiOn = !wifiOn } },
            Tile(R.drawable.ic_bluetooth, "Bluetooth", fos.bluetoothOn && !fos.airplane) { fos.update { bluetoothOn = !bluetoothOn } },
            Tile(R.drawable.ic_airplane, l("Авиарежим", "Airplane"), fos.airplane) { fos.airplane = !fos.airplane },
            Tile(R.drawable.ic_flashlight, l("Фонарик", "Flashlight"), fos.flashlight) { fos.flashlight = !fos.flashlight },
            Tile(R.drawable.ic_dnd, l("Не беспокоить", "DND"), fos.dnd) { fos.dnd = !fos.dnd },
            Tile(R.drawable.ic_rotate, l("Поворот", "Rotate"), fos.autoRotate) { fos.autoRotate = !fos.autoRotate },
            Tile(R.drawable.ic_location, l("Геолокация", "Location"), fos.locationOn) { fos.update { locationOn = !locationOn } },
            Tile(R.drawable.ic_battery, l("Эконом.", "Saver"), fos.batterySaver) { fos.batterySaver = !fos.batterySaver },
        )
        tiles.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t -> QsTile(t, Modifier.weight(1f)) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FosIcon(R.drawable.ic_brightness, MaterialTheme.colorScheme.onSurfaceVariant, 22.dp)
            Slider(
                value = fos.brightness,
                onValueChange = { fos.update { brightness = it } },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FosIcon(R.drawable.ic_volume, MaterialTheme.colorScheme.onSurfaceVariant, 22.dp)
            Slider(
                value = fos.volume,
                onValueChange = { fos.update { volume = it } },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
        }
    }
}
