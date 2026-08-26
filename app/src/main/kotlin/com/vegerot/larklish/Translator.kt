package com.vegerot.larklish

/** Turns Chinese text into English text. */
interface Translator {
    suspend fun zhToEn(text: String): String
}
