package com.fworld.fos.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fworld.fos.FosConfig
import com.fworld.fos.FosState
import com.fworld.fos.LocalFos
import com.fworld.fos.LockType
import com.fworld.fos.R
import com.fworld.fos.lang

private enum class SPage { MAIN, NETWORK, DISPLAY, SOUND, SECURITY, LANG, ABOUT }

@Composable
fun SettingsApp(onClose: () -> Unit) {
    val l = lang()
    var page by remember { mutableStateOf(SPage.MAIN) }
    BackHandler(enabled = page != SPage.MAIN) { page = SPage.MAIN }

    val title = when (page) {
        SPage.MAIN -> l("Настройки", "Settings")
        SPage.NETWORK -> l("Сеть и интернет", "Network & internet")
        SPage.DISPLAY -> l("Экран и обои", "Display & wallpaper")
        SPage.SOUND -> l("Звук", "Sound")
        SPage.SECURITY -> l("Безопасность", "Security")
        SPage.LANG -> l("Язык и время", "Language & time")
        SPage.ABOUT -> l("О телефоне", "About phone")
    }
    AppScaffold(title, onBack = { if (page == SPage.MAIN) onClose() else page = SPage.MAIN }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            when (page) {
                SPage.MAIN -> MainPage { page = it }
                SPage.NETWORK -> NetworkPage()
                SPage.DISPLAY -> DisplayPage()
                SPage.SOUND -> SoundPage()
                SPage.SECURITY -> SecurityPage()
                SPage.LANG -> LangPage()
                SPage.ABOUT -> AboutPage()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MainPage(go: (SPage) -> Unit) {
    val fos = LocalFos.current
    val l = lang()
    val items = listOf(
        Triple(SPage.NETWORK, R.drawable.ic_wifi, l("Сеть и интернет", "Network & internet") to
            (if (fos.wifiName.isNotEmpty() && fos.wifiOn) fos.wifiName else l("Wi-Fi выключен", "Wi-Fi off"))),
        Triple(SPage.DISPLAY, R.drawable.ic_wallpaper, l("Экран и обои", "Display & wallpaper") to l("Тема, обои, яркость", "Theme, wallpaper, brightness")),
        Triple(SPage.SOUND, R.drawable.ic_volume, l("Звук", "Sound") to l("Громкость", "Volume")),
        Triple(SPage.SECURITY, R.drawable.ic_shield, l("Безопасность", "Security") to lockLabel(fos)),
        Triple(SPage.LANG, R.drawable.ic_language, l("Язык и время", "Language & time") to if (fos.langRu) "Русский" else "English"),
        Triple(SPage.ABOUT, R.drawable.ic_phone, l("О телефоне", "About phone") to "fPhone 1.0"),
    )
    if (fos.userName.isNotBlank()) {
        SettingsRow(
            title = fos.userName,
            subtitle = l("Аккаунт fOS", "fOS account"),
            icon = R.drawable.ic_person,
        )
    }
    items.forEach { (p, icon, text) ->
        SettingsRow(title = text.first, subtitle = text.second, icon = icon, onClick = { go(p) })
    }
}

@Composable
private fun lockLabel(fos: FosState): String {
    val l = lang()
    return when (fos.lockType) {
        LockType.NONE -> l("Блокировка выключена", "No lock")
        LockType.PIN -> "PIN"
        LockType.PASSWORD -> l("Пароль", "Password")
        LockType.PATTERN -> l("Графический ключ", "Pattern")
    }
}

@Composable
private fun NetworkPage() {
    val fos = LocalFos.current
    val l = lang()
    SwitchRow(
        title = "Wi-Fi",
        icon = R.drawable.ic_wifi,
        checked = fos.wifiOn,
        onChecked = { fos.update { wifiOn = it; if (!it) wifiName = "" } },
    )
    SwitchRow(
        title = "Bluetooth",
        icon = R.drawable.ic_bluetooth,
        checked = fos.bluetoothOn,
        onChecked = { fos.update { bluetoothOn = it } },
    )
    SwitchRow(
        title = l("Режим полёта", "Airplane mode"),
        icon = R.drawable.ic_airplane,
        checked = fos.airplane,
        onChecked = { fos.airplane = it },
    )
    if (fos.wifiOn) {
        SectionTitle(l("Доступные сети", "Available networks"))
        Column(Modifier.padding(horizontal = 16.dp)) { WifiList(fos) }
    }
}

@Composable
private fun DisplayPage() {
    val fos = LocalFos.current
    val l = lang()
    SectionTitle(l("Тема", "Theme"))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceRow(l("Тёмная", "Dark"), fos.themeMode == 2, { fos.update { themeMode = 2 } })
        ChoiceRow(l("Светлая", "Light"), fos.themeMode == 1, { fos.update { themeMode = 1 } })
        ChoiceRow(l("Как в системе", "Follow system"), fos.themeMode == 0, { fos.update { themeMode = 0 } })
    }
    SectionTitle(l("Обои", "Wallpaper"))
    Column(Modifier.padding(horizontal = 16.dp)) { WallpaperPicker(fos) }
    SectionTitle(l("Яркость", "Brightness"))
    Slider(
        value = fos.brightness,
        onValueChange = { fos.update { brightness = it } },
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun SoundPage() {
    val fos = LocalFos.current
    val l = lang()
    SectionTitle(l("Громкость мультимедиа", "Media volume"))
    Slider(
        value = fos.volume,
        onValueChange = { fos.update { volume = it } },
        modifier = Modifier.padding(horizontal = 20.dp),
    )
    SwitchRow(
        title = l("Не беспокоить", "Do not disturb"),
        subtitle = l("Уведомления не показываются", "Notifications are muted"),
        icon = R.drawable.ic_dnd,
        checked = fos.dnd,
        onChecked = { fos.dnd = it },
    )
}

@Composable
private fun SecurityPage() {
    val fos = LocalFos.current
    val l = lang()
    var chosen by remember { mutableStateOf(fos.lockType) }
    SectionTitle(l("Блокировка экрана", "Screen lock"))
    Column(Modifier.padding(horizontal = 16.dp)) {
        LockSetup(fos, chosen) { chosen = it }
    }
}

@Composable
private fun LangPage() {
    val fos = LocalFos.current
    val l = lang()
    SectionTitle(l("Язык", "Language"))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceRow("Русский", fos.langRu, { fos.update { langRu = true } })
        ChoiceRow("English", !fos.langRu, { fos.update { langRu = false } })
    }
    SwitchRow(
        title = l("24-часовой формат", "24-hour format"),
        checked = fos.use24h,
        onChecked = { fos.update { use24h = it } },
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun AboutPage() {
    val fos = LocalFos.current
    val l = lang()
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }

    // Значения заданы в ТЗ. Подписи: рус / англ
    val rows = listOf(
        l("Номер сборки", "Build number") to FosConfig.BUILD_NUMBER,
        l("Версия ядра", "Kernel version") to FosConfig.KERNEL_VERSION,
        l("Модель", "Model") to "260173FB",
        l("Название телефона", "Phone name") to "fPhone 1.0",
        l("Процессор", "CPU") to "fCpu 1.0 Ultimate",
        l("Графический процессор", "GPU") to "fGpu A-1.0 Turbo",
        l("Оперативная память", "RAM") to l("4 ГБ", "4 GB"),
        l("Встроенная память", "ROM") to l("64 ГБ", "64 GB"),
    )
    rows.forEach { (k, v) -> SettingsRow(title = k, subtitle = v) }

    // Создатель — кликабельно, открывает Telegram
    SettingsRow(
        title = l("Создатель", "Creator"),
        subtitle = "fworld_team",
        icon = R.drawable.ic_send,
        onClick = {
            runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FosConfig.CREATOR_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        },
    )

    SectionTitle(l("Система", "System"))
    SettingsRow(
        title = l("Перезагрузить", "Restart"),
        icon = R.drawable.ic_power,
        onClick = { fos.reboot() },
    )
    SettingsRow(
        title = l("Сброс до заводских", "Factory reset"),
        subtitle = l("Удалит все настройки и вернёт первоначальную настройку", "Erases all settings and restarts setup"),
        icon = R.drawable.ic_close,
        iconBg = MaterialTheme.colorScheme.errorContainer,
        iconTint = MaterialTheme.colorScheme.onErrorContainer,
        onClick = { confirmReset = true },
    )

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(l("Сбросить устройство?", "Reset device?")) },
            text = { Text(l("Все данные fOS будут удалены.", "All fOS data will be erased."), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; fos.factoryReset() }) { Text(l("Сбросить", "Reset")) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(l("Отмена", "Cancel")) } },
        )
    }
}
