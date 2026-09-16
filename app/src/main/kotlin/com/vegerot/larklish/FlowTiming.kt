package com.vegerot.larklish

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import org.json.JSONArray
import org.json.JSONObject

/** Durations for one Original, using a monotonic clock. No message text or credentials. */
class FlowTiming : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<FlowTiming>

    private val spans = JSONArray()

    @Synchronized
    fun add(name: String, ms: Long, failed: Boolean = false, status: Int = 0) {
        val span = JSONObject().put("name", name).put("ms", ms).put("failed", failed)
        if (status != 0) span.put("status", status)
        spans.put(span)
    }

    suspend fun <T> measure(name: String, work: suspend () -> T): T {
        val start = System.nanoTime()
        var failed = true
        try {
            return work().also { failed = false }
        } finally {
            add(name, (System.nanoTime() - start) / 1_000_000, failed)
        }
    }

    fun <T> measureBlocking(name: String, work: () -> T): T {
        val start = System.nanoTime()
        var failed = true
        try {
            return work().also { failed = false }
        } finally {
            add(name, (System.nanoTime() - start) / 1_000_000, failed)
        }
    }

    @Synchronized
    fun backend(remote: JSONArray) {
        for (i in 0 until remote.length()) {
            val entry = remote.getJSONObject(i)
            add(
                "backend.${entry.getString("name")}",
                entry.getLong("ms"),
                entry.optBoolean("failed"),
                entry.optInt("status"),
            )
        }
    }

    @Synchronized fun snapshot(): JSONArray = JSONArray(spans.toString())
}
