package com.cutetool.mestyachkovoe.cache

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CacheCleanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runCatching { CacheManager.clear(applicationContext) }
        return Result.success()
    }
}
