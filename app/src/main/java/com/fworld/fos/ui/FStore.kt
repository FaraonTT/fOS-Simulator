package com.fworld.fos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fworld.fos.LocalFos
import com.fworld.fos.R
import com.fworld.fos.lang

/** fStore: в магазине только 2 приложения — fGram и fTok */
@Composable
fun FStoreApp(onClose: () -> Unit) {
    val l = lang()
    AppScaffold(title = "fStore", onBack = onClose) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                l("Приложения", "Apps"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            StoreCard(
                app = AppId.GRAM,
                desc = l("Быстрый и безопасный мессенджер", "Fast and secure messenger"),
                size = l("48 МБ", "48 MB"),
            )
            StoreCard(
                app = AppId.TOK,
                desc = l("Короткие видео для всех", "Short videos for everyone"),
                size = l("112 МБ", "112 MB"),
            )
        }
    }
}

@Composable
private fun StoreCard(app: AppId, desc: String, size: String) {
    val fos = LocalFos.current
    val l = lang()
    val isInstalled = app.key in fos.installed
    var downloading by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(downloading) {
        if (downloading) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(3500, easing = LinearEasing))
            fos.update { installed = installed + app.key }
            fos.postNotif("fStore", l("Установлено", "Installed"), l("${appLabel(app, true)} установлено", "${appLabel(app, false)} installed"))
            downloading = false
        }
    }

    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, 60.dp)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(appLabel(app, true), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(size, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            if (downloading) {
                LinearProgressIndicator(progress = { progress.value }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
        when {
            downloading -> Text("${(progress.value * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
            isInstalled -> FilledTonalButton(onClick = { fos.update { installed = installed - app.key } }) {
                Text(l("Удалить", "Remove"))
            }
            else -> Button(onClick = { downloading = true }) { Text(l("Установить", "Install")) }
        }
    }
}
