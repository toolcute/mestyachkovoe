package com.cutetool.mestyachkovoe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cutetool.mestyachkovoe.cache.CacheManager
import com.cutetool.mestyachkovoe.data.CacheAutoClean
import com.cutetool.mestyachkovoe.data.SettingsRepository
import com.cutetool.mestyachkovoe.data.UserSettings
import com.cutetool.mestyachkovoe.ui.theme.ExpressiveShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CacheDialog(
    settings: UserSettings,
    settingsRepo: SettingsRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sizeBytes by remember { mutableStateOf<Long?>(null) }
    var clearing by remember { mutableStateOf(false) }
    var cleared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sizeBytes = withContext(Dispatchers.IO) { CacheManager.size(context) }
    }

    AlertDialog(
        onDismissRequest = onClose,
        shape = ExpressiveShapes.hero,
        title = { Text("Кеш", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Кеш ускоряет загрузку ленты и картинок. Если он разрастается, его можно очистить вручную или включить автоочистку.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    shape = ExpressiveShapes.tile,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Занято кешем", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                sizeBytes?.let { CacheManager.formatSize(it) } ?: "Считаем…",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            clearing = true
                            withContext(Dispatchers.IO) { CacheManager.clear(context) }
                            sizeBytes = withContext(Dispatchers.IO) { CacheManager.size(context) }
                            clearing = false
                            cleared = true
                        }
                    },
                    enabled = !clearing,
                    shape = ExpressiveShapes.button,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (clearing) "Очищаем…" else "Очистить кеш")
                }

                if (cleared) {
                    Text(
                        "Готово, кеш очищен.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text("Автоочистка", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CacheAutoClean.entries.forEach { mode ->
                        RadioRow(mode.title, settings.cacheAutoClean == mode) {
                            scope.launch { settingsRepo.setCacheAutoClean(mode) }
                        }
                    }
                }
                Text(
                    "Автоочистка удаляет картинки и временные файлы. Источники и настройки останутся на месте.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Закрыть") }
        }
    )
}
