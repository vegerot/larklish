package com.vegerot.larklish

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val HOST = "https://open.feishu.cn" // Feishu-brand tenant (Experiment 05)

/**
 * Lark's own translation engine over raw HTTP (Experiment 05). Throws on any failure;
 * [FallbackTranslator] turns that into an ML Kit result.
 */
class LarkApiTranslator(private val appId: String, private val appSecret: String) : Translator {
    private var token = ""
    private var tokenExpiresAt = 0L // epoch ms; 0 forces a fetch

    override suspend fun zhToEn(text: String): String = withContext(Dispatchers.IO) {
        val out = try {
            post(
                "/open-apis/translation/v1/text/translate",
                JSONObject().put("source_language", "zh").put("target_language", "en").put("text", text),
                token(),
            ).getJSONObject("data").getString("text")
        } catch (e: IOException) {
            tokenExpiresAt = 0 // a rejected token is refetched on the next call
            throw e
        }
        // Lark runs its own language detection and returns Latin-heavy text untouched
        // (Experiment 05). The caller only sends Han text, so "unchanged" means "not translated".
        if (out == text) throw IOException("Lark returned the input unchanged")
        out
    }

    private fun token(): String {
        if (System.currentTimeMillis() < tokenExpiresAt) return token
        val body = post(
            "/open-apis/auth/v3/tenant_access_token/internal",
            JSONObject().put("app_id", appId).put("app_secret", appSecret),
            token = null,
        )
        token = body.getString("tenant_access_token")
        // Renew a minute early so a call never starts with a token that expires mid-flight.
        tokenExpiresAt = System.currentTimeMillis() + (body.getLong("expire") - 60) * 1000
        return token
    }

    private fun post(path: String, body: JSONObject, token: String?): JSONObject {
        val conn = URL(HOST + path).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 5_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
        conn.doOutput = true
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val status = conn.responseCode
        val stream = if (status == 200) conn.inputStream else conn.errorStream
        val json = JSONObject(stream.bufferedReader().readText())
        if (status != 200 || json.getInt("code") != 0) {
            throw IOException("Lark $path: http $status code ${json.optInt("code")} ${json.optString("msg")}")
        }
        return json
    }
}
