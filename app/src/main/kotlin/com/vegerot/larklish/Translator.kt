package com.vegerot.larklish

/** Turns Chinese text into English text. */
interface Translator {
    suspend fun zhToEn(text: String): String
}

/**
 * Only Han text goes to the Translator. English, names, identifiers and emoji pass
 * unchanged — ML Kit mangles them (Experiment 02; it turned a lone 😀 into "Bamboo").
 */
suspend fun Translator.englishOf(text: String): String = if (text.any { it.isHan() }) zhToEn(text) else text
