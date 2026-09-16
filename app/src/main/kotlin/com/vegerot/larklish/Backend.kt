package com.vegerot.larklish

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * The Backend's answer to one Lookup (Layer 7). `english` is null when Lark would not translate the
 * Full text. `backend` is the HTTPS URL that answered, so the record shows which Backend served
 * each Update.
 */
data class Lookup(
    val outcome: String,
    val reason: String,
    val msgType: String,
    val fullText: String,
    val english: String?,
    val backend: String,
    val timings: org.json.JSONArray,
)

/**
 * The Backend (Layer 7): the Lookup — title → chat id → newest messages → pick — and the Full-text
 * translate run there, under the user token the phone holds and sends along. Blocking: call on
 * `Dispatchers.IO`. Throws [IOException] when the Backend cannot be reached or answers anything but
 * 200; the listener records that as `error: …`, like a failed fetch.
 */
object Backend {
    fun lookup(
        title: String,
        text: String,
        whenMs: Long,
        userToken: String,
        flowId: String = "",
        timing: FlowTiming? = null,
    ): Lookup {
        val body =
            JSONObject()
                .put("title", title)
                .put("text", text)
                .put("whenMs", whenMs)
                .put("userToken", userToken)
                .put("flowId", flowId)
        return post(BuildConfig.LARKLISH_BACKEND_URL, body, timing)
    }

    private fun post(url: String, body: JSONObject, timing: FlowTiming?): Lookup {
        val started = System.nanoTime()
        var status = 0
        var failed = true
        try {
            val conn = URL("$url/lookup").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5_000
            conn.readTimeout =
                30_000 // a DM Lookup polls the search index for up to 12 s, then reads the chat
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.LARKLISH_BACKEND_TOKEN}")
            if (BuildConfig.LARKLISH_BACKEND_TT_ENV.isNotEmpty()) {
                conn.setRequestProperty("x-tt-env", BuildConfig.LARKLISH_BACKEND_TT_ENV)
            }
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            status = conn.responseCode
            val answer =
                (if (status == 200) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.readText()
                    .orEmpty()
            if (status != 200) throw IOException("Backend /lookup: http $status ${answer.trim()}")
            val json = JSONObject(answer)
            val result =
                Lookup(
                    json.getString("outcome"),
                    json.optString("reason"),
                    json.optString("msgType"),
                    json.optString("fullText"),
                    if (json.isNull("english")) null
                    else json.getString("english"), // optString would read JSON null as "null"
                    backend = url,
                    timings = json.optJSONArray("timings") ?: org.json.JSONArray(),
                )
            failed = false
            return result
        } finally {
            timing?.add("POST /lookup", (System.nanoTime() - started) / 1_000_000, failed, status)
        }
    }
}
