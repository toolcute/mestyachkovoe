package com.cutetool.mestyachkovoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.cutetool.mestyachkovoe.data.SettingsRepository
import com.cutetool.mestyachkovoe.data.UserSettings
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
            val settings by settingsRepo.settings.collectAsState(initial = UserSettings())
            LaunchedEffect(settings.notifyEnabled, settings.notifyFrequency) {
                NotificationScheduler.schedule(appContext, settings)
            }
            MestyachkovoeTheme(mode = settings.themeMode) {
                MestyachkovoeApp(settings = settings, settingsRepo = settingsRepo)
            }
        }
    }
}
