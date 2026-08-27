package com.vegerot.larklish

import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * Append-only JSONL record of every Original → Relay pair, every Update or skip, and every removal.
 * File: `filesDir/events.jsonl`. Read it from the debug UI or with
 * `adb exec-out run-as com.vegerot.larklish cat files/events.jsonl`.
 */
class Recorder(private val file: File) {

    fun relayed(key: String, title: String, text: String, relayTitle: String, relayText: String) {
        append(
            JSONObject()
                .put("event", "relayed")
                .put("key", key)
                .put("title", title)
                .put("text", text)
                .put("relayTitle", relayTitle)
                .put("relayText", relayText),
        )
    }

    /** Layer 5: the Relay was Updated with the translated Full text. */
    fun updated(key: String, msgType: String, fullText: String, relayText: String) {
        append(
            JSONObject()
                .put("event", "updated")
                .put("key", key)
                .put("msgType", msgType)
                .put("fullText", fullText)
                .put("relayText", relayText),
        )
    }

    /** Layer 5: no Update; `reason` is `no-chat`, `no-match`, `type:<msg_type>` or `error: …`. */
    fun skipped(key: String, reason: String) {
        append(JSONObject().put("event", "skipped").put("key", key).put("reason", reason))
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
        file.appendText(json.toString() + "\n")
    }
}
