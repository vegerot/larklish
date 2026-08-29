package com.vegerot.larklish

import org.junit.Assert.assertEquals
import org.junit.Test

/** What a `text` body reads as once Lark's editor markup is off (Experiment 11). */
class MessageTextTest {
    @Test
    fun plainTextIsLeftAlone() {
        assertEquals("a < b, and 3 > 2", unwrapHtml("a < b, and 3 > 2"))
    }

    @Test
    fun paragraphsBecomeLines() {
        assertEquals(
            "TTP 故障应急最新信息同步：\n为缓解TTP2 容量风险，请知悉～",
            unwrapHtml("<p>TTP 故障应急最新信息同步：</p><p>为缓解TTP2 容量风险，请知悉～</p>"),
        )
    }

    @Test
    fun entitiesAreDecoded() {
        assertEquals("a & b <tag>", unwrapHtml("<p>a &amp; b &lt;tag&gt;</p>"))
    }

    @Test
    fun anEscapedEntityStaysEscaped() {
        assertEquals("&lt;", unwrapHtml("<p>&amp;lt;</p>")) // the text `&lt;`, not `<`
    }
}
