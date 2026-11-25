package com.fworld.fos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fworld.fos.LocalFos
import com.fworld.fos.R
import com.fworld.fos.lang
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class GramMsg(val text: String, val mine: Boolean, val time: String)

class GramChat(val id: Int, val nameRu: String, val nameEn: String, val color: Color, val replies: List<Pair<String, String>>) {
    val messages = mutableStateListOf<GramMsg>()
    var unread by mutableStateOf(0)
    fun name(ru: Boolean) = if (ru) nameRu else nameEn
}

/** Данные чатов живут пока работает приложение fOS (в памяти) */
private object GramData {
    private fun t() = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    val chats: List<GramChat> = listOf(
        GramChat(
            1, "fworld_team", "fworld_team", Color(0xFF7C4DFF),
            listOf(
                "Спасибо, что пользуетесь fOS!" to "Thanks for using fOS!",
                "Мы работаем над новыми функциями" to "We are working on new features",
                "Следите за обновлениями" to "Stay tuned for updates",
            ),
        ).apply {
            messages.add(GramMsg("Добро пожаловать в fGram! / Welcome to fGram!", false, t()))
            unread = 1
        },
        GramChat(
            2, "Саша", "Alex", Color(0xFF26A69A),
            listOf(
                "Привет! Как дела?" to "Hi! How are you?",
                "Го гулять вечером" to "Let's hang out tonight",
                "Окей, договорились" to "Okay, deal",
            ),
        ).apply { messages.add(GramMsg("Привет! / Hey!", false, t())) },
        GramChat(
            3, "Мама", "Mom", Color(0xFFEF5350),
            listOf(
                "Не забудь поесть" to "Don't forget to eat",
                "Позвони мне вечером" to "Call me tonight",
                "Люблю тебя" to "Love you",
            ),
        ),
        GramChat(
            4, "Заметки", "Saved Messages", Color(0xFF29B6F6),
            emptyList(),
        ),
    )
}

@Composable
fun FGramApp(onClose: () -> Unit) {
    val l = lang()
    var openId by remember { mutableStateOf<Int?>(null) }
    BackHandler(enabled = openId != null) { openId = null }

    val chat = GramData.chats.firstOrNull { it.id == openId }
    if (chat == null) {
        AppScaffold(title = "fGram", onBack = onClose) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(GramData.chats, key = { it.id }) { c -> ChatListItem(c) { openId = c.id; c.unread = 0 } }
            }
        }
    } else {
        ChatScreen(chat, onBack = { openId = null })
    }
}

@Composable
private fun Avatar(name: String, color: Color, size: Int = 50) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = Color.White, fontSize = (size * 0.42f).sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChatListItem(c: GramChat, onClick: () -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    val last = c.messages.lastOrNull()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(c.name(fos.langRu), c.color)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(c.name(fos.langRu), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                last?.text ?: l("Нет сообщений", "No messages"),
                fontSize = 14.sp, maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (last != null) Text(last.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            if (c.unread > 0) {
                Box(
                    Modifier.padding(top = 4.dp).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { Text("${c.unread}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun ChatScreen(c: GramChat, onBack: () -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    var input by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val time = { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }

    // Автоответ собеседника
    LaunchedEffect(pending) {
        if (pending > 0 && c.replies.isNotEmpty()) {
            typing = true
            delay(1400)
            val r = c.replies.random()
            c.messages.add(GramMsg(if (fos.langRu) r.first else r.second, false, time()))
            typing = false
        }
    }
    LaunchedEffect(c.messages.size) {
        if (c.messages.isNotEmpty()) listState.animateScrollToItem(c.messages.size - 1)
    }

    AppScaffold(
        title = c.name(fos.langRu),
        onBack = onBack,
        modifier = Modifier.imePadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            if (typing) {
                Text(l("печатает…", "typing…"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(c.messages) { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start) {
                        Column(
                            Modifier
                                .widthIn(max = 280.dp)
                                .clip(RoundedCornerShape(20.dp, 20.dp, if (m.mine) 4.dp else 20.dp, if (m.mine) 20.dp else 4.dp))
                                .background(if (m.mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(m.text, fontSize = 15.sp,
                                color = if (m.mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            Text(m.time, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(l("Сообщение", "Message")) },
                    shape = RoundedCornerShape(28.dp),
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                Box(Modifier.size(8.dp))
                RoundIconButton(
                    R.drawable.ic_send,
                    onClick = {
                        val t = input.trim()
                        if (t.isNotEmpty()) {
                            c.messages.add(GramMsg(t, true, time()))
                            input = ""
                            pending++
                        }
                    },
                    size = 52.dp,
                    container = MaterialTheme.colorScheme.primary,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
