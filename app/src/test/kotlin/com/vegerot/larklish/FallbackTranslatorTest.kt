package com.vegerot.larklish

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class Fixed(private val out: String) : Translator {
    override suspend fun zhToEn(text: String) = out
}

private object Broken : Translator {
    override suspend fun zhToEn(text: String): String = throw IOException("offline")
}

/** Fails [failures] times, then answers. Counts every call. */
private class FlakyThenFixed(private val failures: Int, private val out: String) : Translator {
    var calls = 0
        private set

    override suspend fun zhToEn(text: String): String {
        calls++
        if (calls <= failures) throw IOException("connection reset by peer")
        return out
    }
}

private object Untranslatable : Translator {
    var calls = 0

    override suspend fun zhToEn(text: String): String {
        calls++
        throw NotTranslated()
    }
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

    @Test
    fun oneTransportFailureIsRetriedAndKeepsLarkQuality() = runBlocking {
        val primary = FlakyThenFixed(failures = 1, out = "hello")
        assertEquals("hello", FallbackTranslator(primary, Fixed("hi")).zhToEn("你好"))
        assertEquals(2, primary.calls)
    }

    @Test
    fun twoFailuresGiveUp() = runBlocking {
        val primary = FlakyThenFixed(failures = 2, out = "hello")
        assertEquals("~hi", FallbackTranslator(primary, Fixed("hi")).zhToEn("你好"))
        assertEquals(2, primary.calls)
    }

    @Test
    fun anInputLarkWillNotTranslateIsNotRetried() = runBlocking {
        Untranslatable.calls = 0
        assertEquals("~hi", FallbackTranslator(Untranslatable, Fixed("hi")).zhToEn("你好"))
        assertEquals(1, Untranslatable.calls)
    }

    @Test
    fun everyFallbackReportsItsReason() = runBlocking {
        val reasons = mutableListOf<String>()
        FallbackTranslator(Broken, Fixed("hi")) { reasons += it }.zhToEn("你好")
        assertEquals(listOf("java.io.IOException: offline"), reasons)
    }
}
