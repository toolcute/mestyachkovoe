package com.cutetool.mestyachkovoe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cutetool.mestyachkovoe.updates.UpdateManager
import kotlinx.coroutines.launch

private sealed interface VersionStatus {
    data object Idle : VersionStatus
    data object Checking : VersionStatus
    data object UpToDate : VersionStatus
    data object Downloading : VersionStatus
    data object InstallerOpened : VersionStatus
    data class NeedsPermission(val info: UpdateManager.UpdateInfo) : VersionStatus
    data class Error(val message: String) : VersionStatus
}

@Composable
fun VersionDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember { UpdateManager.currentVersion(context) }
    var status by remember { mutableStateOf<VersionStatus>(VersionStatus.Idle) }
    val busy = status is VersionStatus.Checking || status is VersionStatus.Downloading

    fun checkForUpdates() {
        scope.launch {
            status = VersionStatus.Checking
            try {
                val info = UpdateManager.latestRelease()
                when {
                    info == null -> status = VersionStatus.Error("В релизах не найден APK-файл.")
                    !UpdateManager.isNewer(info.version, currentVersion) -> status = VersionStatus.UpToDate
                    !UpdateManager.canInstall(context) -> status = VersionStatus.NeedsPermission(info)
                    else -> {
                        status = VersionStatus.Downloading
                        val file = UpdateManager.downloadApk(context, info)
                        status = if (UpdateManager.install(context, file)) {
                            VersionStatus.InstallerOpened
                        } else {
                            VersionStatus.NeedsPermission(info)
                        }
                    }
                }
            } catch (e: Exception) {
                status = VersionStatus.Error(e.message ?: "Не удалось проверить обновления")
            }
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Версия приложения",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text("Текущая версия: $currentVersion", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Обновления проверяются в GitHub Releases репозитория toolcute/mestyachkovoe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider()

                when (val state = status) {
                    VersionStatus.Idle -> {
                        Text(
                            "Нажмите кнопку ниже, чтобы проверить наличие новой версии.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    VersionStatus.Checking -> StatusRow("Проверка обновлений…")
                    VersionStatus.Downloading -> StatusRow("Загрузка обновления…")
                    VersionStatus.UpToDate -> {
                        Text(
                            "Установлена актуальная версия.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    VersionStatus.InstallerOpened -> {
                        Text(
                            "Установщик открыт. Подтвердите обновление в системе Android.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is VersionStatus.NeedsPermission -> {
                        Text(
                            "Для установки обновления разрешите установку из этого источника.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Доступная версия: ${state.info.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { UpdateManager.openInstallPermissionSettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Открыть настройки")
                        }
                    }
                    is VersionStatus.Error -> {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Button(
                    onClick = { checkForUpdates() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Проверить обновления")
                }
                TextButton(
                    onClick = { openLink(context, UpdateManager.RELEASES_URL) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Самостоятельно проверить")
                }
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
private fun StatusRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
