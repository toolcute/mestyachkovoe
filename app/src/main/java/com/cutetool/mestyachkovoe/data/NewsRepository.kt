package com.cutetool.mestyachkovoe.data

import android.text.Html
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

class NewsRepository {
    suspend fun load(region: Region, settings: UserSettings? = null): List<NewsItem> = withContext(Dispatchers.IO) {
        val disabled = settings?.disabledSources.orEmpty()
        val district = settings?.federalDistrict ?: FederalDistrict.URAL
        val sources = (Sources.all + settings?.customSources.orEmpty())
            .filter { it.url !in disabled }
            .filter { it.district == district }
            .filter { region == Region.ALL || it.region == region || it.region == Region.ALL }

        val items = coroutineScope {
            sources.map { src ->
                async {
                    runCatching { fetchFeed(src) }.getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
        }

        val sorted = items
            .distinctBy { it.link.ifBlank { it.id } + "|" + it.title }
            .sortedByDescending { it.published }

        val deduped = dedupe(sorted)

        deduped.filterNot { item ->
            val text = item.title + " " + item.content.take(1200)
            val smart = settings?.smartFilter ?: true
            val svo = settings?.filterSvoPolitics ?: true
            val adult = settings?.filterAdult ?: true
            (smart && StopWords.blocksRegion(region, text)) ||
                (svo && StopWords.blocksSvoPolitics(text)) ||
                (adult && StopWords.blocksAdult(text))
        }
    }

    suspend fun search(query: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val source = FeedSource(
            name = "Google Новости",
            url = "https://news.google.com/rss/search?q=$encoded&hl=ru&gl=RU&ceid=RU:ru",
            region = Region.ALL
        )
        runCatching { fetchFeed(source) }.getOrDefault(emptyList())
            .distinctBy { it.link.ifBlank { it.id } + "|" + it.title }
            .sortedByDescending { it.published }
    }

    private fun dedupe(items: List<NewsItem>): List<NewsItem> {
        val kept = ArrayList<NewsItem>(items.size)
        val signatures = ArrayList<Set<String>>(items.size)
        for (item in items) {
            val sig = signature(item)
            var duplicate = false
            for (existing in signatures) {
                if (jaccard(sig, existing) >= 0.5) {
                    duplicate = true
                    break
                }
            }
            if (!duplicate) {
                kept.add(item)
                signatures.add(sig)
            }
        }
        return kept
    }

    private fun signature(item: NewsItem): Set<String> {
        val snippet = item.content.take(500)
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
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "Mestyachkovoe/1.16")
        }
        conn.inputStream.use { stream ->
            return parse(stream, source)
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
                        title = ""; link = ""; pubDate = ""; description = ""; content = ""; itemSource = ""; image = null
                    } else if (inItem) {
                        when (name) {
                            "title" -> title = parser.nextTextOrEmpty()
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                val text = parser.nextTextOrEmpty()
                                link = href ?: text
                            }
                            "pubdate", "published", "updated" -> pubDate = parser.nextTextOrEmpty()
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
                            items.add(
                                NewsItem(
                                    id = cleanLink,
                                    title = cleanTitle,
                                    link = cleanLink,
                                    imageUrl = img,
                                    source = displaySource,
                                    published = parseDate(pubDate),
                                    region = source.region,
                                    content = cleanHtml(rawBody),
                                    sourceVerified = source.verified,
                                )
                            )
                        }
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

    private fun parseDate(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
            "dd MMM yyyy HH:mm:ss Z"
        )
        for (p in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US)
                if (p.contains("'Z'")) fmt.timeZone = TimeZone.getTimeZone("UTC")
                val d = fmt.parse(raw.trim())
                if (d != null) return d.time
            } catch (_: Exception) { }
        }
        return System.currentTimeMillis()
    }
}