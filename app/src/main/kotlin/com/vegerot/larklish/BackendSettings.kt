package com.vegerot.larklish

import android.content.Context
import java.net.URI

/** The Backend URL is device-local, so changing it does not require rebuilding the app. */
class BackendSettings(context: Context) {
    private val preferences = context.getSharedPreferences("backend", Context.MODE_PRIVATE)

    val url: String
        get() = preferences.getString("url", DEFAULT_URL) ?: DEFAULT_URL

    fun save(input: String): Boolean {
        val url = normalize(input) ?: return false
        preferences.edit().putString("url", url).apply()
        return true
    }

    companion object {
        const val DEFAULT_URL = "https://backend-production-a712b.up.railway.app"

        fun normalize(input: String): String? {
            val value = input.trim().trimEnd('/')
            val uri = runCatching { URI(value) }.getOrNull() ?: return null
            if (
                uri.scheme != "https" ||
                    uri.host.isNullOrEmpty() ||
                    uri.rawQuery != null ||
                    uri.rawFragment != null ||
                    uri.rawUserInfo != null
            )
                return null
            return value
        }
    }
}
