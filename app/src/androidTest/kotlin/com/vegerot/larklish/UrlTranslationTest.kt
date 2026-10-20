package com.vegerot.larklish

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlTranslationTest {
    @Test
    fun preservesCaseSensitiveUrlInChinesePreview() = runBlocking {
        val output =
            MlKitTranslator().englishOf("请查看 https://example.com/CaseSensitive?token=AbC123 查看结果")
        assertTrue(output, output.contains("https://example.com/CaseSensitive?token=AbC123"))
    }

    @Test
    fun preservesExistingMarkerAndTwoUrlsBesidePunctuation() = runBlocking {
        val output =
            MlKitTranslator()
                .englishOf("URL0：请比较（https://example.com/Before）和 https://example.com/After，查看结果")
        println("URL comparison: $output")
        assertTrue(output, output.contains("URL0"))
        assertTrue(output, output.contains("https://example.com/Before"))
        assertTrue(output, output.contains("https://example.com/After"))
        assertTrue(output, output.contains("compare", ignoreCase = true))
        assertTrue(output, output.contains("and", ignoreCase = true))
        assertTrue(output, output.none { it.isHan() })
    }
}
