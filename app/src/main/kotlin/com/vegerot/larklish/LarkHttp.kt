package com.vegerot.larklish

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val HOST = "https://open.feishu.cn" // Feishu-brand tenant (Experiment 05)

/**
 * The Lark Open API over raw HTTP. Blocking: call it on `Dispatchers.IO`.
 * Every call throws [IOException] on a non-200 status or a non-zero Lark `code`.
 */
object LarkHttp {
    fun postJson(path: String, body: JSONObject, bearer: String? = null): JSONObject =
        request("POST", path, path, bearer) { conn ->
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
        }

    fun getJson(path: String, params: Map<String, String>, bearer: String): JSONObject {
        val query = params.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        return request("GET", path, "$path?$query", bearer) {}
    }

    private fun request(
        method: String,
        path: String,
        pathAndQuery: String,
        bearer: String?,
        send: (HttpURLConnection) -> Unit,
    ): JSONObject {
        val conn = URL(HOST + pathAndQuery).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 5_000
        conn.readTimeout = 10_000
        if (bearer != null) conn.setRequestProperty("Authorization", "Bearer $bearer")
        send(conn)
        val status = conn.responseCode
        val stream = if (status == 200) conn.inputStream else conn.errorStream
        val json = JSONObject(stream.bufferedReader().readText())
        if (status != 200 || json.optInt("code") != 0) {
            throw IOException("Lark $path: http $status code ${json.optInt("code")} ${json.optString("msg")}")
        }
        return json
    }
}
