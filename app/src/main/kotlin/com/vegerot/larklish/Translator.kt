package com.vegerot.larklish

/** Turns Chinese text into English text. */
interface Translator {
    suspend fun zhToEn(text: String): String
}

/** The one Translator on the phone: ML Kit. Lark translation belongs to the Backend. */
fun defaultTranslator(): Translator = MlKitTranslator()

private val urlPattern = Regex("https?://[^\\s\\p{IsHan}<>]+")
private val urlMarkerPattern = Regex("URL[0-9]+")

/**
 * Translate Han-containing fields while preserving HTTP(S) URLs exactly. Keep sentence context with
 * numbered markers; retain the Original text if the translation loses a marker. Fields without Han
 * pass unchanged (Experiment 02; ML Kit turned a lone emoji into "Bamboo").
 */
suspend fun Translator.englishOf(text: String): String {
    if (text.none { it.isHan() }) return text
    val urls = mutableMapOf<String, String>()
    var nextMarker = 0
    val masked =
        urlPattern.replace(text) { match ->
            while (text.contains("URL$nextMarker", ignoreCase = true)) nextMarker++
            val marker = "URL${nextMarker++}"
            urls[marker] = match.value
            marker
        }
    val english = zhToEn(masked)
    // ML Kit can rewrite text. A lost or duplicated marker cannot safely name its URL.
    val markers = urlMarkerPattern.findAll(english).map { it.value }.toList()
    if (urls.keys.any { marker -> markers.count { it == marker } != 1 }) return text
    return urlMarkerPattern.replace(english) { urls[it.value] ?: it.value }
}

/**
 * The Sender in English. A bare Han name romanizes well (陈昱萌 → "Chen Yumeng"), but a bot or a group
 * with Han in its name becomes meaningless pinyin — `国际电商风险运营平台` reads "Guo Ji Dian Shang Feng Xian
 * Yun Ying Ping Tai". Lark's engine translates those properly ("Triton Data Security"), and 10 of
 * 257 Relays carry one (Experiment 05 "Soak").
 */
suspend fun Translator.senderOf(sender: String): String =
    if (sender.isHanName()) romanize(sender) else englishOf(sender)
