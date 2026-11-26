package com.fworld.fos.ftok

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import android.util.Base64OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Видео из хранилища fTok */
data class TokVideo(
    val id: String,
    val user: String,
    val title: String,
    val url: String,
    val likes: Int,
    val created: Long,
)

class TokAuthException(message: String) : Exception(message)

/**
 * fTok без своего сервера: «сервером» служит GitHub (репозиторий данных [REPO], Contents API).
 * Токен [ghToken] вшивается в сборку из секрета GH_TOKEN (fine-grained, только репозиторий данных).
 * Раскладка репозитория: users/<имя>.json — аккаунт, meta/<id>.json — видео, videos/<id>.mp4 — файл.
 */
class TokApi(private val ghToken: String, val user: String = "") {
    val configured get() = ghToken.isNotEmpty()
    val loggedIn get() = user.isNotEmpty()

    /** Заголовки для ExoPlayer: видео читается из приватного репозитория как raw */
    fun authHeaders(): Map<String, String> =
        mapOf("Authorization" to "Bearer $ghToken", "Accept" to "application/vnd.github.raw", "X-GitHub-Api-Version" to "2022-11-28")

    private fun videoUrl(id: String) = "$API/contents/videos/$id.mp4?ref=$BRANCH"

    private fun open(path: String, method: String, raw: Boolean = false): HttpURLConnection =
        (URL("$API/contents/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer $ghToken")
            setRequestProperty("Accept", if (raw) "application/vnd.github.raw" else "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }

    private fun readBody(c: HttpURLConnection): String =
        (if (c.responseCode in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty()

    private fun putJson(path: String, message: String, content: ByteArray, sha: String? = null): Int {
        val c = open(path, "PUT")
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        try {
            val body = JSONObject().put("message", message).put("branch", BRANCH)
                .put("content", Base64.encodeToString(content, Base64.NO_WRAP))
            if (sha != null) body.put("sha", sha)
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            return c.responseCode
        } finally { c.disconnect() }
    }

    private fun getRaw(path: String): String? {
        val c = open("$path?ref=$BRANCH", "GET", raw = true)
        return try { if (c.responseCode == 200) c.inputStream.bufferedReader().readText() else null } finally { c.disconnect() }
    }

    // ---------- аккаунты ----------

    private fun hash(password: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(), salt, ITER, 256)).encoded

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
    private fun unhex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16) }

    /** Регистрация или вход. Возвращает имя пользователя. */
    suspend fun auth(register: Boolean, name: String, password: String): String = withContext(Dispatchers.IO) {
        if (!configured) throw TokAuthException("Хранилище fTok не настроено в этой сборке")
        if (!Regex("[A-Za-z0-9_]{3,24}").matches(name)) throw TokAuthException("Имя: 3–24 символа, буквы/цифры/_")
        if (password.length < 6) throw TokAuthException("Пароль: от 6 символов")
        val path = "users/${name.lowercase()}.json"
        if (register) {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val rec = JSONObject().put("user", name).put("salt", hex(salt)).put("hash", hex(hash(password, salt)))
                .put("iter", ITER).put("created", System.currentTimeMillis())
            when (val code = putJson(path, "user", rec.toString().toByteArray())) {
                200, 201 -> name
                422 -> throw TokAuthException("Имя уже занято")
                else -> throw TokAuthException("Ошибка GitHub: HTTP $code")
            }
        } else {
            val rec = getRaw(path)?.let { JSONObject(it) } ?: throw TokAuthException("Неверное имя или пароль")
            val ok = java.security.MessageDigest.isEqual(hash(password, unhex(rec.getString("salt"))), unhex(rec.getString("hash")))
            if (!ok) throw TokAuthException("Неверное имя или пароль")
            rec.getString("user")
        }
    }

    // ---------- видео ----------

    private fun parseMeta(json: String): TokVideo {
        val o = JSONObject(json)
        val id = o.getString("id")
        return TokVideo(id, o.optString("user", "anon"), o.optString("title", ""), videoUrl(id),
            o.optJSONArray("likes")?.length() ?: 0, o.optLong("created", 0))
    }

    suspend fun list(query: String = "", user: String = ""): List<TokVideo> = withContext(Dispatchers.IO) {
        val c = open("meta?ref=$BRANCH", "GET")
        val names = try {
            when (c.responseCode) {
                404 -> return@withContext emptyList()
                200 -> JSONArray(c.inputStream.bufferedReader().readText()).let { a -> List(a.length()) { a.getJSONObject(it).getString("name") } }
                else -> error("HTTP ${c.responseCode}")
            }
        } finally { c.disconnect() }
        val filtered = query.isNotBlank() || user.isNotBlank()
        val take = names.filter { it.endsWith(".json") }.sortedDescending().take(if (filtered) 120 else 30)
        coroutineScope {
            take.map { n -> async { runCatching { getRaw("meta/$n")?.let(::parseMeta) }.getOrNull() } }.awaitAll()
        }.filterNotNull().filter { v ->
            (user.isBlank() || v.user.equals(user, true)) &&
                (query.isBlank() || v.title.contains(query, true) || v.user.contains(query, true))
        }
    }

    suspend fun like(id: String) = withContext(Dispatchers.IO) {
        if (!loggedIn) return@withContext
        repeat(3) {
            val c = open("meta/$id.json?ref=$BRANCH", "GET")
            val (sha, json) = try {
                if (c.responseCode != 200) return@withContext
                val j = JSONObject(c.inputStream.bufferedReader().readText())
                j.getString("sha") to String(Base64.decode(j.getString("content"), Base64.DEFAULT))
            } finally { c.disconnect() }
            val o = JSONObject(json)
            val likes = o.optJSONArray("likes") ?: JSONArray()
            if ((0 until likes.length()).any { likes.getString(it) == user }) return@withContext
            likes.put(user); o.put("likes", likes)
            if (putJson("meta/$id.json", "like", o.toString().toByteArray(), sha) in 200..201) return@withContext
        }
    }

    /** Загрузка: видео потоком в base64 прямо в тело запроса (без загрузки файла в память). */
    suspend fun upload(cr: ContentResolver, uri: Uri, title: String, sizeBytes: Long, onProgress: (Float) -> Unit): String =
        withContext(Dispatchers.IO) {
            if (!loggedIn) throw TokAuthException("Войдите в аккаунт")
            val id = System.currentTimeMillis().toString().padStart(13, '0') + "%04x".format(SecureRandom().nextInt(0x10000))
            val c = open("videos/$id.mp4", "PUT")
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setChunkedStreamingMode(64 * 1024)
            try {
                val input: InputStream = cr.openInputStream(uri) ?: error("Не удалось открыть файл")
                c.outputStream.use { out ->
                    out.write("""{"message":"video","branch":"$BRANCH","content":"""".toByteArray())
                    val b64 = Base64OutputStream(out, Base64.NO_WRAP or Base64.NO_CLOSE)
                    input.use { ins ->
                        val buf = ByteArray(48 * 1024)
                        var sent = 0L
                        while (true) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            b64.write(buf, 0, n)
                            sent += n
                            if (sizeBytes > 0) onProgress((sent.toFloat() / sizeBytes).coerceIn(0f, 0.97f))
                        }
                    }
                    b64.close()
                    out.write("\"}".toByteArray())
                }
                val code = c.responseCode
                if (code !in 200..201) error("HTTP $code ${readBody(c).take(120)}")
            } finally { c.disconnect() }
            val meta = JSONObject().put("id", id).put("user", user).put("title", title.take(120))
                .put("created", System.currentTimeMillis()).put("likes", JSONArray())
            val code = putJson("meta/$id.json", "meta", meta.toString().toByteArray())
            check(code in 200..201) { "HTTP $code" }
            onProgress(1f)
            id
        }

    companion object {
        const val REPO = "FaraonTT/fTok-Data"
        const val BRANCH = "main"
        private const val ITER = 150_000
        private const val API = "https://api.github.com/repos/$REPO"
    }
}
