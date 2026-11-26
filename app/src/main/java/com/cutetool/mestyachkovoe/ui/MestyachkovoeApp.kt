package com.cutetool.mestyachkovoe.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.cutetool.mestyachkovoe.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MestyachkovoeApp(settings: UserSettings, settingsRepo: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { NewsRepository() }
    val freshInstall = remember { isFreshInstall(context) }
    var selectedRegion by remember { mutableStateOf(Region.ALL) }
    var items by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedNews by remember { mutableStateOf<NewsItem?>(null) }
    var fineSettingsOpen by remember { mutableStateOf(false) }
    var versionDialogOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var updateIntroOpen by remember { mutableStateOf(false) }
    var districtPickerOpen by remember { mutableStateOf(false) }
    var cuteToolOpen by remember { mutableStateOf(false) }
    var cacheDialogOpen by remember { mutableStateOf(false) }
    var favoritesOpen by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var cidPollTick by remember { mutableIntStateOf(0) }
    var forceNextLoad by remember { mutableStateOf(false) }
    var loadId by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedTitles = remember(settings.savedPosts) {
        settings.savedPosts.map { it.key }.toSet()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) cidPollTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(settings.federalDistrict) {
        selectedRegion = Region.ALL
    }

    LaunchedEffect(Unit) {
        if (!freshInstall && !settings.onboardingDone) {
            settingsRepo.completeOnboarding(settings.federalDistrict)
            updateIntroOpen = true
        }
    }

    if (!settings.onboardingDone && freshInstall) {
        OnboardingScreen(
            initialDistrict = settings.federalDistrict,
            onContinue = { district ->
                scope.launch { settingsRepo.completeOnboarding(district) }
            }
        )
        return
    }

    fun reload() {
        forceNextLoad = true
        refreshTick++
    }

    fun toggleLike(item: NewsItem) {
        scope.launch {
            settingsRepo.toggleSavedPost(item)
            runCatching { CuteIdRepository.sendSync(settingsRepo.snapshot()) }
        }
    }

    fun toggleFavoriteSource(url: String) {
        scope.launch {
            settingsRepo.toggleSavedSource(url)
            runCatching { CuteIdRepository.sendSync(settingsRepo.snapshot()) }
        }
    }

    // While CID auth is pending, poll GitHub users.json every 2 seconds (also refreshed in onResume).
    LaunchedEffect(settings.cidSession, settings.cidLinked, cidPollTick) {
        val session = settings.cidSession
        if (settings.cidLinked || session.isNullOrBlank()) return@LaunchedEffect
        repeat(150) {
            delay(2000)
            val auth = runCatching { CuteIdRepository.pollForAuth(session) }.getOrNull()
                ?: return@repeat
            settingsRepo.mergeCidData(auth)
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                runCatching { CuteIdRepository.sendSync(settingsRepo.snapshot()) }
            }
            return@LaunchedEffect
        }
    }

    LaunchedEffect(
        selectedRegion,
        settings.federalDistrict,
        settings.disabledSources,
        settings.customSources,
        settings.smartFilter,
        settings.filterSvoPolitics,
        settings.filterAdult,
        refreshTick
    ) {
        val currentLoad = ++loadId
        val force = forceNextLoad
        forceNextLoad = false
        error = null

        val cached = repo.cached(selectedRegion, settings)
        if (cached != null) {
            items = cached
            loading = false
        } else {
            items = emptyList()
            loading = true
        }

        try {
            val loaded = repo.load(
                region = selectedRegion,
                settings = settings,
                forceRefresh = force
            )
            if (currentLoad == loadId) {
                items = loaded
                loading = false
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (currentLoad == loadId) {
                if (items.isEmpty()) error = e.message ?: "error"
                loading = false
            }
        }
    }

    if (favoritesOpen) {
        FavoritesScreen(
            settings = settings,
            settingsRepo = settingsRepo,
            onClose = { favoritesOpen = false },
            onOpenNews = { selectedNews = it }
        )
    } else {
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
                        onDistrict = {
                            scope.launch { drawerState.close() }
                            districtPickerOpen = true
                        },
                        onCuteTool = {
                            scope.launch { drawerState.close() }
                            cuteToolOpen = true
                        },
                        onCache = {
                            scope.launch { drawerState.close() }
                            cacheDialogOpen = true
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
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Меню")
                            }
                        },
                        actions = {
                            IconButton(onClick = { favoritesOpen = true }) {
                                Icon(Icons.Default.Favorite, contentDescription = "Любимое")
                            }
                            IconButton(onClick = { searchOpen = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Поиск по новостям")
                            }
                            IconButton(onClick = { reload() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(Modifier.padding(padding)) {
                    RegionChips(
                        district = settings.federalDistrict,
                        selected = selectedRegion,
                        onSelect = { selectedRegion = it }
                    )
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        when {
                            loading -> ExpressiveLoadingIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                size = 64.dp
                            )
                            error != null -> ErrorState(onRetry = { reload() })
                            items.isEmpty() -> Text("Новостей пока нет", Modifier.align(Alignment.Center))
                            else -> NewsList(
                                items = items,
                                savedTitles = savedTitles,
                                onItemClick = { selectedNews = it },
                                onToggleLike = { toggleLike(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedNews?.let { news ->
        val feedUrl = news.feedUrl.ifBlank { news.link }
        NewsDetailSheet(
            item = news,
            onDismiss = { selectedNews = null },
            saved = news.title.trim().lowercase() in savedTitles,
            sourceSaved = feedUrl in settings.savedSources,
            onToggleLike = { toggleLike(news) },
            onToggleSource = { toggleFavoriteSource(feedUrl) }
        )
    }

    if (fineSettingsOpen) {
        FineSettingsDialog(
            settings = settings,
            settingsRepo = settingsRepo,
            onClose = { fineSettingsOpen = false }
        )
    }

    if (searchOpen) {
        SearchDialog(
            onClose = { searchOpen = false },
            onOpenNews = { selectedNews = it },
            savedTitles = savedTitles,
            onToggleLike = { toggleLike(it) }
        )
    }

    if (versionDialogOpen) {
        VersionDialog(onClose = { versionDialogOpen = false })
    }

    if (updateIntroOpen) {
        UpdateIntroDialog(
            onKeepUral = { updateIntroOpen = false },
            onChooseDistrict = {
                updateIntroOpen = false
                districtPickerOpen = true
            }
        )
    }

    if (districtPickerOpen) {
        DistrictPickerDialog(
            current = settings.federalDistrict,
            onSelect = { scope.launch { settingsRepo.setFederalDistrict(it) } },
            onDismiss = { districtPickerOpen = false }
        )
    }

    if (cuteToolOpen) {
        CuteToolDialog(onClose = { cuteToolOpen = false })
    }

    if (cacheDialogOpen) {
        CacheDialog(
            settings = settings,
            settingsRepo = settingsRepo,
            onClose = { cacheDialogOpen = false }
        )
    }
}

@Composable
fun RegionChips(district: FederalDistrict, selected: Region, onSelect: (Region) -> Unit) {
    val regions = district.regions
    if (regions.size <= 1) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == Region.ALL,
                onClick = { onSelect(Region.ALL) },
                label = { Text(Region.ALL.title) }
            )
        }
        items(regions) { region ->
            FilterChip(
                selected = region == selected,
                onClick = { onSelect(region) },
                label = { Text(region.title, maxLines = 1) }
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
fun NewsList(
    items: List<NewsItem>,
    onItemClick: (NewsItem) -> Unit,
    savedTitles: Set<String> = emptySet(),
    onToggleLike: ((NewsItem) -> Unit)? = null
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id + it.published + it.title }) { item ->
            NewsCard(
                item = item,
                onClick = { onItemClick(item) },
                saved = item.title.trim().lowercase() in savedTitles,
                onToggleLike = onToggleLike?.let { toggle -> { toggle(item) } }
            )
        }
    }
}

@Composable
fun NewsCard(
    item: NewsItem,
    onClick: () -> Unit,
    saved: Boolean = false,
    onToggleLike: (() -> Unit)? = null
) {
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
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (item.sourceVerified) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Проверенный источник",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text("•", style = MaterialTheme.typography.bodySmall)
                        Text(formatTime(item.published), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (onToggleLike != null) {
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (saved) "Убрать из любимого" else "В любимое",
                            tint = if (saved) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailSheet(
    item: NewsItem,
    onDismiss: () -> Unit,
    saved: Boolean = false,
    sourceSaved: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    onToggleSource: (() -> Unit)? = null
) {
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
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            item.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (onToggleLike != null) {
                            IconButton(onClick = onToggleLike) {
                                Icon(
                                    if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (saved) "Убрать из любимого" else "В любимое",
                                    tint = if (saved) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (item.sourceVerified) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Проверенный источник",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text("•", style = MaterialTheme.typography.bodySmall)
                        Text(item.region.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("•", style = MaterialTheme.typography.bodySmall)
                        Text(formatTime(item.published), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { Divider() }
                item {
                    Text(
                        item.content.ifBlank { "Полный текст новости доступен на сайте источника." },
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
            if (onToggleSource != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onToggleSource,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (sourceSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (sourceSaved) "Источник в любимом" else "Источник в любимое!")
                }
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
    onDistrict: () -> Unit,
    onCuteTool: () -> Unit,
    onCache: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Divider()
        Text("Тема", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegButton("Monet", settings.themeMode == ThemeMode.MONET) { onTheme(ThemeMode.MONET) }
                SegButton("Ч/Б", settings.themeMode == ThemeMode.MONO) { onTheme(ThemeMode.MONO) }
            }
            SegButton("Светлая", settings.themeMode == ThemeMode.LIGHT) { onTheme(ThemeMode.LIGHT) }
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
                    "Источники, уведомления и фильтры",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Divider()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onCache)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Кеш", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Очистка и автоочистка: ${settings.cacheAutoClean.title.lowercase()}",
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
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onDistrict)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("ФедОкруг ленты", style = MaterialTheme.typography.titleMedium)
                Text(
                    settings.federalDistrict.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onCuteTool)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("by CuteTool", style = MaterialTheme.typography.titleMedium)
        }
        Divider()
        Text("О приложении", style = MaterialTheme.typography.titleMedium)
        Text(
            "Местячковое — агрегатор региональных новостей России. Собирает новости выбранного федерального округа в одну удобную ленту.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
    }
}

@Composable
fun CuteToolDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("by CuteTool", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                CuteToolLink(
                    title = "Telegram канал",
                    url = "https://t.me/cutetool",
                    onOpen = onClose
                )
                CuteToolLink(
                    title = "Мой сайт!!",
                    url = "https://cutetool.neocities.org",
                    onOpen = onClose
                )
                CuteToolLink(
                    title = "Новости «Местечко»",
                    url = "https://toolcute.github.io/Mestechko/",
                    onOpen = onClose
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Закрыть") }
        }
    )
}

@Composable
private fun CuteToolLink(title: String, url: String, onOpen: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                openLink(context, url)
                onOpen()
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
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
    if (ts <= 0L) return "дата неизвестна"
    val now = System.currentTimeMillis()
    val diff = (now - ts).coerceAtLeast(0L)
    val min = diff / 60000
    val hrs = diff / 3600000
    return when {
        min < 1 -> "только что"
        min < 60 -> "$min мин. назад"
        hrs < 24 -> "$hrs ч. назад"
        else -> SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(ts))
    }
}

@Suppress("DEPRECATION")
private fun isFreshInstall(context: Context): Boolean = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val delta = info.lastUpdateTime - info.firstInstallTime
    delta in 0..60_000L
}.getOrDefault(false)

internal fun openLink(context: android.content.Context, url: String) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
