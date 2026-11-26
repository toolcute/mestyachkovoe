package com.cutetool.mestyachkovoe.data

import android.util.Base64
import com.cutetool.mestyachkovoe.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

data class CidAuthData(
    val tgId: Long,
    val nickname: String,
    val savedPosts: List<String>,
    val savedSources: List<String>,
)

/**
 * CuteID (CID) authorization and sync.
 *
 * The Telegram bot on the server consumes /start through getUpdates, so the app
 * reads the session from toolcute/CuteID/users.json through the GitHub Contents
 * API and writes user data back with a PUT request.
 */
object CuteIdRepository {
    const val BOT_TOKEN = "9151476410:AAGACgh6qbvBAbcEjDwHNGwELvowCdOtGcs"
    const val BOT_USERNAME_FALLBACK = "CuteIDBot"

    private const val TELEGRAM_API = "https://api.telegram.org/bot$BOT_TOKEN"
    private const val GITHUB_USERS_URL =
        "https://api.github.com/repos/toolcute/CuteID/contents/users.json"
    private val GITHUB_TOKEN = BuildConfig.CUTEID_GITHUB_TOKEN
    private const val GITHUB_ACCEPT = "application/vnd.github+json"
    private const val SESSION_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    private const val USER_AGENT = "Mestyachkovoe/1.21 (+https://github.com/toolcute/mestyachkovoe)"

    private val nicknameRegex = Regex("^[a-zA-Z]{4,9}$")
    private val syncMutex = Mutex()
    private var cachedBotUsername: String? = null

    private data class UsersFile(val sha: String, val json: JSONObject)

    fun isValidNickname(nickname: String): Boolean = nicknameRegex.matches(nickname.trim())

    fun newSessionToken(): String =
        (1..8).map { SESSION_ALPHABET[Random.nextInt(SESSION_ALPHABET.length)] }.joinToString("")

    suspend fun botUsername(): String = withContext(Dispatchers.IO) {
        cachedBotUsername?.let { return@withContext it }
        val username = runCatching {
            telegramGetJson("$TELEGRAM_API/getMe")
                ?.optJSONObject("result")
                ?.optString("username")
                .orEmpty()
        }.getOrDefault("")
        val resolved = username.ifBlank { BOT_USERNAME_FALLBACK }
        cachedBotUsername = resolved
        resolved
    }

    fun authUrl(username: String, session: String, nickname: String): String =
        "https://t.me/$username?start=auth_${session}_$nickname"

    /**
     * Reads users.json and returns the session entry created by the bot.
     */
    suspend fun pollForAuth(session: String): CidAuthData? = withContext(Dispatchers.IO) {
        val usersFile = fetchUsersFile() ?: return@withContext null
        val sessions = usersFile.json.optJSONObject("sessions") ?: return@withContext null
        val rawEntry = sessions.opt(session) ?: return@withContext null
        val entry = when (rawEntry) {
            is JSONObject -> rawEntry
            is String -> runCatching { JSONObject(rawEntry) }.getOrNull()
            else -> null
        } ?: return@withContext null
        val payload = entry.optJSONObject("data") ?: entry

        val tgId = optLong(payload, "tg_id")
            ?: optLong(entry, "tg_id")
            ?: optLong(payload, "telegram_id")
            ?: 0L
        if (tgId <= 0L) return@withContext null

        val nickname = payload.optString("nickname").trim()
            .ifBlank { entry.optString("nickname").trim() }
        CidAuthData(
            tgId = tgId,
            nickname = nickname,
            savedPosts = toStringList(payload.optJSONArray("saved_posts")),
            savedSources = toStringList(payload.optJSONArray("saved_sources")),
        )
    }

    /**
     * Writes the current CID_SYNC package into json["users"][tg_id].
     */
    suspend fun sendSync(settings: UserSettings): Boolean = withContext(Dispatchers.IO) {
        val nickname = settings.cidNickname?.takeIf { it.isNotBlank() }
            ?: return@withContext false
        val tgId = settings.cidTgId ?: return@withContext false
        if (tgId <= 0L) return@withContext false

        syncMutex.withLock {
            repeat(3) {
                if (pushUserOnce(tgId, nickname, settings)) return@withLock true
            }
            false
        }
    }

    private fun pushUserOnce(tgId: Long, nickname: String, settings: UserSettings): Boolean {
        val usersFile = fetchUsersFile() ?: return false
        val root = usersFile.json
        val users = root.optJSONObject("users") ?: JSONObject().also { root.put("users", it) }
        val user = users.optJSONObject(tgId.toString()) ?: JSONObject()
        user.put("tg_id", tgId)
        user.put("nickname", nickname)
        user.put("saved_posts", JSONArray(settings.savedPosts.map { it.title }))
        user.put("saved_sources", JSONArray(settings.savedSources.toList()))
        user.put("updated_at", System.currentTimeMillis())
        users.put(tgId.toString(), user)

        val encoded = Base64.encodeToString(
            root.toString(2).toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        val body = JSONObject()
            .put("message", "CuteID: sync $nickname")
            .put("content", encoded)
            .put("sha", usersFile.sha)
        val response = githubRequest("PUT", GITHUB_USERS_URL, body) ?: return false
        return response.has("content") || response.has("commit")
    }

    private fun fetchUsersFile(): UsersFile? {
        val json = githubRequest(
            "GET",
            "$GITHUB_USERS_URL?t=${System.currentTimeMillis()}",
            null
        ) ?: return null
        val content = json.optString("content")
        val sha = json.optString("sha")
        if (content.isBlank() || sha.isBlank()) return null
        val raw = runCatching {
            String(Base64.decode(content, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull() ?: return null
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        return UsersFile(sha, root)
    }

    private fun optLong(obj: JSONObject, key: String): Long? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val value = obj.opt(key)
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }?.takeIf { it > 0L }
    }

    private fun toStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optString(index).trim().takeIf { it.isNotBlank() }
        }
    }

    private fun githubRequest(method: String, url: String, body: JSONObject?): JSONObject? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("Authorization", "Bearer $GITHUB_TOKEN")
            setRequestProperty("Accept", GITHUB_ACCEPT)
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("GitHub API вернул код $code")
            return JSONObject(raw)
        } catch (_: Exception) {
            return null
        } finally {
            connection.disconnect()
        }
    }

    private fun telegramGetJson(url: String): JSONObject? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 12000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) null else JSONObject(raw)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
