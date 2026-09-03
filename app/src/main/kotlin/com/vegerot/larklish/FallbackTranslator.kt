package com.vegerot.larklish

import android.util.Log
import kotlinx.coroutines.CancellationException

private const val TAG = "FallbackTranslator"

/**
 * [primary] first, retried once; [fallback] when that fails too. Fallback output gets a leading `~`
 * so the Relay and `events.jsonl` show which engine produced it (Layer 4). Every fallback reports
 * its reason to [onFallback], so the record can tell an outage from an input Lark will not
 * translate.
 */
class FallbackTranslator(
    private val primary: Translator,
    private val fallback: Translator,
    private val onFallback: (String) -> Unit = {},
) : Translator {
    override suspend fun zhToEn(text: String): String =
        try {
            primary.zhToEn(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: NotTranslated) {
            giveUp(
                text,
                e,
            ) // deterministic: Lark returns Latin-heavy input unchanged (Experiment 05)
        } catch (e: Exception) {
            // Lark's failures are transport-level and clustered (Experiment 11): a call failed
            // inside a batch while its near-twin succeeded, and 0 of 60 sequential calls failed.
            Log.w(TAG, "primary failed: $e; retrying once")
            try {
                primary.zhToEn(text)
            } catch (retry: CancellationException) {
                throw retry
            } catch (retry: Exception) {
                giveUp(text, retry)
            }
        }

    private suspend fun giveUp(text: String, cause: Exception): String {
        Log.w(TAG, "primary failed: $cause; using fallback")
        onFallback(cause.toString())
        return "~" + fallback.zhToEn(text)
    }
}

/** Lark's engine with ML Kit as fallback (Layer 4). */
fun defaultTranslator(onFallback: (String) -> Unit = {}): Translator =
    FallbackTranslator(
        LarkApiTranslator(BuildConfig.LARK_APP_ID, BuildConfig.LARK_APP_SECRET),
        MlKitTranslator(),
        onFallback,
    )
