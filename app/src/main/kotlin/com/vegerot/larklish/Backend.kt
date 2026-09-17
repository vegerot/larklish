package com.vegerot.larklish

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * The Backend's answer to one Update request. `source` says whether `message` came from the Preview
 * or the Full text. A null title or Sender means the phone keeps its ML Kit result.
 */
data class Lookup(
    val outcome: String,
    val reason: String,
    val source: String,
    val msgType: String,
    val message: String,
    val english: String?,
    val title: String?,
    val sender: String?,
    val failures: List<String>,
    val backend: String,
    val timings: org.json.JSONArray,
)

/**
 * The Backend (Layer 7): complete Previews translate directly; cut Previews run the Lookup and
 * translate the Full text. Only the latter needs the phone's user token. Blocking: call on
 * `Dispatchers.IO`. Throws [IOException] when the Backend cannot be reached or answers anything but
 * 200; the listener records that as `error: …`, like a failed fetch.
 */
object Backend {
    fun lookup(
        context: Context,
        title: String,
        text: String,
        whenMs: Long,
        userToken: String?,
        flowId: String = "",
        timing: FlowTiming? = null,
    ): Lookup {
        val body = JSONObject().put("title", title).put("text", text).put("flowId", flowId)
        if (userToken != null) {
            body.put("whenMs", whenMs).put("userToken", userToken)
        }
        return post(BackendSettings(context).url, body, timing)
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
                    json.optString("source"),
                    json.optString("msgType"),
                    json.optString("message"),
                    if (json.isNull("english")) null
                    else json.getString("english"), // optString would read JSON null as "null"
                    if (json.isNull("title")) null else json.getString("title"),
                    if (json.isNull("sender")) null else json.getString("sender"),
                    List(json.optJSONArray("failures")?.length() ?: 0) { index ->
                        json.getJSONArray("failures").getJSONObject(index).let { failure ->
                            "${failure.getString("field")}:${failure.getString("reason")}"
                        }
                    },
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
