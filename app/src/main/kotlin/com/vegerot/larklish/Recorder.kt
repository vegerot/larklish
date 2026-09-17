package com.vegerot.larklish

import java.io.File
import java.time.Instant
import org.json.JSONObject

/**
 * Append-only JSONL record of every Original → Relay pair, every Update or skip, and every removal.
 * File: `filesDir/events.jsonl`. Read it from the debug UI or with `adb exec-out run-as
 * com.vegerot.larklish cat files/events.jsonl`.
 */
class Recorder(
    private val file: File,
    private val networkState: () -> JSONObject? = { null },
) {

    /** `truncated`: Lark cut the Preview (`Preview.truncated`), so an Update was due. */
    fun relayed(
        key: String,
        title: String,
        text: String,
        relayTitle: String,
        relayText: String,
        truncated: Boolean,
        flowId: String = "",
    ) {
        append(
            JSONObject()
                .put("event", "relayed")
                .put("key", key)
                .put("title", title)
                .put("text", text)
                .put("relayTitle", relayTitle)
                .put("relayText", relayText)
                .put("truncated", truncated)
                .put("flowId", flowId)
        )
    }

    /**
     * The Relay was Updated from a translated Preview or Full text. `backend` is the Backend URL
     * that answered; `failures` records title/Sender translations that did not block the Update.
     */
    fun updated(
        key: String,
        source: String,
        msgType: String,
        message: String,
        relayText: String,
        backend: String,
        failures: List<String>,
        flowId: String = "",
    ) {
        append(
            JSONObject()
                .put("event", "updated")
                .put("key", key)
                .put("source", source)
                .put("msgType", msgType)
                .put("message", message)
                .put("relayText", relayText)
                .put("backend", backend)
                .put("failures", failures)
                .put("flowId", flowId)
        )
    }

    /**
     * No Update; `reason` is `complete-no-han`, a Lookup miss, `translation-failed`, or `error: …`.
     * `backend` is the URL that answered a skip; null when no request was needed or it failed.
     */
    fun skipped(key: String, reason: String, backend: String? = null, flowId: String = "") {
        val json =
            JSONObject()
                .put("event", "skipped")
                .put("key", key)
                .put("reason", reason)
                .put("flowId", flowId)
        if (backend != null) json.put("backend", backend)
        append(json)
    }

    fun timing(
        key: String,
        flowId: String,
        outcome: String,
        spans: org.json.JSONArray,
        totalMs: Long,
    ) {
        append(
            JSONObject()
                .put("event", "timing")
                .put("key", key)
                .put("flowId", flowId)
                .put("outcome", outcome)
                .put("totalMs", totalMs)
                .put("spans", spans)
        )
    }

    /**
     * Layer 6: the Translator fell back to ML Kit. `reason` separates an outage from an
     * untranslatable input.
     */
    fun fallback(reason: String) {
        append(JSONObject().put("event", "fallback").put("reason", reason))
    }

    fun removed(key: String, reason: Int) {
        append(JSONObject().put("event", "removed").put("key", key).put("reason", reason))
    }

    fun readAll(): List<JSONObject> =
        if (file.exists()) {
            file.readLines().mapNotNull { line -> runCatching { JSONObject(line) }.getOrNull() }
        } else {
            emptyList()
        }

    private fun append(json: JSONObject) {
        json.put("at", Instant.now().toString())
        networkState()?.let { json.put("network", it) }
        file.appendText(json.toString() + "\n")
    }
}
