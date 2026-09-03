package com.vegerot.larklish

/** Turns Chinese text into English text. */
interface Translator {
    suspend fun zhToEn(text: String): String
}

/**
 * Only Han text goes to the Translator. English, names, identifiers and emoji pass unchanged — ML
 * Kit mangles them (Experiment 02; it turned a lone 😀 into "Bamboo").
 */
suspend fun Translator.englishOf(text: String): String =
    if (text.any { it.isHan() }) zhToEn(text) else text

/**
 * The Sender in English. A bare Han name romanizes well (陈昱萌 → "Chen Yumeng"), but a bot or a group
 * with Han in its name becomes meaningless pinyin — `国际电商风险运营平台` reads "Guo Ji Dian Shang Feng Xian
 * Yun Ying Ping Tai". Lark's engine translates those properly ("Triton Data Security"), and 10 of
 * 257 Relays carry one (Experiment 05 "Soak").
 */
suspend fun Translator.senderOf(sender: String): String =
    if (sender.isHanName()) romanize(sender) else englishOf(sender)
