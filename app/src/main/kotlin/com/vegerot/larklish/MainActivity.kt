package com.vegerot.larklish

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "Translator"
private val TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())

/** Layer 2 translator screen + the recorder's event list (newest first). */
class MainActivity : ComponentActivity() {
    private val translator = defaultTranslator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recorder = Recorder(File(filesDir, "events.jsonl"))
        // Debug hooks (Layer 5): `am start --es debug user|refresh` logs whose token the app holds;
        // `--es debug fetch --es title <group> [--es text "<Sender>: <message>"]` asks the Backend
        // for the Full text.
        intent.getStringExtra("debug")?.let { what ->
            CoroutineScope(Dispatchers.IO).launch {
                debugHook(
                    what,
                    intent.getStringExtra("title").orEmpty(),
                    intent.getStringExtra("text").orEmpty(),
                )
            }
        }
        setContent {
            MaterialTheme {
                Screen(translator, recorder, initialText = intent.getStringExtra("text").orEmpty())
            }
        }
    }
}

private suspend fun MainActivity.debugHook(what: String, title: String, text: String) =
    runCatching {
        if (what == "fetch") {
            // Layer 7: the Lookup runs on the Backend; this asks it the way the listener does.
            val answer =
                Backend.lookup(
                    title,
                    text.ifEmpty { "..." },
                    System.currentTimeMillis(),
                    defaultUserToken(this).bearer(),
                )
            Log.i(TAG, "debug fetch [$title]: $answer")
            return@runCatching
        }
        val token = defaultUserToken(this)
        if (what == "refresh") token.refresh()
        val user =
            LarkHttp.getJson("/open-apis/authen/v1/user_info", emptyMap(), token.bearer())
                .getJSONObject("data")
        Log.i(
            TAG,
            "debug $what: user token belongs to ${user.optString("open_id")} (${user.optString("name")})",
        )
    }
    .onFailure { Log.w(TAG, "debug $what failed: $it") }

@Composable
private fun Screen(translator: Translator, recorder: Recorder, initialText: String) {
    var input by remember { mutableStateOf(initialText) }
    var output by remember { mutableStateOf("") }
    var events by remember { mutableStateOf(recorder.readAll()) }
    val scope = rememberCoroutineScope()

    fun translate() = scope.launch {
        output = "…"
        output = runCatching { translator.englishOf(input) }.getOrElse { "error: $it" }
        Log.i(TAG, "in=[$input] out=[$output]")
    }

    LaunchedEffect(Unit) { if (initialText.isNotEmpty()) translate() }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Chinese") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { translate() }) { Text("Translate") }
        SelectionContainer { Text(output, style = MaterialTheme.typography.bodyLarge) }

        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Events",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { events = recorder.readAll() }) { Text("Refresh") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(events.asReversed()) { event -> EventRow(event) }
        }
    }
}

@Composable
private fun EventRow(event: JSONObject) {
    val at = runCatching { TIME.format(Instant.parse(event.getString("at"))) }.getOrDefault("?")
    Column {
        when (event.optString("event")) {
            "relayed" -> {
                Text(
                    "$at  ${event.optString("relayTitle")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(event.optString("relayText"), style = MaterialTheme.typography.bodyMedium)
                Text(
                    event.optString("text"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            "updated" -> {
                Text(
                    "$at  updated (${event.optString("msgType")})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(event.optString("relayText"), style = MaterialTheme.typography.bodyMedium)
            }
            "skipped" ->
                Text(
                    "$at  skipped: ${event.optString("reason")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            "removed" ->
                Text(
                    "$at  removed (reason ${event.optInt("reason")})  ${event.optString("key").substringAfter("suite|").substringBefore("|")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            else ->
                Text(
                    "$at  ${event.optString("event")}",
                    style = MaterialTheme.typography.labelMedium,
                )
        }
    }
}
