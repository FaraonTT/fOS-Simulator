package com.fworld.fos

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.security.MessageDigest

object FosConfig {
    /** Bootanimation "fworld" */
    const val FWORLD_BOOT_MS = 7_000L

    /** Первая загрузка fOS (до настройки) */
    const val FIRST_BOOT_MS = 30_000L

    /** Загрузка уже настроенной ОС */
    const val NORMAL_BOOT_MS = 10_000L

    const val CREATOR_URL = "https://t.me/fworld_team"
}

/** Двуязычные строки: l("Русский", "English") */
class Lang(val ru: Boolean) {
    operator fun invoke(r: String, e: String): String = if (ru) r else e
}

val LocalLang = staticCompositionLocalOf { Lang(true) }
val LocalFos = staticCompositionLocalOf<FosState> { error("FosState not provided") }

enum class LockType { NONE, PIN, PASSWORD, PATTERN }
enum class Phase { FWORLD_BOOT, OS_BOOT, SETUP, LOCK, DESKTOP }

data class FosNotif(
    val id: Long,
    val app: String,
    val title: String,
    val text: String,
    val time: Long,
)

val WallpaperColors: List<List<Color>> = listOf(
    listOf(Color(0xFF1B1464), Color(0xFF6A3DE8), Color(0xFFE05297)),
    listOf(Color(0xFF0F2027), Color(0xFF2C5364), Color(0xFF3FB6A8)),
    listOf(Color(0xFF200122), Color(0xFF6F0000), Color(0xFFFF8A4C)),
    listOf(Color(0xFF0B1B3A), Color(0xFF1E5AA8), Color(0xFF7FD6FF)),
    listOf(Color(0xFF111111), Color(0xFF2B2B3A), Color(0xFF5C5C8A)),
)

fun wallpaperBrush(index: Int): Brush =
    Brush.linearGradient(WallpaperColors[index.mod(WallpaperColors.size)])

fun sha256(s: String): String =
    MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

class FosState(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("fos", Context.MODE_PRIVATE)

    var phase by mutableStateOf(Phase.FWORLD_BOOT)

    var setupDone by mutableStateOf(sp.getBoolean("setupDone", false))
    var langRu by mutableStateOf(sp.getBoolean("langRu", true))
    var userName by mutableStateOf(sp.getString("userName", "") ?: "")
    var region by mutableStateOf(sp.getString("region", "RU") ?: "RU")
    var use24h by mutableStateOf(sp.getBoolean("use24h", true))

    /** 0 — авто, 1 — светлая, 2 — тёмная */
    var themeMode by mutableStateOf(sp.getInt("themeMode", 2))
    var wallpaper by mutableStateOf(sp.getInt("wallpaper", 0))

    var lockType by mutableStateOf(
        runCatching { LockType.valueOf(sp.getString("lockType", "NONE") ?: "NONE") }.getOrDefault(LockType.NONE)
    )
    var lockHash by mutableStateOf(sp.getString("lockHash", "") ?: "")
    var pinLen by mutableStateOf(sp.getInt("pinLen", 4))

    var wifiOn by mutableStateOf(sp.getBoolean("wifiOn", true))
    var wifiName by mutableStateOf(sp.getString("wifiName", "") ?: "")
    var bluetoothOn by mutableStateOf(sp.getBoolean("bluetoothOn", false))
    var airplane by mutableStateOf(false)
    var flashlight by mutableStateOf(false)
    var dnd by mutableStateOf(false)
    var autoRotate by mutableStateOf(false)
    var locationOn by mutableStateOf(sp.getBoolean("locationOn", true))
    var batterySaver by mutableStateOf(false)
    var brightness by mutableStateOf(sp.getFloat("brightness", 0.8f))
    var volume by mutableStateOf(sp.getFloat("volume", 0.6f))

    var installed by mutableStateOf(
        (sp.getString("installed", "fgram,ftok") ?: "").split(",").filter { it.isNotBlank() }.toSet()
    )

    val notifs = mutableStateListOf<FosNotif>()

    val lang: Lang get() = Lang(langRu)
    val hasLock: Boolean get() = lockType != LockType.NONE && lockHash.isNotEmpty()
    val bootMs: Long get() = if (setupDone) FosConfig.NORMAL_BOOT_MS else FosConfig.FIRST_BOOT_MS

    fun update(block: FosState.() -> Unit) {
        block()
        persist()
    }

    private fun persist() {
        sp.edit()
            .putBoolean("setupDone", setupDone)
            .putBoolean("langRu", langRu)
            .putString("userName", userName)
            .putString("region", region)
            .putBoolean("use24h", use24h)
            .putInt("themeMode", themeMode)
            .putInt("wallpaper", wallpaper)
            .putString("lockType", lockType.name)
            .putString("lockHash", lockHash)
            .putInt("pinLen", pinLen)
            .putBoolean("wifiOn", wifiOn)
            .putString("wifiName", wifiName)
            .putBoolean("bluetoothOn", bluetoothOn)
            .putBoolean("locationOn", locationOn)
            .putFloat("brightness", brightness)
            .putFloat("volume", volume)
            .putString("installed", installed.joinToString(","))
            .apply()
    }

    fun setLock(type: LockType, secret: String) {
        update {
            lockType = type
            lockHash = if (type == LockType.NONE) "" else sha256(secret)
            if (type == LockType.PIN) pinLen = secret.length
        }
    }

    fun checkLock(secret: String): Boolean = hasLock && sha256(secret) == lockHash

    fun postNotif(app: String, title: String, text: String) {
        if (dnd) return
        notifs.add(0, FosNotif(System.nanoTime(), app, title, text, System.currentTimeMillis()))
    }

    fun seedNotifs() {
        if (notifs.isNotEmpty()) return
        val l = lang
        postNotif("fOS", l("Добро пожаловать в fOS", "Welcome to fOS"), l("Система готова к работе", "System is ready"))
        postNotif("fStore", l("Обновления", "Updates"), l("Доступны новые версии fGram и fTok", "New versions of fGram and fTok are available"))
    }

    /** После окончания загрузки fOS */
    fun afterBoot() {
        phase = when {
            !setupDone -> Phase.SETUP
            hasLock -> Phase.LOCK
            else -> Phase.DESKTOP
        }
    }

    fun finishSetup() {
        update { setupDone = true }
        seedNotifs()
        phase = if (hasLock) Phase.LOCK else Phase.DESKTOP
    }

    fun reboot() {
        notifs.clear()
        phase = Phase.FWORLD_BOOT
    }

    fun factoryReset() {
        sp.edit().clear().apply()
        setupDone = false
        langRu = true
        userName = ""
        region = "RU"
        use24h = true
        themeMode = 2
        wallpaper = 0
        lockType = LockType.NONE
        lockHash = ""
        pinLen = 4
        wifiOn = true
        wifiName = ""
        bluetoothOn = false
        locationOn = true
        brightness = 0.8f
        volume = 0.6f
        installed = setOf("fgram", "ftok")
        reboot()
    }
}

@Composable
fun lang(): Lang = LocalLang.current
