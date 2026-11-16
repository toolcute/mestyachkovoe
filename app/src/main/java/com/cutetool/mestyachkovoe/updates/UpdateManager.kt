package com.cutetool.mestyachkovoe.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val API_URL = "https://api.github.com/repos/toolcute/mestyachkovoe/releases/latest"
    private const val USER_AGENT = "Mestyachkovoe"

    data class UpdateInfo(
        val tag: String,
        val version: String,
        val apkUrl: String,
        val releaseUrl: String,
        val notes: String,
    )

    fun currentVersion(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.09"
        }.getOrDefault("1.09")

    suspend fun latestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return@withContext null
            if (code !in 200..299) throw IOException("GitHub API вернул код $code")

            val raw = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(raw)
            val tag = json.optString("tag_name").trim()
            if (tag.isBlank()) return@withContext null

            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        if (apkUrl.isNotBlank()) break
                    }
                }
            }
            if (apkUrl.isBlank()) return@withContext null

            UpdateInfo(
                tag = tag,
                version = normalizeVersion(tag),
                apkUrl = apkUrl,
                releaseUrl = json.optString("html_url"),
                notes = json.optString("body"),
            )
        } finally {
            connection.disconnect()
        }
    }

    fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = versionParts(remote)
        val currentParts = versionParts(current)
        if (remoteParts.isEmpty() || currentParts.isEmpty()) {
            return normalizeVersion(remote) != normalizeVersion(current)
        }
        val size = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until size) {
            val remotePart = remoteParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (remotePart != currentPart) return remotePart > currentPart
        }
        return false
    }

    suspend fun downloadApk(context: Context, info: UpdateInfo): File = withContext(Dispatchers.IO) {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val dir = File(baseDir, "updates").apply { mkdirs() }
        val target = File(dir, "mestyachkovoe-${info.tag.ifBlank { info.version }}.apk")
        if (target.exists() && target.length() > 0L) return@withContext target

        val temp = File(dir, "${target.name}.part")
        val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/octet-stream")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Не удалось скачать APK (код $code)")
            connection.inputStream.use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            }
            if (temp.length() == 0L) throw IOException("Скачанный APK пустой")
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            target
        } finally {
            connection.disconnect()
        }
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun install(context: Context, file: File): Boolean {
        if (!canInstall(context)) {
            openInstallPermissionSettings(context)
            return false
        }
        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun normalizeVersion(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V")

    private fun versionParts(raw: String): List<Int> =
        Regex("\\d+").findAll(normalizeVersion(raw)).map { it.value.toIntOrNull() ?: 0 }.toList()
}
