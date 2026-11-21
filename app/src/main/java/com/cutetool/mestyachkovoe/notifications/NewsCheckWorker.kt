package com.cutetool.mestyachkovoe.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cutetool.mestyachkovoe.MainActivity
import com.cutetool.mestyachkovoe.R
import com.cutetool.mestyachkovoe.data.FederalDistrict
import com.cutetool.mestyachkovoe.data.NewsItem
import com.cutetool.mestyachkovoe.data.NewsRepository
import com.cutetool.mestyachkovoe.data.NotifyImportance
import com.cutetool.mestyachkovoe.data.Region
import com.cutetool.mestyachkovoe.data.SettingsRepository
import com.cutetool.mestyachkovoe.data.district
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Locale

class NewsCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val settings = SettingsRepository(ctx).settings.first()
        if (!settings.notifyEnabled) return Result.success()

        NotificationScheduler.createChannel(ctx)

        val items = runCatching {
            NewsRepository().load(settings.notifyRegion, settings)
        }.getOrElse { return Result.retry() }

        val prefs = ctx.getSharedPreferences("notify_state", Context.MODE_PRIVATE)
        val lastTs = prefs.getLong("last_ts", 0L)
        if (lastTs == 0L) {
            prefs.edit().putLong("last_ts", System.currentTimeMillis()).apply()
            return Result.success()
        }

        val importantOnly = settings.notifyFrequency.importantOnly
        val importance = if (importantOnly) NotifyImportance.MAIN else settings.notifyImportance
        val seen = prefs.getStringSet("seen_ids", emptySet()).orEmpty().toMutableSet()
        val candidates = items
            .filter { it.published > lastTs && it.id !in seen }
            .filter { matchesImportance(it, importance) }
            .sortedWith(compareByDescending<NewsItem> { priority(it) }.thenByDescending { it.published })

        val newest = maxOf(lastTs, items.maxOfOrNull { it.published } ?: lastTs)
        val dailyLimit = if (importantOnly) {
            (1440L / settings.notifyFrequency.intervalMinutes).toInt().coerceAtLeast(1)
        } else {
            Int.MAX_VALUE
        }
        val today = LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong("notify_day", 0L)
        val sentToday = if (storedDay == today) prefs.getInt("notify_count", 0) else 0
        val remainingToday = (dailyLimit - sentToday).coerceAtLeast(0)
        val perRunLimit = if (importantOnly) 1 else 4
        val toShow = candidates.take(minOf(perRunLimit, remainingToday))

        if (toShow.isNotEmpty()) {
            toShow.forEach { show(ctx, it) }
            val trimmed = (toShow.map { it.id } + seen).distinct().take(500).toSet()
            val newLastTs = if (importantOnly) {
                maxOf(lastTs, toShow.maxOf { it.published })
            } else {
                newest
            }
            prefs.edit()
                .putStringSet("seen_ids", trimmed)
                .putLong("last_ts", newLastTs)
                .putLong("notify_day", today)
                .putInt("notify_count", sentToday + toShow.size)
                .apply()
        } else {
            prefs.edit().putLong("last_ts", newest).apply()
        }

        return Result.success()
    }

    private fun priority(item: NewsItem): Int {
        val mestechko = item.source.equals("Местечко", ignoreCase = true)
        val ural = item.region == Region.CHELYABINSK || item.region.district == FederalDistrict.URAL
        return when {
            mestechko && ural -> 2
            mestechko || ural -> 1
            else -> 0
        }
    }

    private fun matchesImportance(item: NewsItem, importance: NotifyImportance): Boolean {
        if (importance == NotifyImportance.ALL) return true
        val text = (item.title + " " + item.content).lowercase(Locale.ROOT)
        return when (importance) {
            NotifyImportance.EMERGENCY -> emergencyWords.any { text.contains(it) }
            NotifyImportance.MAIN ->
                emergencyWords.any { text.contains(it) } || importantWords.any { text.contains(it) }
            NotifyImportance.ALL -> true
        }
    }

    @SuppressLint("MissingPermission")
    private fun show(context: Context, item: NewsItem) {
        if (!canNotify(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = item.content.ifBlank { item.title }.take(400)
        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_news)
            .setContentTitle(item.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSubText("${item.source} • ${item.region.title}")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setGroup("mestyachkovoe_news")
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
        }
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private companion object {
        val emergencyWords = listOf(
            "чс", "чрезвычайн", "пожар", "авари", "погиб", "дтп",
            "эвакуац", "шторм", "паводок", "наводнен", "взрыв", "обрушен"
        )

        val importantWords = listOf(
            "важн", "власт", "губернатор", "мэр", "закон", "суд", "следств",
            "прокуратур", "экономик", "бюджет", "тариф", "цен", "зарплат",
            "пенси", "льгот", "дорог", "транспорт", "школ", "больниц",
            "медицин", "жкх", "отключен", "мороз", "ураган"
        )
    }
}
