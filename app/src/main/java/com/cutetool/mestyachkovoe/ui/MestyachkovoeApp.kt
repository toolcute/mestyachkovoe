package com.cutetool.mestyachkovoe.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import com.cutetool.mestyachkovoe.ui.theme.ExpressiveShapes
import com.cutetool.mestyachkovoe.ui.theme.pressScale
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
            onOpenNews = { selectedNews = it },
            onCheckCid = { cidPollTick++ }
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
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    ExpressiveTopBar(
                        title = settings.appLabel.title,
                        district = settings.federalDistrict.title,
                        onMenu = { scope.launch { drawerState.open() } },
                        onFavorites = { favoritesOpen = true },
                        onSearch = { searchOpen = true },
                        onRefresh = { reload() }
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
                                size = 56.dp
                            )
                            error != null -> ErrorState(onRetry = { reload() })
                            items.isEmpty() -> EmptyFeedState()
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
fun ExpressiveTopBar(
    title: String,
    district: String,
    onMenu: () -> Unit,
    onFavorites: () -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarAction(Icons.Default.Menu, "Меню", onMenu)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    district,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarPillAction(Icons.Default.Favorite, "Любимое", onFavorites)
                    TopBarPillAction(Icons.Default.Search, "Поиск по новостям", onSearch)
                    TopBarPillAction(Icons.Default.Refresh, "Обновить", onRefresh)
                }
            }
        }
    }
}

@Composable
private fun TopBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TopBarPillAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun RegionChips(district: FederalDistrict, selected: Region, onSelect: (Region) -> Unit) {
    val regions = district.regions
    if (regions.size <= 1) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            RegionChip(
                title = Region.ALL.title,
                selected = selected == Region.ALL,
                onClick = { onSelect(Region.ALL) }
            )
        }
        items(regions) { region ->
            RegionChip(
                title = region.title,
                selected = region == selected,
                onClick = { onSelect(region) }
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun RegionChip(title: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = CircleShape,
        label = {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun NewsList(
    items: List<NewsItem>,
    onItemClick: (NewsItem) -> Unit,
    savedTitles: Set<String> = emptySet(),
    onToggleLike: ((NewsItem) -> Unit)? = null
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
    val interaction = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = ExpressiveShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    item.source,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                            }
                            if (item.sourceVerified) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Проверенный источник",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                formatTime(item.published),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            item.region.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (onToggleLike != null) {
                        FilledTonalIconButton(
                            onClick = onToggleLike,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (saved) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (saved) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        ) {
                            Icon(
                                if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (saved) "Убрать из любимого" else "В любимое",
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFeedState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = ExpressiveShapes.tile,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Пока тихо", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Попробуйте обновить ленту или выбрать другой регион.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        sheetState = sheetState,
        shape = ExpressiveShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
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
                shape = ExpressiveShapes.button,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Открыть на сайте источника")
            }
            if (onToggleSource != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onToggleSource,
                    shape = ExpressiveShapes.button,
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
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = ExpressiveShapes.tile,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Не удалось загрузить новости", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Проверьте подключение к интернету и попробуйте снова.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, shape = ExpressiveShapes.button) { Text("Повторить") }
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
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Сделайте ленту своей",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text("Тема", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegButton("Monet", settings.themeMode == ThemeMode.MONET, Modifier.weight(1f)) {
                onTheme(ThemeMode.MONET)
            }
            SegButton("Ч/Б", settings.themeMode == ThemeMode.MONO, Modifier.weight(1f)) {
                onTheme(ThemeMode.MONO)
            }
        }
        SegButton("Светлая", settings.themeMode == ThemeMode.LIGHT, Modifier.fillMaxWidth()) {
            onTheme(ThemeMode.LIGHT)
        }

        SettingsActionCard(
            icon = Icons.Default.Tune,
            title = "Тонкая настройка",
            subtitle = "Источники, уведомления и фильтры",
            onClick = onFineSettings
        )
        SettingsActionCard(
            icon = Icons.Default.Storage,
            title = "Кеш",
            subtitle = "Очистка и автоочистка: ${settings.cacheAutoClean.title.lowercase()}",
            onClick = onCache
        )
        SettingsActionCard(
            icon = Icons.Default.Info,
            title = "Версия приложения",
            subtitle = "Обновления и ченжлог",
            onClick = onVersion
        )
        SettingsActionCard(
            icon = Icons.Default.Public,
            title = "ФедОкруг ленты",
            subtitle = settings.federalDistrict.title,
            onClick = onDistrict
        )
        SettingsActionCard(
            icon = Icons.Default.Person,
            title = "by CuteTool",
            subtitle = "Сайт, Telegram и «Местечко»",
            onClick = onCuteTool
        )

        Surface(
            shape = ExpressiveShapes.hero,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "О приложении",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Местячковое — агрегатор региональных новостей России. Собирает новости выбранного федерального округа в одну удобную ленту.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = onClose,
            shape = ExpressiveShapes.button,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Закрыть") }
    }
}

@Composable
private fun SettingsActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ExpressiveShapes.tile,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CuteToolDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        shape = ExpressiveShapes.hero,
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
fun SegButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, shape = ExpressiveShapes.button, modifier = modifier) {
            Text(text, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = onClick, shape = ExpressiveShapes.button, modifier = modifier) {
            Text(text, maxLines = 1)
        }
    }
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
