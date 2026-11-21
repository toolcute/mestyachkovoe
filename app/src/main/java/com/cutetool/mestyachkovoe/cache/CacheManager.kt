package com.cutetool.mestyachkovoe.cache

import android.content.Context
import coil.imageLoader
import com.cutetool.mestyachkovoe.data.NewsRepository
import java.io.File
import java.util.Locale

@OptIn(coil.annotation.ExperimentalCoilApi::class)
object CacheManager {
    fun size(context: Context): Long =
        directorySize(context.cacheDir) + directorySize(context.externalCacheDir)

    fun clear(context: Context): Long {
        val before = size(context)
        NewsRepository.clearCache()
        runCatching { context.imageLoader.memoryCache?.clear() }
        runCatching { context.imageLoader.diskCache?.clear() }
        clearDirectory(context.cacheDir)
        clearDirectory(context.externalCacheDir)
        return before
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 МБ"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f МБ", mb)
        } else {
            String.format(Locale.getDefault(), "%.0f КБ", bytes / 1024.0)
        }
    }

    private fun directorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun clearDirectory(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
    }
}
