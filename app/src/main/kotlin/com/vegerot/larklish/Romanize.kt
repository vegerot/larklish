package com.vegerot.larklish

import android.icu.text.Transliterator

// Han → pinyin (name readings), then strip tone marks. Android ships the ICU data.
private val hanToLatin: Transliterator = Transliterator.getInstance("Han-Latin/Names; Latin-ASCII")

/**
 * Romanize a Sender for a reader who does not read Chinese.
 * 温天信 → "Wen Tianxin"; Argos平台报警 → "Argos Ping Tai Bao Jing"; "Max Coplan" unchanged.
 */
fun romanize(sender: String): String {
    if (sender.none { it.isHan() }) return sender
    val syllables = hanToLatin.transliterate(sender).split(' ').filter { it.isNotEmpty() }
    val capitalized = syllables.map { it.replaceFirstChar(Char::uppercase) }
    // A bare Chinese name: one-character surname + given name. Two-character surnames
    // (欧阳, 司马) are rare among Max's contacts; they come out as "Ou Yangxx". Accepted.
    val isName = sender.length in 2..4 && sender.all { it.isHan() }
    if (isName) {
        val given = syllables.drop(1).joinToString("").replaceFirstChar(Char::uppercase)
        return "${capitalized.first()} $given"
    }
    return capitalized.joinToString(" ")
}
