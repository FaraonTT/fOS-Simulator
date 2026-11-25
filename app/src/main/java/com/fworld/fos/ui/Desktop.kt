package com.fworld.fos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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

/** Режим верхней шторки: закрыта → уведомления → панель управления */
private enum class Shade { CLOSED, NOTIFS, CONTROLS }

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
            enter = slideInVertically(tween(280)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(),
        ) {
            AppDrawer(onOpen = { openApp = it; drawer = false }, onClose = { drawer = false })
        }

        // Открытое приложение
        AnimatedVisibility(
            visible = openApp != null,
            enter = scaleIn(tween(260), initialScale = 0.85f) + fadeIn(tween(260)),
            exit = scaleOut(tween(200), targetScale = 0.85f) + fadeOut(tween(200)),
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

private const val COLS = 4
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
                listOf(AppId.STORE, AppId.GRAM, AppId.TOK, AppId.SETTINGS).forEach { app ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) { onOpen(app) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        AppIcon(app, 58.dp)
                        Text(
                            appLabel(app, fos.langRu),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp),
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
    Box(
        modifier.padding(4.dp).clip(MaterialTheme.shapes.extraLarge).background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(now.timeText(fos.use24h), color = Color.White, fontSize = 66.sp, fontWeight = FontWeight.Light)
            Text(now.dateText(fos.langRu), color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
        }
    }
}

@Composable
private fun AppDrawer(onOpen: (AppId) -> Unit, onClose: () -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    var query by remember { mutableStateOf("") }
    val all = listOf(AppId.STORE, AppId.GRAM, AppId.TOK, AppId.SETTINGS).filter {
        it == AppId.STORE || it == AppId.SETTINGS || it.key in fos.installed
    }
    val shown = all.filter { query.isBlank() || appLabel(it, fos.langRu).contains(query, ignoreCase = true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                var t = 0f
                detectVerticalDragGestures(
                    onDragStart = { t = 0f },
                    onDragEnd = { if (t > 140f) onClose() },
                ) { _, dy -> t += dy }
            }
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 46.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 44.dp, height = 5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
        }
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(l("Поиск приложений", "Search apps")) },
            leadingIcon = { FosIcon(R.drawable.ic_search, MaterialTheme.colorScheme.onSurfaceVariant, 20.dp) },
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(COLS),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(shown) { app ->
                Column(
                    Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                    ) { onOpen(app) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppIcon(app, 58.dp)
                    Text(
                        appLabel(app, fos.langRu),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
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
        enter = slideInVertically(tween(260)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(220)) { -it } + fadeOut(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) { onShade(Shade.CLOSED) }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                    ) {}
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
    Column(Modifier.fillMaxWidth().height(420.dp).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                l("Уведомления", "Notifications"),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (fos.notifs.isNotEmpty()) {
                Text(
                    l("Очистить", "Clear all"),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { fos.notifs.clear() }.padding(8.dp),
                )
            }
        }
        if (fos.notifs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(l("Нет уведомлений", "No notifications"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fos.notifs, key = { it.id }) { n -> NotifCard(n, fos) }
            }
        }
        Text(
            l("Потяните вниз — панель управления", "Pull down for quick settings"),
            color = MaterialTheme.colorScheme.outline,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
        )
    }
}

@Composable
private fun NotifCard(n: FosNotif, fos: FosState) {
    val time = Instant.ofEpochMilli(n.time).atZone(ZoneId.systemDefault()).toLocalDateTime().timeText(fos.use24h)
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { FosIcon(R.drawable.ic_notifications, MaterialTheme.colorScheme.onPrimaryContainer, 20.dp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("${n.app} · $time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(n.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(n.text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FosIcon(
            R.drawable.ic_close, MaterialTheme.colorScheme.onSurfaceVariant, 20.dp,
            Modifier.clickable { fos.notifs.remove(n) }.padding(4.dp),
        )
    }
}

@Composable
private fun ControlPanel() {
    val fos = LocalFos.current
    val l = lang()
    val bat by rememberBattery()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            l("Панель управления", "Quick settings"),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val tiles: List<Triple<Int, String, Pair<Boolean, () -> Unit>>> = listOf(
            Triple(R.drawable.ic_wifi, "Wi-Fi", (fos.wifiOn && !fos.airplane) to { fos.update { wifiOn = !wifiOn } }),
            Triple(R.drawable.ic_bluetooth, "Bluetooth", (fos.bluetoothOn && !fos.airplane) to { fos.update { bluetoothOn = !bluetoothOn } }),
            Triple(R.drawable.ic_airplane, l("Авиарежим", "Airplane"), fos.airplane to { fos.airplane = !fos.airplane }),
            Triple(R.drawable.ic_flashlight, l("Фонарик", "Flashlight"), fos.flashlight to { fos.flashlight = !fos.flashlight }),
            Triple(R.drawable.ic_dnd, l("Не беспокоить", "DND"), fos.dnd to { fos.dnd = !fos.dnd }),
            Triple(R.drawable.ic_rotate, l("Поворот", "Rotate"), fos.autoRotate to { fos.autoRotate = !fos.autoRotate }),
            Triple(R.drawable.ic_location, l("Геолокация", "Location"), fos.locationOn to { fos.update { locationOn = !locationOn } }),
            Triple(R.drawable.ic_battery, l("Эконом.", "Saver"), fos.batterySaver to { fos.batterySaver = !fos.batterySaver }),
        )
        tiles.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (icon, label, state) ->
                    val (on, toggle) = state
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = toggle)
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FosIcon(icon, if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, 24.dp)
                        Text(
                            label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
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
        Text(
            "${l("Батарея", "Battery")}: ${bat.first}%" + if (bat.second) " ⚡" else "",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
