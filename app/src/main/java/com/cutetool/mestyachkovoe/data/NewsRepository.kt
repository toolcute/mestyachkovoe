package com.cutetool.mestyachkovoe.data

import android.text.Html
import android.util.Xml
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

class NewsRepository {
    fun cached(region: Region, settings: UserSettings? = null): List<NewsItem>? {
        val sources = selectSources(region, settings)
        val key = cacheKey(region, settings, sources)
        val entry = synchronized(cache) { cache[key] } ?: return null
        if (System.currentTimeMillis() - entry.timestamp > CACHE_TTL_MS) return null
        return applyFilters(entry.items, region, settings)
    }

    suspend fun load(
        region: Region,
        settings: UserSettings? = null,
        forceRefresh: Boolean = false
    ): List<NewsItem> = withContext(Dispatchers.IO) {
        val sources = selectSources(region, settings)
        val key = cacheKey(region, settings, sources)

        if (!forceRefresh) {
            val entry = synchronized(cache) { cache[key] }
            if (entry != null && System.currentTimeMillis() - entry.timestamp <= CACHE_TTL_MS) {
                return@withContext applyFilters(entry.items, region, settings)
            }
        }

        val items = coroutineScope {
            sources.map { src ->
                async {
                    try {
                        fetchFeed(src)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }

        currentCoroutineContext().ensureActive()

        val sorted = items
            .distinctBy { it.link.ifBlank { it.id } + "|" + it.title }
            .sortedByDescending { it.published }

        val deduped = dedupe(sorted)
        synchronized(cache) {
            cache[key] = CacheEntry(deduped, System.currentTimeMillis())
        }
        applyFilters(deduped, region, settings)
    }

    suspend fun search(query: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val source = FeedSource(
            name = "Google Новости",
            url = "https://news.google.com/rss/search?q=$encoded&hl=ru&gl=RU&ceid=RU:ru",
            region = Region.ALL
        )
        try {
            fetchFeed(source)
                .distinctBy { it.link.ifBlank { it.id } + "|" + it.title }
                .sortedByDescending { it.published }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Loads a single source for the "Любимое" mini-feed.
     * No smart filtering or cross-source dedupe is applied here.
     */
    suspend fun loadSource(url: String, customSources: List<FeedSource> = emptyList()): List<NewsItem> =
        withContext(Dispatchers.IO) {
            val clean = url.trim()
            if (clean.isBlank()) return@withContext emptyList()
            val source = (Sources.all + customSources).firstOrNull { it.url == clean }
                ?: FeedSource(
                    name = sourceNameFromUrl(clean),
                    url = clean,
                    region = Region.ALL
                )
            try {
                fetchFeed(source).sortedByDescending { it.published }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
        }

    private fun sourceNameFromUrl(url: String): String = runCatching {
        val host = URL(url).host.removePrefix("www.")
        host.ifBlank { url }
    }.getOrDefault(url)

    private fun selectSources(region: Region, settings: UserSettings?): List<FeedSource> {
        val disabled = settings?.disabledSources.orEmpty()
        val district = settings?.federalDistrict ?: FederalDistrict.URAL
        return (Sources.all + settings?.customSources.orEmpty())
            .filter { it.district == district }
            .filter { region == Region.ALL || it.region == region || it.region == Region.ALL }
            .filter { it.url !in disabled }
    }

    private fun cacheKey(region: Region, settings: UserSettings?, sources: List<FeedSource>): String {
        val district = settings?.federalDistrict ?: FederalDistrict.URAL
        return district.name + "|" + region.name + "|" + sources.map { it.url }.sorted().joinToString(",")
    }

    private fun applyFilters(items: List<NewsItem>, region: Region, settings: UserSettings?): List<NewsItem> {
        val smart = settings?.smartFilter ?: true
        val svo = settings?.filterSvoPolitics ?: true
        val adult = settings?.filterAdult ?: true
        return items.filterNot { item ->
            val text = item.title + " " + item.content.take(1200)
            (smart && StopWords.blocksRegion(region, text)) ||
                (svo && StopWords.blocksSvoPolitics(text)) ||
                (adult && StopWords.blocksAdult(text))
        }
    }

    private fun dedupe(items: List<NewsItem>): List<NewsItem> {
        val kept = ArrayList<NewsItem>(items.size)
        val signatures = ArrayList<Set<String>>(items.size)
        val tokenIndex = HashMap<String, MutableList<Int>>()
        for (item in items) {
            val sig = signature(item)
            if (sig.isEmpty()) {
                kept.add(item)
                signatures.add(sig)
                continue
            }

            val sharedTokens = HashMap<Int, Int>()
            for (token in sig) {
                tokenIndex[token]?.forEach { index ->
                    sharedTokens[index] = (sharedTokens[index] ?: 0) + 1
                }
            }

            var duplicate = false
            for ((index, shared) in sharedTokens) {
                if (shared < 2) continue
                if (jaccard(sig, signatures[index]) >= 0.5) {
                    duplicate = true
                    break
                }
            }
            if (duplicate) continue

            val newIndex = kept.size
            kept.add(item)
            signatures.add(sig)
            for (token in sig) {
                tokenIndex.getOrPut(token) { ArrayList() }.add(newIndex)
            }
        }
        return kept
    }

    private fun signature(item: NewsItem): Set<String> {
        val snippet = item.content.take(300)
        val text = (item.title + " " + snippet).lowercase(Locale.ROOT)
        return text.split(Regex("[^\\p{L}\\p{N}]+"))
            .asSequence()
            .filter { it.length >= 3 }
            .toSet()
    }

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        var intersection = 0
        val (small, large) = if (a.size <= b.size) a to b else b to a
        for (w in small) if (large.contains(w)) intersection++
        val union = a.size + b.size - intersection
        return if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
    }

    private fun fetchFeed(source: FeedSource): List<NewsItem> {
        val conn = (URL(source.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 8000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mestyachkovoe/1.20 (+https://github.com/toolcute/mestyachkovoe)")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
            setRequestProperty("Accept-Encoding", "gzip")
        }
        try {
            val encoding = conn.contentEncoding
            val raw = conn.inputStream
            val stream = if (encoding?.contains("gzip", ignoreCase = true) == true) {
                GZIPInputStream(raw)
            } else {
                raw
            }
            stream.use { return parse(it, source) }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(stream: InputStream, source: FeedSource): List<NewsItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(stream, null)
        val items = mutableListOf<NewsItem>()
        var event = parser.eventType
        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var updatedDate = ""
        var guid = ""
        var description = ""
        var content = ""
        var itemSource = ""
        var image: String? = null

        while (event != XmlPullParser.END_DOCUMENT) {
            val name = parser.name?.lowercase(Locale.ROOT)
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (name == "item" || name == "entry") {
                        inItem = true
                        title = ""; link = ""; pubDate = ""; updatedDate = ""; guid = ""
                        description = ""; content = ""; itemSource = ""; image = null
                    } else if (inItem) {
                        when (name) {
                            "title" -> title = parser.nextTextOrEmpty()
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                val text = parser.nextTextOrEmpty()
                                link = href ?: text
                            }
                            "pubdate", "published" -> {
                                if (pubDate.isBlank()) pubDate = parser.nextTextOrEmpty()
                                else parser.nextTextOrEmpty()
                            }
                            "updated" -> {
                                if (updatedDate.isBlank()) updatedDate = parser.nextTextOrEmpty()
                                else parser.nextTextOrEmpty()
                            }
                            "date" -> {
                                // dc:date and similar namespaced dates
                                if (pubDate.isBlank()) pubDate = parser.nextTextOrEmpty()
                                else parser.nextTextOrEmpty()
                            }
                            "guid", "id" -> guid = parser.nextTextOrEmpty()
                            "description" -> description = parser.nextTextOrEmpty()
                            "summary" -> if (description.isBlank()) description = parser.nextTextOrEmpty()
                            "source" -> itemSource = parser.nextTextOrEmpty()
                            "encoded" -> content = parser.nextTextOrEmpty()
                            "content" -> {
                                val url = parser.getAttributeValue(null, "url") ?: parser.getAttributeValue(null, "href")
                                if (!url.isNullOrBlank()) image = image ?: url
                                val text = parser.nextTextOrEmpty()
                                if (text.isNotBlank()) content = text
                            }
                            "enclosure" -> {
                                val type = parser.getAttributeValue(null, "type") ?: ""
                                if (type.startsWith("image")) image = image ?: parser.getAttributeValue(null, "url")
                            }
                            "thumbnail" -> {
                                val url = parser.getAttributeValue(null, "url") ?: parser.getAttributeValue(null, "href")
                                if (!url.isNullOrBlank()) image = image ?: url
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if ((name == "item" || name == "entry") && inItem) {
                        inItem = false
                        val rawBody = content.ifBlank { description }
                        val img = image ?: extractImage(rawBody)
                        val displaySource = itemSource.trim().ifBlank { source.name }
                        val rawTitle = cleanHtml(title)
                        val cleanTitle = if (itemSource.isNotBlank() && rawTitle.endsWith(" - $displaySource")) {
                            rawTitle.removeSuffix(" - $displaySource").trim()
                        } else {
                            rawTitle
                        }
                        val cleanLink = link.trim()
                        if (cleanTitle.isNotBlank() && cleanLink.isNotBlank()) {
                            val realPublished = parseDate(pubDate)
                                ?: parseDate(updatedDate)
                                ?: parseGuidDate(guid)
                                ?: 0L
                            items.add(
                                NewsItem(
                                    id = cleanLink,
                                    title = cleanTitle,
                                    link = cleanLink,
                                    imageUrl = img,
                                    source = displaySource,
                                    published = realPublished,
                                    region = source.region,
                                    content = cleanHtml(rawBody),
                                    sourceVerified = source.verified,
                                    feedUrl = source.url,
                                )
                            )
                        }
                        if (items.size >= MAX_ITEMS_PER_FEED) break
                    }
                }
            }
            event = parser.next()
        }
        return items
    }

    private fun XmlPullParser.nextTextOrEmpty(): String = try { nextText() ?: "" } catch (_: Exception) { "" }

    private fun extractImage(html: String): String? {
        val regex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.getOrNull(1)
    }

    private fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""
        val withBreaks = html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p\\s*>"), "\n\n")
            .replace(Regex("(?i)<li[^>]*>"), "• ")
            .replace(Regex("(?i)</(div|li|h[1-6])\\s*>"), "\n")
        val text = runCatching {
            Html.fromHtml(withBreaks, Html.FROM_HTML_MODE_LEGACY).toString()
        }.getOrElse {
            withBreaks.replace(Regex("<[^>]+>"), " ")
        }
        return text
            .replace('\u00A0', ' ')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n[ \\t]+"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun parseDate(raw: String): Long? {
        val value = raw.trim()
        if (value.isBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, d MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd.MM.yyyy HH:mm:ss",
            "dd.MM.yyyy HH:mm"
        )
        for (p in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US)
                fmt.isLenient = false
                fmt.timeZone = when {
                    p.contains("'Z'") -> TimeZone.getTimeZone("UTC")
                    p.contains("Z") || p.contains("z") || p.contains("X") ->
                        TimeZone.getTimeZone("UTC")
                    else -> TimeZone.getTimeZone("Europe/Moscow")
                }
                val d = fmt.parse(value)
                if (d != null) return d.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * Old Mestechko posts have no <pubDate>. Their guid carries the real local
     * publication time, for example: mestechko-26.11.2026 16:23-0 (UTC+5).
     */
    private fun parseGuidDate(guid: String): Long? {
        if (guid.isBlank()) return null
        val match = Regex(
            """mestechko-(\d{2})\.(\d{2})\.(\d{4})\s+(\d{2}):(\d{2})""",
            RegexOption.IGNORE_CASE
        ).find(guid) ?: return null
        val value = buildString {
            append(match.groupValues[1]).append('.')
            append(match.groupValues[2]).append('.')
            append(match.groupValues[3]).append(' ')
            append(match.groupValues[4]).append(':')
            append(match.groupValues[5])
        }
        return try {
            val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("GMT+5")
            }
            fmt.parse(value)?.time
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
        private const val MAX_CACHE_ENTRIES = 8
        private const val MAX_ITEMS_PER_FEED = 40

        private data class CacheEntry(val items: List<NewsItem>, val timestamp: Long)

        private val cache = object : LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean =
                size > MAX_CACHE_ENTRIES
        }

        fun clearCache() {
            synchronized(cache) { cache.clear() }
        }
    }
}
