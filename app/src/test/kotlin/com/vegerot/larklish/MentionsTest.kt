package com.vegerot.larklish

import org.junit.Assert.assertEquals
import org.junit.Test

class MentionsTest {
    @Test
    fun placeholdersBecomeAtNames() {
        assertEquals(
            "@Max Coplan 这条消息 @Yinghao Li 谢谢",
            resolveMentions(
                "@_user_1 这条消息 @_user_2 谢谢",
                mapOf("@_user_1" to "Max Coplan", "@_user_2" to "Yinghao Li"),
            ),
        )
    }

    @Test
    fun tenthMentionIsNotEatenByTheFirst() {
        assertEquals(
            "@Ten @One",
            resolveMentions("@_user_10 @_user_1", mapOf("@_user_1" to "One", "@_user_10" to "Ten")),
        )
    }

    @Test
    fun unknownPlaceholdersStay() {
        assertEquals("@_user_3 hi", resolveMentions("@_user_3 hi", emptyMap()))
    }

    @Test
    fun theAllMentionHasNoEntryAndReadsLikeThePreview() {
        // A `text` body spells @all as `@_all`, and the `mentions` array never names it (soak
        // 2026-09-02).
        assertEquals(
            "Hi @all Labor Day event setup is ready!",
            resolveMentions("Hi @_all Labor Day event setup is ready!", emptyMap()),
        )
    }
}
