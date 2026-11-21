package com.cutetool.mestyachkovoe.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
    val launcherIcon: LauncherIcon = LauncherIcon.NEW,
    val smartFilter: Boolean = true,
    val filterSvoPolitics: Boolean = true,
    val filterAdult: Boolean = true,
    val disabledSources: Set<String> = emptySet(),
    val customSources: List<FeedSource> = emptyList(),
    val federalDistrict: FederalDistrict = FederalDistrict.URAL,
    val cacheAutoClean: CacheAutoClean = CacheAutoClean.OFF,
    val onboardingDone: Boolean = false,
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
            launcherIcon = runCatching { LauncherIcon.valueOf(p[Keys.LAUNCHER_ICON] ?: "") }.getOrDefault(LauncherIcon.NEW),
            smartFilter = p[Keys.SMART_FILTER] ?: true,
            filterSvoPolitics = p[Keys.FILTER_SVO] ?: true,
            filterAdult = p[Keys.FILTER_ADULT] ?: true,
            disabledSources = p[Keys.DISABLED].orEmpty(),
            customSources = decodeSources(p[Keys.CUSTOM]),
            federalDistrict = runCatching {
                FederalDistrict.valueOf(p[Keys.FEDERAL_DISTRICT] ?: "")
            }.getOrDefault(FederalDistrict.URAL),
            cacheAutoClean = runCatching {
                CacheAutoClean.valueOf(p[Keys.CACHE_AUTO_CLEAN] ?: "")
            }.getOrDefault(CacheAutoClean.OFF),
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
        )
    }

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
