package com.vegerot.larklish

import android.util.Log
import kotlinx.coroutines.CancellationException

private const val TAG = "FallbackTranslator"

/**
 * [primary] first; [fallback] when it fails. Fallback output gets a leading `~` so the
 * Relay and `events.jsonl` show which engine produced it (Layer 4).
 */
class FallbackTranslator(private val primary: Translator, private val fallback: Translator) : Translator {
    override suspend fun zhToEn(text: String): String = try {
        primary.zhToEn(text)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "primary failed: $e; using fallback")
        "~" + fallback.zhToEn(text)
    }
}

/** Lark's engine with ML Kit as fallback (Layer 4). */
fun defaultTranslator(): Translator =
    FallbackTranslator(LarkApiTranslator(BuildConfig.LARK_APP_ID, BuildConfig.LARK_APP_SECRET), MlKitTranslator())
