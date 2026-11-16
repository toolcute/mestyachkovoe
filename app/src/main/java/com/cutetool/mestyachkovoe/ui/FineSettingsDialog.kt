package com.cutetool.mestyachkovoe.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.cutetool.mestyachkovoe.AppLabelManager
import com.cutetool.mestyachkovoe.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FineSettingsDialog(
    settings: UserSettings,
    settingsRepo: SettingsRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val feeds = Sources.all + settings.customSources

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Тонкая настройка", fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SectionHeader(
                            "Ярлык на рабочем столе",
                            "Независимо выберите название и иконку приложения в лаунчере"
                        )
                    }
                    item {
                        Text("Название", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            AppLabel.entries.forEach { label ->
                                RadioRow(label.title, settings.appLabel == label) {
                                    scope.launch {
                                        settingsRepo.setAppLabel(label)
                                        AppLabelManager.apply(context, label, settings.launcherIcon)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text("Иконка", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            LauncherIcon.entries.forEach { icon ->
                                RadioRow(
                                    if (icon == LauncherIcon.NEW) "Новая иконка" else "Предыдущая иконка",
                                    settings.launcherIcon == icon
                                ) {
                                    scope.launch {
                                        settingsRepo.setLauncherIcon(icon)
                                        AppLabelManager.apply(context, settings.appLabel, icon)
                                    }
                                }
                            }
                        }
                    }

                    item { Divider() }

                    item {
                        SectionHeader(
                            "Умная фильтрация",
                            "Скрывает СВО, спецоперацию, федеральную повестку и новости соседних регионов"
                        )
                    }
                    item {
                        SwitchRow(
                            title = "Фильтровать чужие регионы",
                            subtitle = "Работает во вкладках конкретных областей",
                            checked = settings.smartFilter,
                            onCheckedChange = { scope.launch { settingsRepo.setSmartFilter(it) } }
                        )
                    }

                    item { Divider() }

                    item {
                        SectionHeader(
                            "Управление источниками",
                            "Отключайте лишние RSS-ленты или добавляйте свои"
                        )
                    }
                    items(feeds, key = { it.url }) { source ->
                        val isCustom = settings.customSources.any { it.url == source.url }
                        SourceRow(
                            source = source,
                            enabled = source.url !in settings.disabledSources,
                            onToggle = { enabled ->
                                scope.launch { settingsRepo.setSourceEnabled(source.url, enabled) }
                            },
                            onDelete = if (isCustom) {
                                { scope.launch { settingsRepo.removeCustomSource(source.url) } }
                            } else null
                        )
                    }
                    item {
                        AddSourceForm(
                            existingUrls = feeds.map { it.url }.toSet(),
                            settingsRepo = settingsRepo
                        )
                    }

                    item { Divider() }

                    item {
                        NotificationsSection(settings = settings, settingsRepo = settingsRepo)
                    }

                    item { Divider() }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Помогите сделать приложение лучше",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Есть идея или замечание? Напишите своё предложение в наш Telegram-канал.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(onClick = { openLink(context, "https://t.me/cutetool") }) {
                                    Text("t.me/cutetool")
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SourceRow(
    source: FeedSource,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(source.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${source.region.title} • ${source.url}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить источник")
            }
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun AddSourceForm(
    existingUrls: Set<String>,
    settingsRepo: SettingsRepository
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var region by remember { mutableStateOf(Region.CHELYABINSK) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Своя RSS-ссылка", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it; error = null },
            label = { Text("RSS-ссылка") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название (необязательно)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        EnumDropdown(
            label = "Регион",
            selected = region,
            options = Region.entries,
            title = { it.title },
            onSelect = { region = it }
        )
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
                val cleanUrl = url.trim()
                error = when {
                    !cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://") ->
                        "Ссылка должна начинаться с http:// или https://"
                    cleanUrl in existingUrls -> "Этот источник уже добавлен"
                    else -> null
                }
                if (error == null) {
                    val source = FeedSource(
                        name = name.trim().ifBlank { "Свой источник" },
                        url = cleanUrl,
                        region = region
                    )
                    scope.launch {
                        settingsRepo.addCustomSource(source)
                        name = ""
                        url = ""
                        error = null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Добавить источник")
        }
    }
}

@Composable
private fun NotificationsSection(
    settings: UserSettings,
    settingsRepo: SettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scope.launch { settingsRepo.setNotifyEnabled(true) }
    }

    fun changeNotifications(enabled: Boolean) {
        if (!enabled) {
            scope.launch { settingsRepo.setNotifyEnabled(false) }
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch { settingsRepo.setNotifyEnabled(true) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            "Уведомления",
            "Фоновая проверка новостей работает даже при закрытом приложении"
        )
        SwitchRow(
            title = "Уведомлять о новостях",
            subtitle = "Можно полностью отключить в любой момент",
            checked = settings.notifyEnabled,
            onCheckedChange = { changeNotifications(it) }
        )

        if (settings.notifyEnabled) {
            Text("Частота", style = MaterialTheme.typography.bodyMedium)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                NotifyFrequency.entries.forEach { frequency ->
                    RadioRow(frequency.title, settings.notifyFrequency == frequency) {
                        scope.launch { settingsRepo.setFrequency(frequency) }
                    }
                }
            }

            Text("Важность", style = MaterialTheme.typography.bodyMedium)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                NotifyImportance.entries.forEach { importance ->
                    RadioRow(importance.title, settings.notifyImportance == importance) {
                        scope.launch { settingsRepo.setImportance(importance) }
                    }
                }
            }

            EnumDropdown(
                label = "Уведомлять только по региону",
                selected = settings.notifyRegion,
                options = Region.entries,
                title = { if (it == Region.ALL) "Все регионы" else it.title },
                onSelect = { scope.launch { settingsRepo.setNotifyRegion(it) } }
            )

            Text(
                "Android проверяет новости в фоне не чаще раза в 15 минут. Для мгновенных уведомлений разрешите приложению работу в фоне без ограничений.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun <T> EnumDropdown(
    label: String,
    selected: T,
    options: List<T>,
    title: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title(selected),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(title(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
