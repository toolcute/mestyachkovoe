package com.cutetool.mestyachkovoe.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.cutetool.mestyachkovoe.data.CuteIdRepository
import com.cutetool.mestyachkovoe.data.NewsItem
import com.cutetool.mestyachkovoe.data.NewsRepository
import com.cutetool.mestyachkovoe.data.SavedPost
import com.cutetool.mestyachkovoe.data.SettingsRepository
import com.cutetool.mestyachkovoe.data.Sources
import com.cutetool.mestyachkovoe.data.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    settings: UserSettings,
    settingsRepo: SettingsRepository,
    onClose: () -> Unit,
    onOpenNews: (NewsItem) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var miniFeedUrl by remember { mutableStateOf<String?>(null) }
    var showFullCid by remember { mutableStateOf(false) }
    val savedTitles = remember(settings.savedPosts) {
        settings.savedPosts.map { it.key }.toSet()
    }

    LaunchedEffect(Unit) {
        delay(1000)
        showFullCid = true
    }

    fun syncCid() {
        scope.launch { CuteIdRepository.sendSync(settingsRepo.snapshot()) }
    }

    val currentMiniFeed = miniFeedUrl
    if (currentMiniFeed != null) {
        MiniFeedScreen(
            url = currentMiniFeed,
            settings = settings,
            settingsRepo = settingsRepo,
            onBack = { miniFeedUrl = null },
            onOpenNews = onOpenNews,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showFullCid) "Мой CuteID" else "Мой CID",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CidAccountSection(
                    settings = settings,
                    settingsRepo = settingsRepo,
                    onSync = { syncCid() }
                )
            }

            item { DividerLine() }

            item {
                SectionTitle(
                    title = "Любимые статьи",
                    subtitle = if (settings.savedPosts.isEmpty()) {
                        "Нажимайте сердечко на карточке новости"
                    } else {
                        "Сохранено: ${settings.savedPosts.size}"
                    }
                )
            }

            if (settings.savedPosts.isEmpty()) {
                item { EmptyHint("Пока нет любимых статей.") }
            } else {
                items(settings.savedPosts, key = { it.key }) { post ->
                    SavedPostCard(
                        post = post,
                        onOpen = if (post.link.isNotBlank()) {
                            { onOpenNews(post.toNewsItem()) }
                        } else null,
                        onRemove = {
                            scope.launch {
                                settingsRepo.removeSavedPost(post)
                                syncCid()
                            }
                        }
                    )
                }
            }

            item { DividerLine() }

            item {
                SectionTitle(
                    title = "Любимые источники",
                    subtitle = if (settings.savedSources.isEmpty()) {
                        "Кнопка «Источник в любимое!» в новости"
                    } else {
                        "Сохранено: ${settings.savedSources.size}"
                    }
                )
            }

            if (settings.savedSources.isEmpty()) {
                item { EmptyHint("Пока нет любимых источников.") }
            } else {
                items(settings.savedSources.sortedBy { favoriteSourceName(it, settings) }, key = { it }) { url ->
                    SavedSourceCard(
                        name = favoriteSourceName(url, settings),
                        url = url,
                        onOpen = { miniFeedUrl = url },
                        onRemove = {
                            scope.launch {
                                settingsRepo.toggleSavedSource(url)
                                syncCid()
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CidAccountSection(
    settings: UserSettings,
    settingsRepo: SettingsRepository,
    onSync: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nickname by remember(settings.cidNickname) {
        mutableStateOf(settings.cidNickname.orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }
    val pending = !settings.cidLinked && !settings.cidSession.isNullOrBlank()
    val valid = CuteIdRepository.isValidNickname(nickname)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "CuteID (CID)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (settings.cidLinked) {
                            "Любимое синхронизируется между устройствами"
                        } else {
                            "Необязательно. Можно переносить любимое между устройствами."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (settings.cidLinked) {
                Text(
                    "Вы вошли как ${settings.cidNickname ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                settings.cidTgId?.let {
                    Text(
                        "Telegram ID: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        nickname = it.filter { ch -> ch in 'a'..'z' || ch in 'A'..'Z' }
                        error = null
                    },
                    label = { Text("Новое имя (4–9 английских букв)") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        Text(error ?: "Только латиница: ^[a-zA-Z]{4,9}$")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val clean = nickname.trim()
                            error = if (CuteIdRepository.isValidNickname(clean)) {
                                null
                            } else {
                                "Нужно 4–9 английских букв"
                            }
                            if (error == null) {
                                scope.launch {
                                    settingsRepo.setCidNickname(clean)
                                    onSync()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сменить имя")
                    }
                    OutlinedButton(
                        onClick = { onSync() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Синхронизировать")
                    }
                }
            } else {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        nickname = it.filter { ch -> ch in 'a'..'z' || ch in 'A'..'Z' }
                        error = null
                    },
                    label = { Text("Имя CuteID") },
                    placeholder = { Text("Например: Cutis") },
                    singleLine = true,
                    isError = nickname.isNotBlank() && !valid,
                    supportingText = {
                        Text(
                            if (nickname.isNotBlank() && !valid) {
                                "Нужно 4–9 английских букв"
                            } else {
                                "4–9 английских букв, например Cutis"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        val clean = nickname.trim()
                        if (!CuteIdRepository.isValidNickname(clean)) {
                            error = "Нужно 4–9 английских букв"
                            return@Button
                        }
                        error = null
                        val session = CuteIdRepository.newSessionToken()
                        scope.launch {
                            settingsRepo.beginCidAuth(clean, session)
                            val username = CuteIdRepository.botUsername()
                            openTelegram(
                                context,
                                CuteIdRepository.authUrl(username, session, clean)
                            )
                        }
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Войти через Telegram")
                }
                if (pending) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExpressiveLoadingIndicator(size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Ожидаем подтверждение в Telegram…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(
                        onClick = {
                            val session = settings.cidSession ?: return@TextButton
                            val nick = settings.cidNickname ?: return@TextButton
                            scope.launch {
                                val username = CuteIdRepository.botUsername()
                                openTelegram(
                                    context,
                                    CuteIdRepository.authUrl(username, session, nick)
                                )
                            }
                        }
                    ) {
                        Text("Открыть Telegram снова")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DividerLine() {
    androidx.compose.material3.HorizontalDivider()
}

@Composable
private fun SavedPostCard(
    post: SavedPost,
    onOpen: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!post.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    post.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (post.source.isNotBlank()) {
                        Text(
                            post.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (post.published > 0L) {
                        Text(
                            formatTime(post.published),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Убрать из любимого",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SavedSourceCard(
    name: String,
    url: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = "Открыть мини-ленту",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Убрать источник")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiniFeedScreen(
    url: String,
    settings: UserSettings,
    settingsRepo: SettingsRepository,
    onBack: () -> Unit,
    onOpenNews: (NewsItem) -> Unit,
) {
    val repo = remember { NewsRepository() }
    val scope = rememberCoroutineScope()
    val savedTitles = remember(settings.savedPosts) {
        settings.savedPosts.map { it.key }.toSet()
    }
    var items by remember(url) { mutableStateOf<List<NewsItem>>(emptyList()) }
    var loading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        loading = true
        error = null
        try {
            items = repo.loadSource(url, settings.customSources)
        } catch (e: Exception) {
            error = e.message ?: "Не удалось загрузить источник"
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            favoriteSourceName(url, settings),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Мини-лента источника · без фильтрации",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> ExpressiveLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    size = 56.dp
                )
                error != null -> Text(
                    error ?: "",
                    Modifier.align(Alignment.Center).padding(20.dp),
                    color = MaterialTheme.colorScheme.error
                )
                items.isEmpty() -> Text(
                    "В этом источнике пока нет постов.",
                    Modifier.align(Alignment.Center).padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> NewsList(
                    items = items,
                    savedTitles = savedTitles,
                    onItemClick = onOpenNews,
                    onToggleLike = { item ->
                        scope.launch {
                            settingsRepo.toggleSavedPost(item)
                            CuteIdRepository.sendSync(settingsRepo.snapshot())
                        }
                    }
                )
            }
        }
    }
}

internal fun favoriteSourceName(url: String, settings: UserSettings): String {
    (Sources.all + settings.customSources).firstOrNull { it.url == url }?.let { return it.name }
    return runCatching {
        URL(url).host.removePrefix("www.").ifBlank { url }
    }.getOrDefault(url)
}

private fun SavedPost.toNewsItem(): NewsItem = NewsItem(
    id = link.ifBlank { "saved:${title}" },
    title = title,
    link = link,
    imageUrl = imageUrl,
    source = source.ifBlank { "CuteID" },
    published = published,
    region = region,
    feedUrl = feedUrl,
)

private fun openTelegram(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { openLink(context, url) }
}
