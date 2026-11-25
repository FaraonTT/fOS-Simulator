package com.fworld.fos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.fworld.fos.ui.Desktop
import com.fworld.fos.ui.FosTheme
import com.fworld.fos.ui.FworldBoot
import com.fworld.fos.ui.LockScreen
import com.fworld.fos.ui.OsBoot
import com.fworld.fos.ui.SetupWizard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val fos = remember { FosState(applicationContext) }
            CompositionLocalProvider(
                LocalFos provides fos,
                LocalLang provides fos.lang,
            ) {
                FosTheme(themeMode = fos.themeMode) {
                    // Яркость «экрана» симулятора
                    val dim = 1f - (0.25f * (1f - fos.brightness)).coerceIn(0f, 0.25f)
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        Crossfade(
                            targetState = fos.phase,
                            animationSpec = tween(450),
                            label = "phase",
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = dim },
                        ) { phase ->
                            when (phase) {
                                Phase.FWORLD_BOOT -> FworldBoot { fos.phase = Phase.OS_BOOT }
                                Phase.OS_BOOT -> OsBoot(
                                    durationMs = fos.bootMs,
                                    firstBoot = !fos.setupDone,
                                    onDone = { fos.afterBoot() },
                                )
                                Phase.SETUP -> SetupWizard()
                                Phase.LOCK -> LockScreen()
                                Phase.DESKTOP -> Desktop()
                            }
                        }
                    }
                }
            }
        }
    }
}
