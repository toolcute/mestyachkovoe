package com.cutetool.mestyachkovoe

import android.app.Application
import com.cutetool.mestyachkovoe.notifications.NotificationScheduler

class MestyachkovoeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.createChannel(this)
    }
}
