package com.cutetool.mestyachkovoe.ui

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cutetool.mestyachkovoe.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MestyachkovoeApp(settings: UserSettings, settingsRepo: SettingsRepository) {
    val scope = rememberCoroutineScope()
    val repo = remember { NewsRepository() }
    var selectedRegion by remember { mutableStateOf(Region.ALL) }
    var items by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedNews by remember { mutableStateOf<NewsItem?>(null) }
    var fineSettingsOpen by remember { mutableStateOf(false) }
    var versionDialogOpen by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                items = repo.load(selectedRegion, settings)
            } catch (e: Exception) {
                error = e.message ?: "error"
            }
            loading = false
        }
    }

    LaunchedEffect(selectedRegion, settings.disabledSources, settings.customSources, settings.smartFilter) {
        load()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
                SettingsSheet(
                    settings = settings,
                    onTheme = { scope.launch { settingsRepo.setTheme(it) } },
                    onFineSettings = {
                        scope.launch { drawerState.close() }
                        fineSettingsOpen = true
                    },
                    onVersion = {
                        scope.launch { drawerState.close() }
                        versionDialogOpen = true
                    },
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(settings.appLabel.title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {},
                    actions = {
                        IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, contentDescription = "Обновить") }
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                RegionChips(selected = selectedRegion, onSelect = { selectedRegion = it })
                Box(Modifier.fillMaxSize()) {
                    when {
                        loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        error != null -> ErrorState(onRetry = { load() })
                        items.isEmpty() -> Text("Новостей пока нет", Modifier.align(Alignment.Center))
                        else -> NewsList(items = items, onItemClick = { selectedNews = it })
                    }
                }
            }
        }
    }

    selectedNews?.let { news ->
        NewsDetailSheet(item = news, onDismiss = { selectedNews = null })
    }

    if (fineSettingsOpen) {
        FineSettingsDialog(
            settings = settings,
            settingsRepo = settingsRepo,
            onClose = { fineSettingsOpen = false }
        )
    }

    if (versionDialogOpen) {
        VersionDialog(onClose = { versionDialogOpen = false })
    }
}

@Composable
fun RegionChips(selected: Region, onSelect: (Region) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Region.entries) { region ->
            FilterChip(
                selected = region == selected,
                onClick = { onSelect(region) },
                label = { Text(region.title, maxLines = 1) }
            )
        }
    }
}

@Composable
fun NewsList(items: List<NewsItem>, onItemClick: (NewsItem) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id + it.published }) { item ->
            NewsCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
fun NewsCard(item: NewsItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(Modifier.padding(12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("•", style = MaterialTheme.typography.bodySmall)
                    Text(formatTime(item.published), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailSheet(item: NewsItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    item {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                item {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text("•", style = MaterialTheme.typography.bodySmall)
                        Text(item.region.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("•", style = MaterialTheme.typography.bodySmall)
                        Text(formatTime(item.published), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { Divider() }
                item {
                    Text(
                        item.content.ifBlank { "Полный текст доступен на сайте источника." },
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { openLink(context, item.link) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Открыть на сайте источника")
            }
        }
    }
}

@Composable
fun ErrorState(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Не удалось загрузить новости")
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
fun SettingsSheet(
    settings: UserSettings,
    onTheme: (ThemeMode) -> Unit,
    onFineSettings: () -> Unit,
    onVersion: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Меню", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Divider()
        Text("Тема", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegButton("Ч/Б", settings.themeMode == ThemeMode.MONO) { onTheme(ThemeMode.MONO) }
            SegButton("Monet", settings.themeMode == ThemeMode.MONET) { onTheme(ThemeMode.MONET) }
        }
        Divider()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onFineSettings)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Тонкая настройка", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ярлык, источники и уведомления",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Divider()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onVersion)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("Версия приложения", style = MaterialTheme.typography.titleMedium)
        }
        Divider()
        Text("О приложении", style = MaterialTheme.typography.titleMedium)
        Text("Приложение, собирающее все главные новости Урала в одной ленте", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Text("by CuteTool", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(
            "telegram",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { openLink(context, "https://t.me/cutetool") }
        )
        Text(
            "Наш сайт!!",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { openLink(context, "https://cutetool.neocities.org") }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
    }
}

@Composable
fun SegButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(text) } else OutlinedButton(onClick = onClick) { Text(text) }
}

@Composable
fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(text)
    }
}

internal fun formatTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    val min = diff / 60000
    val hrs = diff / 3600000
    return when {
        min < 60 -> "$min мин. назад"
        hrs < 24 -> "$hrs ч. назад"
        else -> SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(ts))
    }
}

internal fun openLink(context: android.content.Context, url: String) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
