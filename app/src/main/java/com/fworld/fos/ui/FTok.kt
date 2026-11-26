package com.fworld.fos.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fworld.fos.LocalFos
import com.fworld.fos.R
import com.fworld.fos.ftok.TokApi
import com.fworld.fos.ftok.TokVideo
import com.fworld.fos.lang
import kotlinx.coroutines.launch

private const val MAX_UPLOAD = 50L * 1024 * 1024

private enum class TokTab { HOME, SEARCH, ADD, PROFILE }

@Composable
fun FTokApp(onClose: () -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    val api = remember(fos.ftokServer) { TokApi(fos.ftokServer) }
    var tab by remember { mutableStateOf(TokTab.HOME) }
    var viewer by remember { mutableStateOf<Pair<List<TokVideo>, Int>?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    BackHandler(enabled = viewer != null || tab != TokTab.HOME) {
        if (viewer != null) viewer = null else tab = TokTab.HOME
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val v = viewer
        if (v != null) {
            Feed(v.first, v.second, api)
            RoundIconButton(
                R.drawable.ic_back, { viewer = null },
                Modifier.statusBarsPadding().padding(12.dp),
                container = Color.Black.copy(alpha = 0.45f), tint = Color.White,
            )
        } else {
            when (tab) {
                TokTab.HOME -> HomeFeed(api, refreshKey, onExit = onClose)
                TokTab.SEARCH -> SearchTab(api) { list, i -> viewer = list to i }
                TokTab.ADD -> UploadTab(api, onDone = { refreshKey++; tab = TokTab.HOME })
                TokTab.PROFILE -> ProfileTab(api, refreshKey) { list, i -> viewer = list to i }
            }
            Dock(
                current = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp),
            )
        }
    }
}

/** Плавающий док: Главная, Поиск, +, Профиль — на фигурах M3 Expressive */
@Composable
private fun Dock(current: TokTab, onSelect: (TokTab) -> Unit, modifier: Modifier = Modifier) {
    val l = lang()
    Row(
        modifier
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xE61C1B22))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockItem(R.drawable.ic_home, l("Главная", "Home"), current == TokTab.HOME) { onSelect(TokTab.HOME) }
        DockItem(R.drawable.ic_search, l("Поиск", "Search"), current == TokTab.SEARCH) { onSelect(TokTab.SEARCH) }
        // Центральная кнопка «+»
        GradientShape(
            ShapeKind.SoftBurst, listOf(Color(0xFFFF4081), Color(0xFF7B1FA2)), 56.dp,
            Modifier.clickable { onSelect(TokTab.ADD) },
        ) { FosIcon(R.drawable.ic_add, Color.White, 28.dp) }
        DockItem(R.drawable.ic_person, l("Профиль", "Profile"), current == TokTab.PROFILE) { onSelect(TokTab.PROFILE) }
    }
}

@Composable
private fun DockItem(icon: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val w by animateDpAsState(if (selected) 92.dp else 52.dp, label = "dockW")
    val bg by animateColorAsState(if (selected) Color.White else Color.Transparent, label = "dockBg")
    val fg = if (selected) Color(0xFF1C1B22) else Color.White.copy(alpha = 0.8f)
    Row(
        Modifier.size(width = w, height = 48.dp).clip(RoundedCornerShape(24.dp)).background(bg).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FosIcon(icon, fg, 24.dp)
        if (selected) Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun Empty(title: String, text: String, shape: ShapeKind = ShapeKind.Cookie9, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ShapeBadge(shape, Color(0xFF2A2833), 110.dp) { FosIcon(R.drawable.ic_music, Color.White, 48.dp) }
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp))
        Text(text, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        action?.invoke()
    }
}

@Composable
private fun HomeFeed(api: TokApi, refreshKey: Int, onExit: () -> Unit) {
    val l = lang()
    var videos by remember { mutableStateOf<List<TokVideo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(api, refreshKey, reload) {
        if (!api.configured) return@LaunchedEffect
        loading = true; error = null
        runCatching { api.list() }.onSuccess { videos = it }.onFailure { error = it.message ?: "error" }
        loading = false
    }

    when {
        !api.configured -> Empty(
            l("Сервер не подключён", "No server connected"),
            l("Откройте Профиль и укажите адрес сервера fTok-Server.", "Open Profile and set your fTok-Server address."),
        )
        loading && videos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
        error != null -> Empty(
            l("Не удалось загрузить", "Couldn't load"), error ?: "", ShapeKind.Burst,
        ) { FilledTonalButton(onClick = { reload++ }) { Text(l("Повторить", "Retry")) } }
        videos.isEmpty() -> Empty(
            l("Пока нет видео", "No videos yet"),
            l("Нажмите + и загрузите первое видео.", "Tap + to upload the first video."), ShapeKind.Flower,
        )
        else -> Feed(videos, 0, api)
    }
}

/** Вертикальная лента с одним ExoPlayer */
@OptIn(UnstableApi::class)
@Composable
private fun Feed(videos: List<TokVideo>, start: Int, api: TokApi) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = start.coerceIn(0, (videos.size - 1).coerceAtLeast(0))) { videos.size }
    val player = remember { ExoPlayer.Builder(ctx).build().apply { repeatMode = Player.REPEAT_MODE_ONE; playWhenReady = true } }
    val likes = remember(videos) { mutableStateOf(videos.associate { it.id to it.likes }) }
    val liked = remember { mutableStateOf(setOf<String>()) }

    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(pager.settledPage, videos) {
        videos.getOrNull(pager.settledPage)?.let {
            player.setMediaItem(MediaItem.fromUri(it.url))
            player.prepare()
            player.play()
        }
    }

    VerticalPager(pager, Modifier.fillMaxSize()) { page ->
        val v = videos[page]
        Box(Modifier.fillMaxSize().clickable { if (player.isPlaying) player.pause() else player.play() }) {
            if (page == pager.settledPage) {
                AndroidView(
                    factory = { c ->
                        PlayerView(c).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            this.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Градиент и подписи
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().size(260.dp).background(
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
            ))
            Column(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 96.dp, bottom = 120.dp)) {
                Text("@${v.user}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (v.title.isNotBlank()) Text(v.title, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
            Column(Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val isLiked = v.id in liked.value
                ShapeBadge(
                    ShapeKind.Cookie9,
                    if (isLiked) Color(0xFFFF4081) else Color.White.copy(alpha = 0.18f),
                    56.dp,
                    Modifier.clickable {
                        if (!isLiked) {
                            liked.value = liked.value + v.id
                            likes.value = likes.value + (v.id to (likes.value[v.id] ?: 0) + 1)
                            scope.launch { runCatching { api.like(v.id) } }
                        }
                    },
                ) { FosIcon(R.drawable.ic_favorite, Color.White, 26.dp) }
                Text("${likes.value[v.id] ?: 0}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun VideoRow(v: TokVideo, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color(0xFF211F29)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeBadge(ShapeKind.Clover4, Color(0xFF3A2A55), 52.dp) { FosIcon(R.drawable.ic_play, Color.White, 24.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(v.title.ifBlank { "—" }, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@${v.user} · ♥ ${v.likes}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SearchTab(api: TokApi, onOpen: (List<TokVideo>, Int) -> Unit) {
    val l = lang()
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TokVideo>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(q, api) {
        if (!api.configured) return@LaunchedEffect
        kotlinx.coroutines.delay(350)
        busy = true
        runCatching { api.list(query = q.trim()) }.onSuccess { results = it; searched = true }
        busy = false
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true, shape = CircleShape,
            placeholder = { Text(l("Поиск видео и авторов", "Search videos and creators")) },
            leadingIcon = { FosIcon(R.drawable.ic_search, Color.White.copy(alpha = 0.7f), 20.dp) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
        )
        if (!api.configured) {
            Empty(l("Сервер не подключён", "No server connected"), l("Укажите сервер в Профиле.", "Set the server in Profile."))
        } else if (busy && results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
        } else if (searched && results.isEmpty()) {
            Empty(l("Ничего не найдено", "Nothing found"), "", ShapeKind.Puffy)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp)) {
                items(results.size) { i -> VideoRow(results[i]) { onOpen(results, i) } }
            }
        }
    }
}

@Composable
private fun UploadTab(api: TokApi, onDone: () -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf("") }
    var size by remember { mutableStateOf(0L) }
    var title by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null) {
            ctx.contentResolver.query(u, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (ni >= 0) name = c.getString(ni) ?: ""
                    if (si >= 0) size = c.getLong(si)
                }
            }
            uri = u; msg = null
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 120.dp)) {
        Text(l("Новое видео", "New video"), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Color(0xFF211F29))
                .clickable(enabled = !busy) { picker.launch("video/*") }.padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GradientShape(ShapeKind.SoftBurst, listOf(Color(0xFFFF4081), Color(0xFF7B1FA2)), 88.dp) { FosIcon(R.drawable.ic_add, Color.White, 40.dp) }
                Text(
                    if (uri == null) l("Выбрать видео", "Choose a video") else name,
                    color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp),
                )
                Text(l("MP4, до 50 МБ", "MP4, up to 50 MB"), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
        OutlinedTextField(
            value = title, onValueChange = { title = it.take(120) }, shape = RoundedCornerShape(24.dp),
            label = { Text(l("Описание", "Caption")) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        if (busy) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp))
        msg?.let { Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 12.dp)) }
        Button(
            onClick = {
                val u = uri ?: return@Button
                if (!api.configured) { msg = l("Сначала укажите сервер в Профиле", "Set the server in Profile first"); return@Button }
                if (size > MAX_UPLOAD) { msg = l("Файл больше 50 МБ", "File is over 50 MB"); return@Button }
                busy = true; progress = 0f; msg = null
                scope.launch {
                    runCatching {
                        api.upload(ctx.contentResolver, u, fos.ftokUser.ifBlank { "guest" }, title.trim(), size) { value -> progress = value }
                    }.onSuccess { onDone() }.onFailure { msg = it.message ?: "error" }
                    busy = false
                }
            },
            enabled = uri != null && !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        ) { Text(l("Опубликовать", "Publish")) }
    }
}

@Composable
private fun ProfileTab(api: TokApi, refreshKey: Int, onOpen: (List<TokVideo>, Int) -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    var name by remember { mutableStateOf(fos.ftokUser) }
    var server by remember { mutableStateOf(fos.ftokServer) }
    var mine by remember { mutableStateOf<List<TokVideo>>(emptyList()) }

    LaunchedEffect(api, refreshKey, fos.ftokUser) {
        if (api.configured && fos.ftokUser.isNotBlank()) {
            runCatching { api.list(user = fos.ftokUser) }.onSuccess { mine = it }
        } else mine = emptyList()
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                GradientShape(ShapeKind.Cookie12, listOf(Color(0xFFFF4081), Color(0xFF7B1FA2)), 108.dp) {
                    Text((fos.ftokUser.firstOrNull() ?: '?').uppercase(), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                }
                Text("@${fos.ftokUser.ifBlank { "guest" }}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text("${mine.size} " + l("видео", "videos"), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        }
        item {
            OutlinedTextField(
                value = name, onValueChange = { name = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(24) },
                label = { Text(l("Имя пользователя", "Username")) }, singleLine = true, shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = server, onValueChange = { server = it.trim() },
                label = { Text(l("Адрес сервера fTok", "fTok server address")) },
                placeholder = { Text("https://your-server.example.com") },
                singleLine = true, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = { fos.update { ftokUser = name; ftokServer = server.trimEnd('/') } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(l("Сохранить", "Save")) }
        }
        if (mine.isNotEmpty()) {
            item { Text(l("Мои видео", "My videos"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            items(mine.size) { i -> VideoRow(mine[i]) { onOpen(mine, i) } }
        }
    }
}
