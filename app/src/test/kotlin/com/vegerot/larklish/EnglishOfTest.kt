package com.vegerot.larklish

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Records what reaches the engine; the two gates must keep non-Han text out. */
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

/**
 * The Sender gate. `romanize` is right only for a bare Han name; everything else must go to
 * the engine. The romanizing branch needs Android's ICU, so it is covered by `RomanizeTest`
 * on the phone — every case here stays off that path.
 */
class SenderOfTest {
    private val fake = FakeTranslator()

    @Test
    fun aLatinSenderNeverReachesTheEngine() = runBlocking {
        assertEquals("Max Coplan", fake.senderOf("Max Coplan"))
        assertEquals("Daitao(DT) Song", fake.senderOf("Daitao(DT) Song"))
        assertEquals(emptyList<String>(), fake.received)
    }

    @Test
    fun aSenderThatMixesLatinAndHanGoesToTheEngine() = runBlocking {
        // `Triton数据安全` romanized to "Triton Shu Ju An Quan" — pinyin carries no meaning.
        assertEquals("translated(Triton数据安全)", fake.senderOf("Triton数据安全"))
        assertEquals(listOf("Triton数据安全"), fake.received)
    }

    @Test
    fun aHanSenderTooLongToBeANameGoesToTheEngine() = runBlocking {
        assertEquals("translated(国际电商风险运营平台)", fake.senderOf("国际电商风险运营平台"))
        assertEquals(listOf("国际电商风险运营平台"), fake.received)
    }
}

class HanNameTest {
    @Test
    fun aBareHanNameIsAName() {
        assertEquals(true, "温天信".isHanName())
        assertEquals(true, "陈昱萌".isHanName())
        assertEquals(true, "樊艳".isHanName())
    }

    @Test
    fun anythingElseIsNot() {
        assertEquals(false, "Triton数据安全".isHanName()) // Latin mixed in
        assertEquals(false, "国际电商风险运营平台".isHanName()) // too long for a name
        assertEquals(false, "陈昱萌😀".isHanName()) // an emoji is not Han
        assertEquals(false, "王".isHanName()) // a surname alone is not a name
        assertEquals(false, "Max Coplan".isHanName())
    }
}
