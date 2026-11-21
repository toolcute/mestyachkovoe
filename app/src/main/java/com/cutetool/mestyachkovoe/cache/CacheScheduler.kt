package com.cutetool.mestyachkovoe.cache

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cutetool.mestyachkovoe.data.CacheAutoClean
import java.util.concurrent.TimeUnit

object CacheScheduler {
    private const val WORK_NAME = "cache_auto_clean"

    fun schedule(context: Context, mode: CacheAutoClean) {
        val workManager = WorkManager.getInstance(context)
        val hours = mode.intervalHours
        if (hours == null) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<CacheCleanWorker>(hours, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
