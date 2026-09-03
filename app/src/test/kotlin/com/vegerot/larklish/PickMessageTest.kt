package com.vegerot.larklish

import org.junit.Assert.assertEquals
import org.junit.Test

/** The picker gets the newest messages of the chat and must find the one behind the Original. */
class PickMessageTest {
    private val at = 1_787_869_583_000L // an Original's `when`

    private fun text(id: String, ms: Long, text: String) =
        Candidate(id, "text", ms, deleted = false, text = text)

    @Test
    fun newestTextInTheWindowThatStartsWithTheStemWins() {
        val items =
            listOf(
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
        val items = listOf(text("future", at + 6_000, "x"), text("past", at - 61_000, "x"))
        assertEquals(Pick.Skipped("no-message"), pickMessage(items, stem = "", whenMs = at))
    }

    @Test
    fun theWindowReachesBackTheFullMinute() {
        // The message-to-Original delay runs to 43.9 s on real traffic (Experiment 11).
        val late = text("late", at - 44_000, "还在窗口里")
        assertEquals(Pick.Found(late), pickMessage(listOf(late), stem = "还在窗口里", whenMs = at))
    }

    @Test
    fun theStemMatchesWithoutItsCase() {
        // Lark's Preview lower-cases the `@All` mention of a `post` (Experiment 11).
        val post =
            Candidate("p", "post", at - 1_000, deleted = false, text = "Hi @All Happy Friday!")
        assertEquals(
            Pick.Found(post),
            pickMessage(listOf(post), stem = "Hi @all Happy Friday!", whenMs = at),
        )
    }

    @Test
    fun onlyTheOpeningOfTheStemHasToAgree() {
        // Lark's Preview dropped the trailing bold run `FOR SAN JOSE OFFICE ONLY` (Experiment 11).
        val post =
            Candidate(
                "p",
                "post",
                at - 1_000,
                deleted = false,
                text =
                    "Hi @All Happy Friday! FOR SAN JOSE OFFICE ONLY\nHope everyone has started packing",
            )
        assertEquals(
            Pick.Found(post),
            pickMessage(
                listOf(post),
                stem = "Hi @all Happy Friday!Hope everyone has started packing",
                whenMs = at,
            ),
        )
    }

    @Test
    fun aFullLengthStemReachesPastADelayedPush() {
        // Lark delivered a backlog 3 m 59 s late on 2026-08-28; the stem still names the message.
        val late =
            text("late", at - 239_000, "How cool would it be if you could search lark and docs")
        assertEquals(
            Pick.Found(late),
            pickMessage(
                listOf(late),
                stem = "How cool would it be if you could search lark and docs...".dropLast(3),
                whenMs = at,
            ),
        )
    }

    @Test
    fun aShortStemDoesNotReachThatFar() {
        // With too little needle the window is the only identifier, so it stays near the Original.
        val late = text("late", at - 239_000, "Yo")
        assertEquals(
            Pick.Skipped("no-message"),
            pickMessage(listOf(late), stem = "Yo", whenMs = at),
        )
    }

    @Test
    fun anEmptyStemNeverReachesPastTheMinute() {
        // `Sender:...` matches anything, so a wide window could take the wrong message.
        val late = text("late", at - 120_000, "任意内容")
        assertEquals(Pick.Skipped("no-message"), pickMessage(listOf(late), stem = "", whenMs = at))
    }

    @Test
    fun aShortStemStillHasToAgreeInFull() {
        val items = listOf(text("other", at - 1_000, "好的"))
        assertEquals(Pick.Skipped("no-match"), pickMessage(items, stem = "不好", whenMs = at))
    }

    @Test
    fun aMentionLarkLocalizesStillMatchesOnItsClose() {
        // The phone writes `@Oncall Assistant`, the API `@Oncall 助手` (Experiment 12).
        val post =
            Candidate(
                "p",
                "post",
                at - 1_000,
                deleted = false,
                text = "@Oncall 助手 MPA流水线发布卡住，重试之后失败",
            )
        assertEquals(
            Pick.Found(post),
            pickMessage(listOf(post), stem = "@Oncall Assistant MPA流水线发布卡住，重试之后失败", whenMs = at),
        )
    }

    @Test
    fun theAllMentionIsLocalizedTheSameWay() {
        // `@all` on the phone, `@所有人` from the API — no `mentions` entry names it (Experiment 12).
        val post =
            Candidate(
                "p",
                "post",
                at - 1_000,
                deleted = false,
                text = "@所有人 正在直播中~\n会议链接：https://www.us.larkoffice.com/calendar/share?token=e975",
            )
        assertEquals(
            Pick.Found(post),
            pickMessage(
                listOf(post),
                stem = "@all 正在直播中~会议链接：https://www.us.larkoffice.com",
                whenMs = at,
            ),
        )
    }

    @Test
    fun theCloseOnlyDecidesWhenBothSidesOpenWithAMention() {
        // Without that guard a shared tail — a link, a sign-off — could name the wrong message.
        val other = text("other", at - 1_000, "刚才 MPA流水线发布卡住，重试之后失败")
        assertEquals(
            Pick.Skipped("no-match"),
            pickMessage(listOf(other), stem = "@Oncall Assistant MPA流水线发布卡住，重试之后失败", whenMs = at),
        )
    }

    @Test
    fun theOpeningStillDecidesWhenItCan() {
        val items =
            listOf(
                text("newer", at - 1_000, "@Someone 完全不同的开头 MPA流水线发布卡住，重试之后失败"),
                text("mine", at - 2_000, "@Oncall Assistant MPA流水线发布卡住，重试之后失败"),
            )
        assertEquals(
            Pick.Found(items[1]),
            pickMessage(items, stem = "@Oncall Assistant MPA流水线发布卡住，重试之后失败", whenMs = at),
        )
    }

    @Test
    fun recalledMessagesAreSkipped() {
        val items =
            listOf(
                Candidate("r", "text", at - 1_000, deleted = true, text = ""),
                text("ok", at - 2_000, "还在"),
            )
        assertEquals(Pick.Found(items[1]), pickMessage(items, stem = "", whenMs = at))
    }

    @Test
    fun aTextlessMessageInTheWindowReportsItsType() {
        val items = listOf(Candidate("i", "image", at - 1_000, deleted = false, text = ""))
        assertEquals(Pick.Skipped("type:image"), pickMessage(items, stem = "标题", whenMs = at))
    }

    @Test
    fun aFlattenedPostMatchesThePreviewThatDroppedItsNewlines() {
        // Lark's Preview for an image-then-text post: `[image]Cool performance by …` (Experiment
        // 08).
        val post =
            Candidate(
                "p",
                "post",
                at - 1_000,
                deleted = false,
                text = "[image]\nCool performance by Sofia Alise in the courtyard!! Swing by!!!!",
            )
        assertEquals(
            Pick.Found(post),
            pickMessage(
                listOf(post),
                stem = "[image]Cool performance by Sofia Alise in the courtyard!!",
                whenMs = at,
            ),
        )
    }

    @Test
    fun aTextThatDoesNotStartWithTheStemIsNoMatch() {
        val items = listOf(text("other", at - 1_000, "别的内容"))
        assertEquals(Pick.Skipped("no-match"), pickMessage(items, stem = "这是", whenMs = at))
    }
}
