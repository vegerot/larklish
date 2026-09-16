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
     * Layer 5: the Relay was Updated with the translated Full text. `backend` (Layer 8) is the
     * Backend URL that answered.
     */
    fun updated(
        key: String,
        msgType: String,
        fullText: String,
        relayText: String,
        backend: String,
        flowId: String = "",
    ) {
        append(
            JSONObject()
                .put("event", "updated")
                .put("key", key)
                .put("msgType", msgType)
                .put("fullText", fullText)
                .put("relayText", relayText)
                .put("backend", backend)
                .put("flowId", flowId)
        )
    }

    /**
     * Layer 5: no Update; `reason` is `no-chat`, `no-match`, `type:<msg_type>` or `error: …`.
     * `backend` is the URL that answered a skip; null when none did (`error:`, `not-truncated`).
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
