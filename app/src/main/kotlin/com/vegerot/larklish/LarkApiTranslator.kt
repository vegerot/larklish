package com.vegerot.larklish

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Lark ran its own detection and decided there was nothing to translate. A retry cannot change
 * that.
 */
class NotTranslated : IOException("Lark returned the input unchanged")

/**
 * Lark's own translation engine (Experiment 05). Throws on any failure; [FallbackTranslator]
 * retries once, then turns that into an ML Kit result.
 */
class LarkApiTranslator(private val appId: String, private val appSecret: String) : Translator {
    private var token = ""
    private var tokenExpiresAt = 0L // epoch ms; 0 forces a fetch

    override suspend fun zhToEn(text: String): String =
        withContext(Dispatchers.IO) {
            val out =
                try {
                    LarkHttp.postJson(
                            "/open-apis/translation/v1/text/translate",
                            JSONObject()
                                .put("source_language", "zh")
                                .put("target_language", "en")
                                .put("text", text),
                            token(),
                        )
                        .getJSONObject("data")
                        .getString("text")
                } catch (e: IOException) {
                    tokenExpiresAt = 0 // a rejected token is refetched on the next call
                    throw e
                }
            // Lark runs its own language detection and returns Latin-heavy text untouched
            // (Experiment 05). The caller only sends Han text, so "unchanged" means "not
            // translated".
            if (out == text) throw NotTranslated()
            out
        }

    private fun token(): String {
        if (System.currentTimeMillis() < tokenExpiresAt) return token
        val body =
            LarkHttp.postJson(
                "/open-apis/auth/v3/tenant_access_token/internal",
                JSONObject().put("app_id", appId).put("app_secret", appSecret),
            )
        token = body.getString("tenant_access_token")
        // Renew a minute early so a call never starts with a token that expires mid-flight.
        tokenExpiresAt = System.currentTimeMillis() + (body.getLong("expire") - 60) * 1000
        return token
    }
}
