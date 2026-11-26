package com.cutetool.mestyachkovoe.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

data class CidAuthData(
    val tgId: Long,
    val nickname: String,
    val savedPosts: List<String>,
    val savedSources: List<String>,
)

/**
 * CuteID (CID) authorization and sync over the Telegram bot.
 * No external server is needed: the bot carries the auth marker and the sync payload.
 */
object CuteIdRepository {
    const val BOT_TOKEN = "9151476410:AAGACgh6qbvBAbcEjDwHNGwELvowCdOtGcs"
    const val BOT_USERNAME_FALLBACK = "CuteIDBot"

    private const val API_BASE = "https://api.telegram.org/bot$BOT_TOKEN"
    private const val AUTH_MARKER = "CID_DATA:"
    private const val SESSION_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    private const val USER_AGENT = "Mestyachkovoe/1.19 (+https://github.com/toolcute/mestyachkovoe)"

    private val nicknameRegex = Regex("^[a-zA-Z]{4,9}$")
    private var cachedBotUsername: String? = null

    fun isValidNickname(nickname: String): Boolean = nicknameRegex.matches(nickname.trim())

    fun newSessionToken(): String =
        (1..8).map { SESSION_ALPHABET[Random.nextInt(SESSION_ALPHABET.length)] }.joinToString("")

    suspend fun botUsername(): String = withContext(Dispatchers.IO) {
        cachedBotUsername?.let { return@withContext it }
        val username = runCatching {
            val json = getJson("$API_BASE/getMe")
            json.optJSONObject("result")?.optString("username").orEmpty()
        }.getOrDefault("")
        val resolved = username.ifBlank { BOT_USERNAME_FALLBACK }
        cachedBotUsername = resolved
        resolved
    }

    fun authUrl(username: String, session: String, nickname: String): String =
        "https://t.me/$username?start=auth_${session}_$nickname"

    /**
     * Looks through recent bot updates for the auth marker matching [session].
     */
    suspend fun pollForAuth(session: String): CidAuthData? = withContext(Dispatchers.IO) {
        val json = runCatching { getJson("$API_BASE/getUpdates?limit=100&timeout=0") }
            .getOrNull() ?: return@withContext null
        val updates = json.optJSONArray("result") ?: return@withContext null
        for (i in 0 until updates.length()) {
            val update = updates.optJSONObject(i) ?: continue
            val message = update.optJSONObject("message")
                ?: update.optJSONObject("edited_message")
                ?: continue
            val text = message.optString("text")
            val markerIndex = text.indexOf(AUTH_MARKER)
            if (markerIndex < 0) continue
            val payload = extractJsonObject(text.substring(markerIndex + AUTH_MARKER.length))
                ?: continue
            val obj = runCatching { JSONObject(payload) }.getOrNull() ?: continue
            if (!obj.optBoolean("cid_auth")) continue
            if (obj.optString("session") != session) continue
            val tgId = obj.optLong("tg_id", 0L)
                .takeIf { it > 0L }
                ?: message.optJSONObject("from")?.optLong("id", 0L)
                ?: 0L
            if (tgId <= 0L) continue
            val nickname = obj.optString("nickname").trim()
                .ifBlank { obj.optJSONObject("from")?.optString("username").orEmpty() }
            return@withContext CidAuthData(
                tgId = tgId,
                nickname = nickname,
                savedPosts = toStringList(obj.optJSONArray("saved_posts")),
                savedSources = toStringList(obj.optJSONArray("saved_sources")),
            )
        }
        null
    }

    /**
     * Pushes the current CID_SYNC package into the bot chat so it can update
     * toolcute/CuteID/users.json.
     */
    suspend fun sendSync(settings: UserSettings): Boolean = withContext(Dispatchers.IO) {
        val nickname = settings.cidNickname?.takeIf { it.isNotBlank() } ?: return@withContext false
        val tgId = settings.cidTgId ?: return@withContext false
        if (tgId <= 0L) return@withContext false

        val payload = JSONObject()
            .put("nickname", nickname)
            .put("saved_posts", JSONArray(settings.savedPosts.map { it.title }))
            .put("saved_sources", JSONArray(settings.savedSources.toList()))
        val text = "CID_SYNC:$payload"

        runCatching {
            val body = buildString {
                append("chat_id=").append(URLEncoder.encode(tgId.toString(), "UTF-8"))
                append("&text=").append(URLEncoder.encode(text, "UTF-8"))
                append("&disable_web_page_preview=true")
            }
            val json = postForm("$API_BASE/sendMessage", body)
            json.optBoolean("ok")
        }.getOrDefault(false)
    }

    private fun toStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optString(index).trim().takeIf { it.isNotBlank() }
        }
    }

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until raw.length) {
            val ch = raw[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    ch == '\\' -> escaped = true
                    ch == '"' -> inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 12000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("Telegram API вернул код $code")
            return JSONObject(raw)
        } finally {
            connection.disconnect()
        }
    }

    private fun postForm(url: String, body: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 12000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("Telegram API вернул код $code")
            return JSONObject(raw)
        } finally {
            connection.disconnect()
        }
    }
}
