package com.cutetool.mestyachkovoe.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cutetool.mestyachkovoe.updates.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private sealed interface VersionStatus {
    data object Idle : VersionStatus
    data object Checking : VersionStatus
    data object UpToDate : VersionStatus
    data class Available(val info: UpdateManager.UpdateInfo) : VersionStatus
    data class Downloading(val info: UpdateManager.UpdateInfo, val progress: Float) : VersionStatus
    data class NeedsPermission(val info: UpdateManager.UpdateInfo, val file: File?) : VersionStatus
    data class InstallerOpened(val info: UpdateManager.UpdateInfo) : VersionStatus
    data class Error(val message: String) : VersionStatus
}

private val VersionStatus.infoOrNull: UpdateManager.UpdateInfo?
    get() = when (this) {
        is VersionStatus.Available -> info
        is VersionStatus.Downloading -> info
        is VersionStatus.NeedsPermission -> info
        is VersionStatus.InstallerOpened -> info
        else -> null
    }

private val HeroShape = RoundedCornerShape(
    topStart = 34.dp,
    topEnd = 10.dp,
    bottomEnd = 34.dp,
    bottomStart = 10.dp
)

private val AccentShape = RoundedCornerShape(
    topStart = 8.dp,
    topEnd = 24.dp,
    bottomEnd = 8.dp,
    bottomStart = 24.dp
)

private val ChangelogShape = RoundedCornerShape(
    topStart = 22.dp,
    topEnd = 6.dp,
    bottomEnd = 22.dp,
    bottomStart = 6.dp
)

@Composable
fun VersionDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember { UpdateManager.currentVersion(context) }
    var status by remember { mutableStateOf<VersionStatus>(VersionStatus.Idle) }
    var releases by remember { mutableStateOf<List<UpdateManager.ReleaseNote>>(emptyList()) }
    var changelogLoading by remember { mutableStateOf(true) }
    var changelogError by remember { mutableStateOf<String?>(null) }
    var latestInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var downloadId by remember { mutableStateOf<Long?>(null) }
    var downloadInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    val busy = status is VersionStatus.Checking || status is VersionStatus.Downloading

    fun loadChangelog() {
        scope.launch {
            changelogLoading = true
            changelogError = null
            releases = try {
                UpdateManager.recentReleases()
            } catch (e: Exception) {
                changelogError = e.message ?: "Не удалось загрузить ченжлог"
                emptyList()
            }
            changelogLoading = false
        }
    }

    fun openInstaller(info: UpdateManager.UpdateInfo, file: File) {
        status = if (!UpdateManager.canInstall(context)) {
            VersionStatus.NeedsPermission(info, file)
        } else if (UpdateManager.install(context, file)) {
            VersionStatus.InstallerOpened(info)
        } else {
            VersionStatus.NeedsPermission(info, file)
        }
    }

    fun checkForUpdates() {
        scope.launch {
            status = VersionStatus.Checking
            try {
                val info = UpdateManager.latestRelease()
                latestInfo = info
                status = when {
                    info == null -> VersionStatus.Error("В релизах не найден APK-файл.")
                    !UpdateManager.isNewer(info.version, currentVersion) -> VersionStatus.UpToDate
                    else -> VersionStatus.Available(info)
                }
            } catch (e: Exception) {
                latestInfo = null
                status = VersionStatus.Error(e.message ?: "Не удалось проверить обновления")
            }
        }
    }

    fun startDownload(info: UpdateManager.UpdateInfo) {
        if (status is VersionStatus.Downloading) return
        scope.launch {
            try {
                val existing = UpdateManager.downloadedApkFile(context, info)
                if (existing.exists() && existing.length() > 0L) {
                    status = VersionStatus.Downloading(info, 1f)
                    openInstaller(info, existing)
                    return@launch
                }
                downloadInfo = info
                val id = UpdateManager.enqueueApkDownload(context, info)
                downloadId = id
                status = VersionStatus.Downloading(info, 0f)
            } catch (e: Exception) {
                status = VersionStatus.Error(e.message ?: "Не удалось начать загрузку APK")
            }
        }
    }

    LaunchedEffect(Unit) {
        loadChangelog()
        checkForUpdates()
    }

    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        val info = downloadInfo ?: return@LaunchedEffect
        while (isActive) {
            delay(500)
            val progress = UpdateManager.downloadProgress(context, id)
            status = VersionStatus.Downloading(info, progress.fraction)
            when (progress.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val file = UpdateManager.downloadedApkFile(context, info)
                    if (file.exists() && file.length() > 0L) {
                        openInstaller(info, file)
                    } else {
                        status = VersionStatus.Error("APK скачан, но файл не найден.")
                    }
                    downloadId = null
                    return@LaunchedEffect
                }
                DownloadManager.STATUS_FAILED -> {
                    status = VersionStatus.Error("Загрузка APK не удалась (код ${progress.reason}).")
                    downloadId = null
                    return@LaunchedEffect
                }
            }
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 36.dp,
                topEnd = 12.dp,
                bottomEnd = 36.dp,
                bottomStart = 16.dp
            ),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = HeroShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Местячковое",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Версия $currentVersion",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        onClick = onClose,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Surface(shape = AccentShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Обновления из GitHub Releases",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "APK скачивается системным загрузчиком",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        IconButton(onClick = { loadChangelog() }, enabled = !changelogLoading) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Обновить ченжлог",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                latestInfo?.let { info ->
                    val displayStatus = status.infoOrNull?.let { status }
                        ?: VersionStatus.Available(info)
                    UpdateActionCard(
                        status = displayStatus,
                        info = info,
                        isNewer = UpdateManager.isNewer(info.version, currentVersion),
                        onDownload = { startDownload(info) },
                        onInstall = { file -> openInstaller(info, file) },
                        onOpenSettings = { UpdateManager.openInstallPermissionSettings(context) },
                        onOpenBrowser = {
                            openExternalBrowser(
                                context,
                                info.releaseUrl.ifBlank { info.apkUrl }
                            )
                        }
                    )
                }

                StatusBlock(
                    status = status,
                    onOpenSettings = { UpdateManager.openInstallPermissionSettings(context) }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Что нового",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { openExternalBrowser(context, UpdateManager.RELEASES_URL) }) {
                        Text("Все релизы")
                    }
                }

                when {
                    changelogLoading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExpressiveLoadingIndicator(size = 20.dp)
                        Text("Загружаем ченжлог…", style = MaterialTheme.typography.bodyMedium)
                    }

                    changelogError != null -> Surface(
                        shape = ChangelogShape,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            changelogError ?: "",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    releases.isEmpty() -> Text(
                        "Релизы пока не найдены.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    else -> releases.take(4).forEachIndexed { index, release ->
                        ReleaseCard(release = release, latest = index == 0)
                    }
                }

                Button(
                    onClick = { checkForUpdates() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Проверить обновления")
                }
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
private fun UpdateActionCard(
    status: VersionStatus,
    info: UpdateManager.UpdateInfo,
    isNewer: Boolean,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBrowser: () -> Unit,
) {
    val downloading = status is VersionStatus.Downloading
    val progress = (status as? VersionStatus.Downloading)?.progress ?: 0f
    val needsPermission = status is VersionStatus.NeedsPermission

    Surface(
        shape = ChangelogShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isNewer) "Доступна версия ${info.version}" else "Версия ${info.version}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (isNewer) {
                            "Скачайте APK и установите поверх текущей версии"
                        } else {
                            "Последний релиз. Можно скачать и переустановить."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (status is VersionStatus.Downloading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Загрузка: ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (needsPermission) {
                Text(
                    "Нужно разрешить установку приложений из этого источника.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                        Text("Разрешить")
                    }
                    val file = (status as? VersionStatus.NeedsPermission)?.file
                    if (file != null) {
                        Button(onClick = { onInstall(file) }, modifier = Modifier.weight(1f)) {
                            Text("Установить")
                        }
                    }
                }
            }

            Button(
                onClick = onDownload,
                enabled = !downloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        downloading -> "Загрузка обновления…"
                        status is VersionStatus.InstallerOpened -> "Открыть установщик снова"
                        else -> "Скачать и установить обновление"
                    }
                )
            }
            OutlinedButton(onClick = onOpenBrowser, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Открыть в браузере")
            }
        }
    }
}

@Composable
private fun StatusBlock(
    status: VersionStatus,
    onOpenSettings: () -> Unit,
) {
    Surface(
        shape = ChangelogShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (status) {
                VersionStatus.Idle -> Text(
                    "Нажмите «Проверить обновления», чтобы узнать, есть ли новая версия.",
                    style = MaterialTheme.typography.bodyMedium
                )
                VersionStatus.Checking -> StatusRow("Проверка обновлений…")
                is VersionStatus.Downloading -> StatusRow("Загрузка APK… ${(status.progress * 100).toInt()}%")
                VersionStatus.UpToDate -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Установлена актуальная версия.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                is VersionStatus.Available -> Text(
                    "Найдено обновление ${status.info.version}. Нажмите кнопку выше, чтобы скачать и установить APK.",
                    style = MaterialTheme.typography.bodyMedium
                )
                is VersionStatus.InstallerOpened -> Text(
                    "Установщик открыт. Подтвердите обновление в системе Android.",
                    style = MaterialTheme.typography.bodyMedium
                )
                is VersionStatus.NeedsPermission -> {
                    Text(
                        "Для установки обновления разрешите установку из этого источника.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Доступная версия: ${status.info.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Открыть настройки")
                    }
                }
                is VersionStatus.Error -> Text(
                    status.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: UpdateManager.ReleaseNote, latest: Boolean) {
    Surface(
        shape = ChangelogShape,
        color = if (latest) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        release.tag,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (latest) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            "последний",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    formatReleaseDate(release.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (release.name.isNotBlank() && release.name != release.tag) {
                Text(
                    release.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (release.notes.isBlank()) {
                Text(
                    "Описание релиза не указано.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MarkdownChangelog(release.notes.take(2000))
            }
        }
    }
}

@Composable
private fun MarkdownChangelog(markdown: String) {
    val lines = markdown.lines()
        .map { it.trimEnd() }
        .filter { it.isNotBlank() }
        .filterNot { it.trim() in setOf("---", "***", "___") }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("###") -> Text(
                    inlineMarkdown(line.removePrefix("###").trim()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                line.startsWith("##") -> Text(
                    inlineMarkdown(line.removePrefix("##").trim()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                line.startsWith("#") -> Text(
                    inlineMarkdown(line.removePrefix("#").trim()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                line.startsWith("- [x]") || line.startsWith("- [X]") -> BulletLine(
                    text = "✓ " + line.removePrefix("- [x]").removePrefix("- [X]").trim(),
                    checked = true
                )
                line.startsWith("- [ ]") -> BulletLine(
                    text = "○ " + line.removePrefix("- [ ]").trim(),
                    checked = false
                )
                line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") ->
                    BulletLine(text = line.drop(2).trim(), checked = false)
                line matches Regex("^\\d+[.)]\\s+.*") -> BulletLine(text = line, checked = false)
                line.startsWith(">") -> Text(
                    inlineMarkdown(line.removePrefix(">").trim()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Text(
                    inlineMarkdown(line),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BulletLine(text: String, checked: Boolean) {
    Row {
        Text(
            if (checked) "✓" else "•",
            style = MaterialTheme.typography.bodySmall,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            inlineMarkdown(text),
            style = MaterialTheme.typography.bodySmall,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun inlineMarkdown(text: String): AnnotatedString {
    val cleaned = text
        .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
        .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("<[^>]+>"), "")
        .replace("`", "")
    return buildAnnotatedString {
        val bold = Regex("(\\*\\*|__)(.+?)\\1")
        var last = 0
        bold.findAll(cleaned).forEach { match ->
            append(cleaned.substring(last, match.range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(match.groupValues[2])
            }
            last = match.range.last + 1
        }
        append(cleaned.substring(last))
    }
}

@Composable
private fun StatusRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ExpressiveLoadingIndicator(size = 20.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatReleaseDate(raw: String): String {
    if (raw.length < 10) return ""
    return runCatching {
        val parts = raw.substring(0, 10).split("-")
        "${parts[2]}.${parts[1]}.${parts[0]}"
    }.getOrDefault("")
}

private fun openExternalBrowser(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
