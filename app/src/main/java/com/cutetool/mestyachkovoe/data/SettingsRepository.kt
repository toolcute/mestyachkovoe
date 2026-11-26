package com.cutetool.mestyachkovoe.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore("settings")

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.MONET,
    val notifyEnabled: Boolean = false,
    val notifyFrequency: NotifyFrequency = NotifyFrequency.INSTANT,
    val notifyImportance: NotifyImportance = NotifyImportance.ALL,
    val notifyRegion: Region = Region.ALL,
    val appLabel: AppLabel = AppLabel.MESTYACHKOVOE,
    val launcherIcon: LauncherIcon = LauncherIcon.CLASSIC,
    val smartFilter: Boolean = true,
    val filterSvoPolitics: Boolean = true,
    val filterAdult: Boolean = true,
    val disabledSources: Set<String> = emptySet(),
    val customSources: List<FeedSource> = emptyList(),
    val federalDistrict: FederalDistrict = FederalDistrict.URAL,
    val cacheAutoClean: CacheAutoClean = CacheAutoClean.WEEKLY,
    val onboardingDone: Boolean = false,
    val savedPosts: List<SavedPost> = emptyList(),
    val savedSources: Set<String> = emptySet(),
    val cidNickname: String? = null,
    val cidSession: String? = null,
    val cidTgId: Long? = null,
    val cidLinked: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val NOTIFY_ENABLED = booleanPreferencesKey("notify_enabled")
        val FREQ = stringPreferencesKey("notify_freq")
        val IMPORTANCE = stringPreferencesKey("notify_importance")
        val NOTIFY_REGION = stringPreferencesKey("notify_region")
        val APP_LABEL = stringPreferencesKey("app_label")
        val LAUNCHER_ICON = stringPreferencesKey("launcher_icon")
        val SMART_FILTER = booleanPreferencesKey("smart_filter")
        val FILTER_SVO = booleanPreferencesKey("filter_svo_politics")
        val FILTER_ADULT = booleanPreferencesKey("filter_adult")
        val DISABLED = stringSetPreferencesKey("disabled_sources")
        val CUSTOM = stringPreferencesKey("custom_sources")
        val FEDERAL_DISTRICT = stringPreferencesKey("federal_district")
        val CACHE_AUTO_CLEAN = stringPreferencesKey("cache_auto_clean")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SAVED_POSTS = stringPreferencesKey("saved_posts")
        val SAVED_SOURCES = stringSetPreferencesKey("saved_sources")
        val CID_NICKNAME = stringPreferencesKey("cid_nickname")
        val CID_SESSION = stringPreferencesKey("cid_session")
        val CID_TG_ID = longPreferencesKey("cid_tg_id")
        val CID_LINKED = booleanPreferencesKey("cid_linked")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        val rawTheme = p[Keys.THEME]
        UserSettings(
            themeMode = when (rawTheme) {
                null -> ThemeMode.MONET
                else -> runCatching { ThemeMode.valueOf(rawTheme) }.getOrDefault(ThemeMode.MONET)
            },
            notifyEnabled = p[Keys.NOTIFY_ENABLED] ?: false,
            notifyFrequency = runCatching { NotifyFrequency.valueOf(p[Keys.FREQ] ?: "") }.getOrDefault(NotifyFrequency.INSTANT),
            notifyImportance = runCatching { NotifyImportance.valueOf(p[Keys.IMPORTANCE] ?: "") }.getOrDefault(NotifyImportance.ALL),
            notifyRegion = runCatching { Region.valueOf(p[Keys.NOTIFY_REGION] ?: "") }.getOrDefault(Region.ALL),
            appLabel = runCatching { AppLabel.valueOf(p[Keys.APP_LABEL] ?: "") }.getOrDefault(AppLabel.MESTYACHKOVOE),
            launcherIcon = runCatching { LauncherIcon.valueOf(p[Keys.LAUNCHER_ICON] ?: "") }.getOrDefault(LauncherIcon.CLASSIC),
            smartFilter = p[Keys.SMART_FILTER] ?: true,
            filterSvoPolitics = p[Keys.FILTER_SVO] ?: true,
            filterAdult = p[Keys.FILTER_ADULT] ?: true,
            disabledSources = p[Keys.DISABLED].orEmpty(),
            customSources = decodeSources(p[Keys.CUSTOM]),
            federalDistrict = runCatching {
                FederalDistrict.valueOf(p[Keys.FEDERAL_DISTRICT] ?: "")
            }.getOrDefault(FederalDistrict.URAL),
            cacheAutoClean = p[Keys.CACHE_AUTO_CLEAN]?.let { raw ->
                runCatching { CacheAutoClean.valueOf(raw) }.getOrNull()
            } ?: CacheAutoClean.WEEKLY,
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
            savedPosts = decodeSavedPosts(p[Keys.SAVED_POSTS]),
            savedSources = p[Keys.SAVED_SOURCES].orEmpty(),
            cidNickname = p[Keys.CID_NICKNAME]?.takeIf { it.isNotBlank() },
            cidSession = p[Keys.CID_SESSION]?.takeIf { it.isNotBlank() },
            cidTgId = p[Keys.CID_TG_ID],
            cidLinked = p[Keys.CID_LINKED] ?: false,
        )
    }

    suspend fun snapshot(): UserSettings = settings.first()

    suspend fun setTheme(mode: ThemeMode) { context.dataStore.edit { it[Keys.THEME] = mode.name } }
    suspend fun setFrequency(freq: NotifyFrequency) { context.dataStore.edit { it[Keys.FREQ] = freq.name } }
    suspend fun setImportance(imp: NotifyImportance) { context.dataStore.edit { it[Keys.IMPORTANCE] = imp.name } }
    suspend fun setNotifyEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.NOTIFY_ENABLED] = enabled } }
    suspend fun setNotifyRegion(region: Region) { context.dataStore.edit { it[Keys.NOTIFY_REGION] = region.name } }
    suspend fun setAppLabel(label: AppLabel) { context.dataStore.edit { it[Keys.APP_LABEL] = label.name } }
    suspend fun setLauncherIcon(icon: LauncherIcon) { context.dataStore.edit { it[Keys.LAUNCHER_ICON] = icon.name } }
    suspend fun setSmartFilter(enabled: Boolean) { context.dataStore.edit { it[Keys.SMART_FILTER] = enabled } }
    suspend fun setFilterSvoPolitics(enabled: Boolean) { context.dataStore.edit { it[Keys.FILTER_SVO] = enabled } }
    suspend fun setFilterAdult(enabled: Boolean) { context.dataStore.edit { it[Keys.FILTER_ADULT] = enabled } }
    suspend fun setFederalDistrict(district: FederalDistrict) {
        context.dataStore.edit { it[Keys.FEDERAL_DISTRICT] = district.name }
    }

    suspend fun setCacheAutoClean(mode: CacheAutoClean) {
        context.dataStore.edit { it[Keys.CACHE_AUTO_CLEAN] = mode.name }
    }

    suspend fun completeOnboarding(district: FederalDistrict) {
        context.dataStore.edit {
            it[Keys.FEDERAL_DISTRICT] = district.name
            it[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun setSourceEnabled(url: String, enabled: Boolean) {
        context.dataStore.edit { p ->
            val current = p[Keys.DISABLED].orEmpty().toMutableSet()
            if (enabled) current.remove(url) else current.add(url)
            p[Keys.DISABLED] = current
        }
    }

    suspend fun addCustomSource(source: FeedSource) {
        context.dataStore.edit { p ->
            val current = decodeSources(p[Keys.CUSTOM]).filterNot { it.url == source.url }
            p[Keys.CUSTOM] = encodeSources(current + source)
        }
    }

    suspend fun removeCustomSource(url: String) {
        context.dataStore.edit { p ->
            p[Keys.CUSTOM] = encodeSources(decodeSources(p[Keys.CUSTOM]).filterNot { it.url == url })
            p[Keys.DISABLED] = p[Keys.DISABLED].orEmpty() - url
        }
    }

    suspend fun toggleSavedPost(item: NewsItem): Boolean {
        var saved = false
        context.dataStore.edit { p ->
            val current = decodeSavedPosts(p[Keys.SAVED_POSTS])
            val key = item.title.trim().lowercase()
            val existing = current.any { it.key == key }
            val updated = if (existing) {
                current.filterNot { it.key == key }
            } else {
                saved = true
                current + SavedPost(
                    title = item.title.trim(),
                    link = item.link,
                    source = item.source,
                    imageUrl = item.imageUrl,
                    published = item.published,
                    region = item.region,
                    feedUrl = item.feedUrl,
                )
            }
            p[Keys.SAVED_POSTS] = encodeSavedPosts(updated.sortedByDescending { it.published })
        }
        return saved
    }

    suspend fun removeSavedPost(post: SavedPost) {
        context.dataStore.edit { p ->
            p[Keys.SAVED_POSTS] = encodeSavedPosts(
                decodeSavedPosts(p[Keys.SAVED_POSTS]).filterNot { it.key == post.key }
            )
        }
    }

    suspend fun toggleSavedSource(url: String): Boolean {
        val clean = url.trim()
        if (clean.isBlank()) return false
        var saved = false
        context.dataStore.edit { p ->
            val current = p[Keys.SAVED_SOURCES].orEmpty().toMutableSet()
            if (clean in current) {
                current.remove(clean)
            } else {
                current.add(clean)
                saved = true
            }
            p[Keys.SAVED_SOURCES] = current
        }
        return saved
    }

    suspend fun setSavedSources(urls: Set<String>) {
        context.dataStore.edit { p -> p[Keys.SAVED_SOURCES] = urls }
    }

    suspend fun beginCidAuth(nickname: String, session: String) {
        context.dataStore.edit { p ->
            p[Keys.CID_NICKNAME] = nickname
            p[Keys.CID_SESSION] = session
            p[Keys.CID_LINKED] = false
            p.remove(Keys.CID_TG_ID)
        }
    }

    suspend fun cancelCidAuth() {
        context.dataStore.edit { p -> p.remove(Keys.CID_SESSION) }
    }

    suspend fun setCidNickname(nickname: String) {
        context.dataStore.edit { p -> p[Keys.CID_NICKNAME] = nickname }
    }

    suspend fun mergeCidData(auth: CidAuthData) {
        context.dataStore.edit { p ->
            val current = decodeSavedPosts(p[Keys.SAVED_POSTS])
            val byKey = current.associateBy { it.key }.toMutableMap()
            auth.savedPosts.forEach { rawTitle ->
                val title = rawTitle.trim()
                if (title.isBlank()) return@forEach
                val key = title.lowercase()
                if (key !in byKey) {
                    byKey[key] = SavedPost(
                        title = title,
                        source = "CuteID",
                        published = 0L,
                    )
                }
            }
            p[Keys.SAVED_POSTS] = encodeSavedPosts(byKey.values.sortedByDescending { it.published })
            val sources = (p[Keys.SAVED_SOURCES].orEmpty() + auth.savedSources.filter {
                it.startsWith("http://") || it.startsWith("https://")
            }).toSet()
            p[Keys.SAVED_SOURCES] = sources
            p[Keys.CID_NICKNAME] = auth.nickname
            p[Keys.CID_TG_ID] = auth.tgId
            p[Keys.CID_LINKED] = true
            p.remove(Keys.CID_SESSION)
        }
    }

    private fun decodeSavedPosts(raw: String?): List<SavedPost> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val title = obj.optString("title").trim()
                if (title.isBlank()) return@mapNotNull null
                SavedPost(
                    title = title,
                    link = obj.optString("link"),
                    source = obj.optString("source"),
                    imageUrl = obj.optString("image").takeIf { it.isNotBlank() },
                    published = obj.optLong("published", 0L),
                    region = runCatching { Region.valueOf(obj.optString("region")) }
                        .getOrDefault(Region.ALL),
                    feedUrl = obj.optString("feed_url"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeSavedPosts(posts: List<SavedPost>): String {
        val array = JSONArray()
        posts.forEach { post ->
            array.put(
                JSONObject()
                    .put("title", post.title)
                    .put("link", post.link)
                    .put("source", post.source)
                    .put("image", post.imageUrl ?: "")
                    .put("published", post.published)
                    .put("region", post.region.name)
                    .put("feed_url", post.feedUrl)
            )
        }
        return array.toString()
    }

    private fun decodeSources(raw: String?): List<FeedSource> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val url = obj.optString("url")
                if (url.isBlank()) return@mapNotNull null
                val region = runCatching { Region.valueOf(obj.optString("region")) }.getOrDefault(Region.ALL)
                val district = runCatching {
                    FederalDistrict.valueOf(obj.optString("district"))
                }.getOrDefault(region.district)
                FeedSource(
                    name = obj.optString("name").ifBlank { "Свой источник" },
                    url = url,
                    region = region,
                    district = district
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeSources(sources: List<FeedSource>): String {
        val array = JSONArray()
        sources.forEach { source ->
            array.put(
                JSONObject()
                    .put("name", source.name)
                    .put("url", source.url)
                    .put("region", source.region.name)
                    .put("district", source.district.name)
            )
        }
        return array.toString()
    }
}
