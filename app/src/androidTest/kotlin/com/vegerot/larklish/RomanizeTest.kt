package com.vegerot.larklish

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on the phone: romanization uses Android's ICU data. */
@RunWith(AndroidJUnit4::class)
class RomanizeTest {
    @Test
    fun chineseNameBecomesSurnameGivenName() {
        assertEquals("Wen Tianxin", romanize("温天信"))
    }

    @Test
    fun senderWithoutHanIsUnchanged() {
        assertEquals("Bits", romanize("Bits"))
        assertEquals("Max Coplan", romanize("Max Coplan"))
        assertEquals("Daitao(DT) Song", romanize("Daitao(DT) Song"))
    }

    @Test
    fun hanInsideOtherTextBecomesPinyinWords() {
        assertEquals("Argos Ping Tai Bao Jing", romanize("Argos平台报警"))
    }
}
