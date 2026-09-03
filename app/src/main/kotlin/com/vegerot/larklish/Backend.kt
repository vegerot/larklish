package com.vegerot.larklish

import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * The Backend's answer to one Lookup (Layer 7). `english` is null when Lark would not translate the
 * Full text.
 */
data class Lookup(
    val outcome: String,
    val reason: String,
    val msgType: String,
    val fullText: String,
    val english: String?,
)

/**
 * The Backend (Layer 7): the Lookup — title → chat id → newest messages → pick — and the Full-text
 * translate run there, under the user token the phone holds and sends along. Blocking: call on
 * `Dispatchers.IO`. Throws [IOException] when the Backend cannot be reached or answers anything but
 * 200; the listener records that as `error: …`, like a failed fetch.
 */
object Backend {
    /**
     * Where the Backend is: on the phone's own port when it hangs on USB (`adb reverse tcp:8787
     * tcp:8787`, which `tools/larklish-helper probe` sets), else on the Mac's address from
     * `local.properties`. A refused connection moves on to the next.
     */
    private val urls = listOf("http://127.0.0.1:8787", BuildConfig.LARKLISH_BACKEND_URL)

    fun lookup(title: String, text: String, whenMs: Long, userToken: String): Lookup {
        val body =
            JSONObject()
                .put("title", title)
                .put("text", text)
                .put("whenMs", whenMs)
                .put("userToken", userToken)
        var refused: ConnectException? = null
        for (url in urls) {
            try {
                return post(url, body)
            } catch (e: ConnectException) {
                refused = e
            }
        }
        throw refused!!
    }

    private fun post(url: String, body: JSONObject): Lookup {
        val conn = URL("$url/lookup").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 5_000
        conn.readTimeout =
            30_000 // a DM Lookup polls the search index for up to 12 s, then reads the chat
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val status = conn.responseCode
        val answer =
            (if (status == 200) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.readText()
                .orEmpty()
        if (status != 200) throw IOException("Backend /lookup: http $status ${answer.trim()}")
        val json = JSONObject(answer)
        return Lookup(
            json.getString("outcome"),
            json.optString("reason"),
            json.optString("msgType"),
            json.optString("fullText"),
            if (json.isNull("english")) null
            else json.getString("english"), // optString would read JSON null as "null"
        )
    }
}
