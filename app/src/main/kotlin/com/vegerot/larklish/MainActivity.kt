package com.vegerot.larklish

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val TAG = "Translator"

/** Layer 2: type Chinese, get English. Debug hook: `--es text "…"` translates at once and logs. */
class MainActivity : ComponentActivity() {
    private val translator = MlKitTranslator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TranslatorScreen(translator, initialText = intent.getStringExtra("text").orEmpty())
            }
        }
    }
}

@Composable
private fun TranslatorScreen(translator: Translator, initialText: String) {
    var input by remember { mutableStateOf(initialText) }
    var output by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun translate() = scope.launch {
        output = "…"
        output = runCatching { translator.englishOf(input) }.getOrElse { "error: $it" }
        Log.i(TAG, "in=[$input] out=[$output]")
    }

    LaunchedEffect(Unit) { if (initialText.isNotEmpty()) translate() }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Chinese") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { translate() }) { Text("Translate") }
        SelectionContainer { Text(output, style = MaterialTheme.typography.bodyLarge) }
    }
}
