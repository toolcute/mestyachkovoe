package com.cutetool.mestyachkovoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cutetool.mestyachkovoe.data.SettingsRepository
import com.cutetool.mestyachkovoe.data.ThemeMode
import com.cutetool.mestyachkovoe.notifications.NotificationScheduler
import com.cutetool.mestyachkovoe.ui.MestyachkovoeApp
import com.cutetool.mestyachkovoe.ui.theme.MestyachkovoeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContext = applicationContext
        val settingsRepo = SettingsRepository(appContext)
        setContent {
            val loaded by settingsRepo.settings.collectAsState(initial = null)
            val settings = loaded
            if (settings == null) {
                MestyachkovoeTheme(mode = ThemeMode.MONET) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                LaunchedEffect(settings.notifyEnabled, settings.notifyFrequency) {
                    NotificationScheduler.schedule(appContext, settings)
                }
                MestyachkovoeTheme(mode = settings.themeMode) {
                    MestyachkovoeApp(settings = settings, settingsRepo = settingsRepo)
                }
            }
        }
    }
}
