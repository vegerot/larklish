package com.vegerot.larklish

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
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
        client.downloadModelIfNeeded().await() // no-op once the model is on the phone
        return client.translate(text).await()
    }
}
