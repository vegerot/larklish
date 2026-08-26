package com.vegerot.larklish

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Records what reaches the engine; the gate must keep non-Han text out. */
private class FakeTranslator : Translator {
    val received = mutableListOf<String>()
    override suspend fun zhToEn(text: String): String {
        received += text
        return "translated($text)"
    }
}

class EnglishOfTest {
    private val fake = FakeTranslator()

    @Test
    fun emojiOnlyTextNeverReachesTheEngine() = runBlocking {
        assertEquals("😀", fake.englishOf("😀"))
        assertEquals("👍🏼🎉", fake.englishOf("👍🏼🎉"))
        assertEquals(emptyList<String>(), fake.received)
    }

    @Test
    fun englishTextNeverReachesTheEngine() = runBlocking {
        assertEquals("Sync Task Conflicted", fake.englishOf("Sync Task Conflicted"))
        assertEquals(emptyList<String>(), fake.received)
    }

    @Test
    fun hanTextGoesToTheEngineWholeIncludingEmoji() = runBlocking {
        assertEquals("translated(服务挂了😀)", fake.englishOf("服务挂了😀"))
        assertEquals(listOf("服务挂了😀"), fake.received)
    }
}
