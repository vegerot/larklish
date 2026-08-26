package com.vegerot.larklish

import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * Append-only JSONL record of every Original → Relay pair and every removal.
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
