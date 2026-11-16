package com.cutetool.mestyachkovoe.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cutetool.mestyachkovoe.data.UserSettings
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val CHANNEL_ID = "mestyachkovoe_news"
    const val WORK_NAME = "mestyachkovoe_news_check"
    private const val WORK_NOW = "mestyachkovoe_news_check_now"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Новости",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Новости выбранного региона"
        }
        manager.createNotificationChannel(channel)
    }

    fun schedule(context: Context, settings: UserSettings) {
        val wm = WorkManager.getInstance(context)
        if (!settings.notifyEnabled) {
            wm.cancelUniqueWork(WORK_NAME)
            wm.cancelUniqueWork(WORK_NOW)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodic = PeriodicWorkRequestBuilder<NewsCheckWorker>(
            settings.notifyFrequency.intervalMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic)

        val now = OneTimeWorkRequestBuilder<NewsCheckWorker>()
            .setConstraints(constraints)
            .build()
        wm.enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE, now)
    }
}
