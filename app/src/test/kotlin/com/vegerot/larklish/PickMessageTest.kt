package com.vegerot.larklish

import org.junit.Assert.assertEquals
import org.junit.Test

/** The picker gets the newest messages of the chat and must find the one behind the Original. */
class PickMessageTest {
    private val at = 1_787_869_583_000L // an Original's `when`
    private fun text(id: String, ms: Long, text: String) = Candidate(id, "text", ms, deleted = false, text = text)

    @Test
    fun newestTextInTheWindowThatStartsWithTheStemWins() {
        val items = listOf(
            text("new", at - 1_000, "完全不同的另一条消息"),
            text("mine", at - 2_000, "这是一条很长的消息，前四十五个字符里没有边界所以预览是空的"),
            text("old", at - 60_000, "这是一条很长的消息"),
        )
        assertEquals(Pick.Found(items[1]), pickMessage(items, stem = "这是一条很长的消息，", whenMs = at))
    }

    @Test
    fun emptyStemTakesTheNewestTextInTheWindow() {
        val items = listOf(text("a", at + 1_000, "后来的"), text("b", at - 3_000, "先来的"))
        assertEquals(Pick.Found(items[0]), pickMessage(items, stem = "", whenMs = at))
    }

    @Test
    fun messagesOutsideTheWindowNeverMatch() {
        val items = listOf(text("future", at + 6_000, "x"), text("past", at - 16_000, "x"))
        assertEquals(Pick.Skipped("no-match"), pickMessage(items, stem = "", whenMs = at))
    }

    @Test
    fun recalledMessagesAreSkipped() {
        val items = listOf(Candidate("r", "text", at - 1_000, deleted = true, text = ""), text("ok", at - 2_000, "还在"))
        assertEquals(Pick.Found(items[1]), pickMessage(items, stem = "", whenMs = at))
    }

    @Test
    fun aNonTextMessageInTheWindowReportsItsType() {
        val items = listOf(Candidate("p", "post", at - 1_000, deleted = false, text = ""))
        assertEquals(Pick.Skipped("type:post"), pickMessage(items, stem = "标题", whenMs = at))
    }

    @Test
    fun aTextThatDoesNotStartWithTheStemIsNoMatch() {
        val items = listOf(text("other", at - 1_000, "别的内容"))
        assertEquals(Pick.Skipped("no-match"), pickMessage(items, stem = "这是", whenMs = at))
    }
}
