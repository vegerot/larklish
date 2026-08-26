package com.vegerot.larklish

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

private class Fixed(private val out: String) : Translator {
    override suspend fun zhToEn(text: String) = out
}

private object Broken : Translator {
    override suspend fun zhToEn(text: String): String = throw IOException("offline")
}

class FallbackTranslatorTest {
    @Test
    fun primaryResultIsUsedUnmarked() = runBlocking {
        assertEquals("hello", FallbackTranslator(Fixed("hello"), Fixed("hi")).zhToEn("你好"))
    }

    @Test
    fun fallbackResultIsMarkedWithTilde() = runBlocking {
        assertEquals("~hi", FallbackTranslator(Broken, Fixed("hi")).zhToEn("你好"))
    }
}
