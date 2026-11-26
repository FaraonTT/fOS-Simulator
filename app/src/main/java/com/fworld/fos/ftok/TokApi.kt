package com.fworld.fos.ftok

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Видео с сервера fTok (https://github.com/FaraonTT/fTok-Server) */
data class TokVideo(
    val id: String,
    val user: String,
    val title: String,
    val url: String,
    val likes: Int,
    val created: Long,
)

/** Клиент API сервера fTok. Заглушек нет: если сервер не задан или пуст — лента пустая. */
class TokApi(base: String) {
    private val base = base.trim().trimEnd('/')

    val configured get() = base.startsWith("http://") || base.startsWith("https://")

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    suspend fun list(query: String = "", user: String = ""): List<TokVideo> = withContext(Dispatchers.IO) {
        val u = "$base/api/videos?q=${enc(query)}&user=${enc(user)}&limit=50"
        val c = open(u, "GET")
        try {
            check(c.responseCode == 200) { "HTTP ${c.responseCode}" }
            val arr = JSONObject(c.inputStream.bufferedReader().readText()).getJSONArray("videos")
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val id = o.getString("id")
                TokVideo(
                    id = id,
                    user = o.optString("user", "anon"),
                    title = o.optString("title", ""),
                    url = "$base/v/$id",
                    likes = o.optInt("likes", 0),
                    created = o.optLong("created", 0),
                )
            }
        } finally { c.disconnect() }
    }

    suspend fun like(id: String) = withContext(Dispatchers.IO) {
        val c = open("$base/api/videos/$id/like", "POST")
        try { c.responseCode } finally { c.disconnect() }
    }

    /** Загрузка видео потоком; onProgress 0..1. Возвращает id нового видео. */
    suspend fun upload(
        cr: ContentResolver,
        uri: Uri,
        user: String,
        title: String,
        sizeBytes: Long,
        onProgress: (Float) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val c = open("$base/api/videos?user=${enc(user)}&title=${enc(title)}", "POST")
        c.doOutput = true
        c.setRequestProperty("Content-Type", "video/mp4")
        if (sizeBytes > 0) c.setFixedLengthStreamingMode(sizeBytes) else c.setChunkedStreamingMode(64 * 1024)
        try {
            val input: InputStream = cr.openInputStream(uri) ?: error("Не удалось открыть файл")
            input.use { ins ->
                c.outputStream.use { out ->
                    val buf = ByteArray(64 * 1024)
                    var sent = 0L
                    while (true) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        sent += n
                        if (sizeBytes > 0) onProgress((sent.toFloat() / sizeBytes).coerceIn(0f, 1f))
                    }
                }
            }
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty()
            check(code in 200..299) { "HTTP $code ${body.take(120)}" }
            JSONObject(body).optString("id")
        } finally { c.disconnect() }
    }

    private fun open(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 60_000
        }
}
