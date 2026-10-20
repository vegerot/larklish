package com.vegerot.larklish

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlTranslationTest {
    @Test
    fun preservesExistingMarkerLikeText() = runBlocking {
        val translator =
            object : Translator {
                override suspend fun zhToEn(text: String) = text.replace("请查看", "See")
            }
        assertEquals(
            "URL0 See https://example.com/CaseSensitive",
            translator.englishOf("URL0 请查看 https://example.com/CaseSensitive"),
        )
    }

    @Test
    fun retainsOriginalWhenTranslationDropsTheUrl() = runBlocking {
        val translator =
            object : Translator {
                override suspend fun zhToEn(text: String) = "Please see the link"
            }
        val original = "请查看 https://example.com/CaseSensitive"
        assertEquals(original, translator.englishOf(original))
    }
}
