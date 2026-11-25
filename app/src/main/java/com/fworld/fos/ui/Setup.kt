package com.fworld.fos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fworld.fos.FosState
import com.fworld.fos.LocalFos
import com.fworld.fos.LockType
import com.fworld.fos.R
import com.fworld.fos.WallpaperColors
import com.fworld.fos.lang
import com.fworld.fos.wallpaperBrush
import kotlinx.coroutines.delay
import java.time.ZoneId

private const val STEPS = 10

val FakeNetworks = listOf("fWorld-Home", "fOS-Guest", "Keenetic-7A2F", "TP-Link_5G", "Open-Cafe")

private val Regions = listOf(
    Triple("RU", "Россия", "Russia"),
    Triple("BY", "Беларусь", "Belarus"),
    Triple("KZ", "Казахстан", "Kazakhstan"),
    Triple("UA", "Украина", "Ukraine"),
    Triple("US", "США", "United States"),
    Triple("DE", "Германия", "Germany"),
)

/** Полная первоначальная настройка fOS */
@Composable
fun SetupWizard() {
    val fos = LocalFos.current
    val l = lang()
    var step by remember { mutableIntStateOf(0) }
    var termsOk by remember { mutableStateOf(false) }
    var chosenLock by remember { mutableStateOf(LockType.NONE) }

    val canNext = when (step) {
        6 -> chosenLock == LockType.NONE || (fos.lockType == chosenLock && fos.hasLock)
        8 -> termsOk
        else -> true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Прогресс
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(STEPS) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (i <= step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                }
            },
            label = "setupStep",
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                when (s) {
                    0 -> StepWelcome()
                    1 -> StepLanguage(fos)
                    2 -> StepRegion(fos)
                    3 -> StepWifi(fos)
                    4 -> StepDateTime(fos)
                    5 -> StepAccount(fos)
                    6 -> StepSecurity(fos, chosenLock) { chosenLock = it }
                    7 -> StepAppearance(fos)
                    8 -> StepTerms(termsOk) { termsOk = it }
                    else -> StepFinish()
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                OutlinedButton(onClick = { step-- }) { Text(l("Назад", "Back")) }
            } else Spacer(Modifier.size(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step == 3 || step == 5) {
                    TextButton(onClick = { step++ }) { Text(l("Пропустить", "Skip")) }
                }
                Button(
                    enabled = canNext,
                    onClick = { if (step == STEPS - 1) fos.finishSetup() else step++ },
                ) { Text(if (step == STEPS - 1) l("Начать", "Start") else l("Далее", "Next")) }
            }
        }
    }
}

@Composable
private fun StepHeader(icon: Int, title: String, subtitle: String? = null) {
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier.size(64.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) { FosIcon(icon, MaterialTheme.colorScheme.onPrimaryContainer, 34.dp) }
    Text(
        title,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 18.dp),
    )
    if (subtitle != null) {
        Text(
            subtitle,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
        )
    } else Spacer(Modifier.height(18.dp))
}

@Composable
private fun StepWelcome() {
    val l = lang()
    Spacer(Modifier.height(60.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(120.dp).clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { FosIcon(R.drawable.ic_logo, MaterialTheme.colorScheme.onPrimaryContainer, 72.dp) }
    }
    Text(
        l("Добро пожаловать в fOS", "Welcome to fOS"),
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
    )
    Text(
        l(
            "Давайте настроим ваше устройство. Это займёт пару минут.",
            "Let's set up your device. It will only take a couple of minutes.",
        ),
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}

@Composable
private fun StepLanguage(fos: FosState) {
    val l = lang()
    StepHeader(R.drawable.ic_language, l("Язык", "Language"), l("Выберите язык системы", "Choose the system language"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceRow("Русский", fos.langRu, { fos.update { langRu = true } }, subtitle = "Russian")
        ChoiceRow("English", !fos.langRu, { fos.update { langRu = false } }, subtitle = "Английский")
    }
}

@Composable
private fun StepRegion(fos: FosState) {
    val l = lang()
    StepHeader(R.drawable.ic_location, l("Регион", "Region"), l("Влияет на формат даты и сервисы", "Affects date format and services"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Regions.forEach { (code, ru, en) ->
            ChoiceRow(l(ru, en), fos.region == code, { fos.update { region = code } })
        }
    }
}

@Composable
private fun StepWifi(fos: FosState) {
    val l = lang()
    StepHeader(R.drawable.ic_wifi, l("Wi-Fi", "Wi-Fi"), l("Подключитесь к сети или пропустите", "Connect to a network or skip"))
    WifiList(fos)
}

/** Список Wi-Fi сетей (используется в мастере и в настройках) */
@Composable
fun WifiList(fos: FosState) {
    val l = lang()
    var asking by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf<String?>(null) }
    var pass by remember { mutableStateOf("") }

    LaunchedEffect(connecting) {
        val n = connecting
        if (n != null) {
            delay(1400)
            fos.update { wifiName = n; wifiOn = true }
            connecting = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FakeNetworks.forEach { name ->
            val isOpen = name == "Open-Cafe"
            ChoiceRow(
                title = name,
                selected = fos.wifiName == name,
                icon = R.drawable.ic_wifi,
                subtitle = when {
                    connecting == name -> l("Подключение…", "Connecting…")
                    fos.wifiName == name -> l("Подключено", "Connected")
                    isOpen -> l("Открытая сеть", "Open network")
                    else -> l("Защищена WPA2", "Secured with WPA2")
                },
                onClick = {
                    if (fos.wifiName == name) return@ChoiceRow
                    if (isOpen) connecting = name else { pass = ""; asking = name }
                },
            )
        }
    }

    val ask = asking
    if (ask != null) {
        AlertDialog(
            onDismissRequest = { asking = null },
            title = { Text(ask) },
            text = {
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    singleLine = true,
                    label = { Text(l("Пароль", "Password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = pass.length >= 8,
                    onClick = { connecting = ask; asking = null },
                ) { Text(l("Подключить", "Connect")) }
            },
            dismissButton = { TextButton(onClick = { asking = null }) { Text(l("Отмена", "Cancel")) } },
        )
    }
}

@Composable
private fun StepDateTime(fos: FosState) {
    val l = lang()
    val now by rememberNow()
    StepHeader(R.drawable.ic_info, l("Дата и время", "Date & time"), l("Определяются автоматически", "Set automatically"))
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(now.timeText(fos.use24h), fontSize = 56.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface)
        Text(now.dateText(fos.langRu), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(ZoneId.systemDefault().id, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 6.dp))
    }
    Spacer(Modifier.height(10.dp))
    SwitchRow(
        title = l("24-часовой формат", "24-hour format"),
        checked = fos.use24h,
        onChecked = { fos.update { use24h = it } },
    )
}

@Composable
private fun StepAccount(fos: FosState) {
    val l = lang()
    StepHeader(R.drawable.ic_person, l("Ваше имя", "Your name"), l("Показывается на экране блокировки и в fGram", "Shown on lock screen and in fGram"))
    OutlinedTextField(
        value = fos.userName,
        onValueChange = { fos.update { userName = it.take(24) } },
        singleLine = true,
        label = { Text(l("Имя", "Name")) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StepSecurity(fos: FosState, chosen: LockType, onChoose: (LockType) -> Unit) {
    val l = lang()
    StepHeader(R.drawable.ic_shield, l("Защита устройства", "Device security"), l("Выберите способ блокировки экрана", "Choose a screen lock method"))
    LockSetup(fos, chosen, onChoose)
}

/** Выбор и установка способа блокировки (мастер и настройки) */
@Composable
fun LockSetup(fos: FosState, chosen: LockType, onChoose: (LockType) -> Unit) {
    val l = lang()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceRow(l("Нет", "None"), chosen == LockType.NONE, {
            onChoose(LockType.NONE); fos.setLock(LockType.NONE, "")
        }, subtitle = l("Сразу рабочий стол", "Go straight to the desktop"))
        ChoiceRow("PIN", chosen == LockType.PIN, {
            if (chosen != LockType.PIN) { fos.setLock(LockType.NONE, ""); onChoose(LockType.PIN) }
        }, subtitle = l("Минимум 4 цифры", "At least 4 digits"))
        ChoiceRow(l("Пароль", "Password"), chosen == LockType.PASSWORD, {
            if (chosen != LockType.PASSWORD) { fos.setLock(LockType.NONE, ""); onChoose(LockType.PASSWORD) }
        }, subtitle = l("Минимум 4 символа", "At least 4 characters"))
        ChoiceRow(l("Графический ключ", "Pattern"), chosen == LockType.PATTERN, {
            if (chosen != LockType.PATTERN) { fos.setLock(LockType.NONE, ""); onChoose(LockType.PATTERN) }
        }, subtitle = l("Соедините минимум 4 точки", "Connect at least 4 dots"))
    }
    Spacer(Modifier.height(18.dp))
    if (fos.hasLock && fos.lockType == chosen && chosen != LockType.NONE) {
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.tertiaryContainer).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FosIcon(R.drawable.ic_check, MaterialTheme.colorScheme.onTertiaryContainer, 22.dp)
            Text(l("Защита установлена", "Lock is set"), color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    } else when (chosen) {
        LockType.PIN -> PinSetup(fos)
        LockType.PASSWORD -> PasswordSetup(fos)
        LockType.PATTERN -> PatternSetup(fos)
        LockType.NONE -> {}
    }
}

@Composable
private fun PinSetup(fos: FosState) {
    val l = lang()
    var first by remember { mutableStateOf<String?>(null) }
    var entry by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (mismatch) l("PIN не совпал, начните заново", "PINs don't match, start over")
            else if (first == null) l("Придумайте PIN", "Create a PIN") else l("Повторите PIN", "Confirm PIN"),
            color = if (mismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(14.dp))
        PinDots(entry.length, maxOf(4, entry.length), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(18.dp))
        PinPad(
            value = entry,
            onChange = { entry = it; mismatch = false },
            keyColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            textColor = MaterialTheme.colorScheme.onSurface,
        )
        Button(
            enabled = entry.length >= 4,
            modifier = Modifier.padding(top = 16.dp),
            onClick = {
                val f = first
                if (f == null) {
                    first = entry; entry = ""
                } else if (f == entry) {
                    fos.setLock(LockType.PIN, entry)
                } else {
                    first = null; entry = ""; mismatch = true
                }
            },
        ) { Text(if (first == null) l("Продолжить", "Continue") else l("Подтвердить", "Confirm")) }
    }
}

@Composable
private fun PasswordSetup(fos: FosState) {
    val l = lang()
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = p1, onValueChange = { p1 = it }, singleLine = true,
            label = { Text(l("Пароль", "Password")) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = p2, onValueChange = { p2 = it }, singleLine = true,
            label = { Text(l("Повторите пароль", "Repeat password")) },
            visualTransformation = PasswordVisualTransformation(),
            isError = p2.isNotEmpty() && p1 != p2,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = p1.length >= 4 && p1 == p2,
            onClick = { fos.setLock(LockType.PASSWORD, p1) },
        ) { Text(l("Сохранить", "Save")) }
    }
}

@Composable
private fun PatternSetup(fos: FosState) {
    val l = lang()
    var first by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error) { delay(700); error = false }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            msg ?: if (first == null) l("Нарисуйте ключ", "Draw a pattern") else l("Повторите ключ", "Draw it again"),
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        PatternPad(
            error = error,
            color = MaterialTheme.colorScheme.primary,
            onComplete = { p ->
                val s = p.joinToString("-")
                if (p.size < 4) {
                    msg = l("Минимум 4 точки", "At least 4 dots"); error = true
                } else if (first == null) {
                    first = s; msg = null
                } else if (first == s) {
                    fos.setLock(LockType.PATTERN, s)
                } else {
                    first = null; msg = l("Не совпало, ещё раз", "Didn't match, try again"); error = true
                }
            },
        )
    }
}

@Composable
private fun StepAppearance(fos: FosState) {
    val l = lang()
    StepHeader(R.drawable.ic_wallpaper, l("Оформление", "Appearance"), l("Тема и обои", "Theme and wallpaper"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceRow(l("Тёмная", "Dark"), fos.themeMode == 2, { fos.update { themeMode = 2 } })
        ChoiceRow(l("Светлая", "Light"), fos.themeMode == 1, { fos.update { themeMode = 1 } })
        ChoiceRow(l("Как в системе", "Follow system"), fos.themeMode == 0, { fos.update { themeMode = 0 } })
    }
    Text(l("Обои", "Wallpaper"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
    WallpaperPicker(fos)
}

@Composable
fun WallpaperPicker(fos: FosState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WallpaperColors.indices.forEach { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(wallpaperBrush(i))
                    .then(
                        if (fos.wallpaper == i)
                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                        else Modifier
                    )
                    .clickable { fos.update { wallpaper = i } }
            )
        }
    }
}

@Composable
private fun StepTerms(ok: Boolean, onOk: (Boolean) -> Unit) {
    val l = lang()
    StepHeader(R.drawable.ic_info, l("Условия использования", "Terms of use"))
    Text(
        l(
            "fOS Simulator — это симулятор операционной системы, созданный командой fworld_team. " +
                "Все приложения (fStore, fGram, fTok) и данные внутри симулятора вымышленные и никуда не передаются. " +
                "Продолжая, вы соглашаетесь использовать приложение только в демонстрационных целях.",
            "fOS Simulator is an operating system simulator created by fworld_team. " +
                "All apps (fStore, fGram, fTok) and data inside the simulator are fictional and are never sent anywhere. " +
                "By continuing you agree to use the app for demonstration purposes only.",
        ),
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp).clickable { onOk(!ok) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = ok, onCheckedChange = onOk)
        Text(l("Я принимаю условия", "I accept the terms"), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StepFinish() {
    val l = lang()
    Spacer(Modifier.height(70.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(110.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { FosIcon(R.drawable.ic_check, MaterialTheme.colorScheme.onPrimaryContainer, 64.dp) }
    }
    Text(
        l("Всё готово!", "All set!"),
        fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
    )
    Text(
        l("fOS настроена. Нажмите «Начать», чтобы перейти к устройству.", "fOS is configured. Tap Start to continue."),
        fontSize = 16.sp, textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
}
