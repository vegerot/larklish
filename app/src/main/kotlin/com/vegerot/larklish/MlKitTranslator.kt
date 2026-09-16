package com.vegerot.larklish

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.tasks.await

/** On-device translation with Google ML Kit. The model (~30 MB) downloads on first use. */
class MlKitTranslator : Translator {
    private val client =
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.CHINESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
        )

    override suspend fun zhToEn(text: String): String {
        val timing = currentCoroutineContext()[FlowTiming]
        if (timing == null) {
            client.downloadModelIfNeeded().await()
            return client.translate(text).await()
        }
        timing.measure("device.model") { client.downloadModelIfNeeded().await() }
        return timing.measure("device.translate") { client.translate(text).await() }
    }
}
