package com.vegerot.larklish

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewTest {
    @Test
    fun senderAndMessageSplitAtTheFirstColonSpace() {
        assertEquals(
            Preview(sender = "Yinghao Li", mention = null, message = "哈喽，我发现有个问题"),
            Preview.parse("Yinghao Li: 哈喽，我发现有个问题"),
        )
    }

    @Test
    fun mentionSuffixOnTheSenderIsSplitOff() {
        assertEquals(
            Preview(sender = "Bits", mention = Mention.YOU, message = "Sync Task Conflicted"),
            Preview.parse("Bits@you: Sync Task Conflicted"),
        )
        assertEquals(
            Preview(sender = "Rachel Lam", mention = Mention.ALL, message = "Hello @all Please join us"),
            Preview.parse("Rachel Lam@all: Hello @all Please join us"),
        )
    }

    @Test
    fun onlyTheFirstColonSpaceSplits() {
        assertEquals(
            Preview(sender = "Bits", mention = null, message = "【告警】集群: 5xx 比例超过 1%"),
            Preview.parse("Bits: 【告警】集群: 5xx 比例超过 1%"),
        )
    }

    @Test
    fun textWithoutColonSpaceIsAllMessage() {
        assertEquals(
            Preview(sender = "", mention = null, message = "You have received a message."),
            Preview.parse("You have received a message."),
        )
    }

    @Test
    fun emptyPreviewIsSenderColonDots() {
        // No boundary in the first 45 characters → Lark posts `Sender:...` (Experiment 06 E5).
        assertEquals(Preview(sender = "Bits", mention = null, message = "..."), Preview.parse("Bits:..."))
        assertEquals("", Preview.parse("Bits:...").stem)
        assertEquals("这是一条很长的消息，", Preview.parse("Bits: 这是一条很长的消息，...").stem)
    }

    @Test
    fun emojiSurvivesInSenderAndMessage() {
        assertEquals(
            Preview(sender = "夏银璐😀", mention = null, message = "好的👍🏼 收到"),
            Preview.parse("夏银璐😀: 好的👍🏼 收到"),
        )
        assertEquals(
            Preview(sender = "Bits", mention = Mention.YOU, message = "😀"),
            Preview.parse("Bits@you: 😀"),
        )
    }
}
