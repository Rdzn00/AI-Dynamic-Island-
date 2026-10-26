package com.aiisland.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bus.load(this)
        setContent { App() }
    }

    private fun toggleIsland(v: Boolean) {
        if (!IslandService.running) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            Bus.islandOn.value = v
            Bus.save(this)
        }
    }

    @Composable
    private fun App() {
        var tab by remember { mutableStateOf(0) }
        var picker by remember { mutableStateOf(0) }
        var svc by remember { mutableStateOf(IslandService.running) }
        var notif by remember { mutableStateOf(false) }
        var mic by remember { mutableStateOf(false) }
        val banks by Bus.banks
        val blocked by Bus.blocked
        val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            mic = ok
        }
        LaunchedEffect(Unit) {
            while (true) {
                svc = IslandService.running
                notif = NotificationManagerCompat.getEnabledListenerPackages(this@MainActivity)
                    .contains(packageName)
                mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                delay(1000L)
            }
        }
        LaunchedEffect(Unit) {
            if (!Bus.banksInit.value) {
                val all = withContext(Dispatchers.IO) { loadApps(this@MainActivity, false) }
                Bus.banks.value = all.filter { looksLikeBank(it.label, it.pkg) }.map { it.pkg }.toSet()
                Bus.banksInit.value = true
                Bus.save(this@MainActivity)
            }
        }
        MaterialTheme(colorScheme = darkColorScheme(primary = C.Mint, onPrimary = Color.Black)) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF0B1220), C.Bg)))
                        .systemBarsPadding()
                ) {
                    Box(Modifier.weight(1f)) {
                        Crossfade(targetState = tab, animationSpec = tween(250), label = "tab") { t ->
                            when (t) {
                                0 -> HomeScreen(svc) { v -> toggleIsland(v) }
                                1 -> ChatScreen()
                                2 -> ToolsScreen({ picker = 1 }, { picker = 2 })
                                else -> SettingScreen(
                                    svc,
                                    notif,
                                    mic,
                                    { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                    { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                                    { ask.launch(Manifest.permission.RECORD_AUDIO) },
                                    {
                                        startActivity(
                                            Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.parse("package:$packageName")
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                    TabBar(tab) { tab = it }
                }
                if (picker == 1) {
                    AppPicker(
                        L.t("picker_banks"),
                        banks,
                        { s ->
                            Bus.banks.value = s
                            Bus.save(this@MainActivity)
                        },
                        { picker = 0 }
                    )
                }
                if (picker == 2) {
                    AppPicker(
                        L.t("picker_block"),
                        blocked,
                        { s ->
                            Bus.blocked.value = s
                            Bus.save(this@MainActivity)
                        },
                        { picker = 0 }
                    )
                }
            }
        }
    }
}
