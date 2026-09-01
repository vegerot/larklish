package com.vegerot.larklish

import android.content.Context
import android.text.Html
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "MessageFetcher"

/** the message is posted before its Original: p50 5 s, p95 24 s, max 44 s (Experiment 11) */
private const val BEFORE_MS = 60_000L

/**
 * Lark can deliver a backlog minutes late — 3 m 59 s observed, three Originals in two seconds.
 * Only a full-length needle may reach that far back: it names one message on its own, so the
 * extra reach cannot mistake a neighbour for it.
 */
private const val BEFORE_DELAYED_MS = 300_000L

/** clock slack between the phone and Lark. No message ever arrives after its Original (Experiment 11). */
private const val AFTER_MS = 5_000L

/** enough characters to name one message inside the window; 0 false positives in 35 chats (Experiment 11) */
private const val MATCH_CHARS = 12

/** a bot DM's Original has this title; the Sender is the bot*/
private const val BOT_DM_TITLE = "Lark"

/** chats to re-try when the title names no chat; two is the whole prize (Experiment 10) */
private const val RECENT_TRIES = 2

/** a truncated title can name many chats (`【bytedcli MR 处理】` names 6); try this many (Experiment 11) */
private const val PREFIX_TRIES = 5

/** the search index lags ~15 s behind the list (E2 continued) */
private const val SEARCH_TRIES = 4
private const val SEARCH_GAP_MS = 2_000L

/** One message from the `messages` list, reduced to what [pickMessage] needs. Mentions resolved. */
data class Candidate(
    val id: String,
    val msgType: String,
    val createTime: Long, // epoch ms
    val deleted: Boolean,
    val text: String, // `text` and flattened `post` messages; "" for every other type
)

/** A [Pick.Skipped] reason that means the call failed, not that nothing matched. */
private const val ERROR = "error: "

sealed interface Pick {
    data class Found(val candidate: Candidate) : Pick

    /**
     * `no-chat`, `no-message` (nothing in the window), `no-match`, `type:<msg_type>`, or
     * `error: …` when the chat could not be read — recorded so the soak shows why.
     */
    data class Skipped(val reason: String) : Pick
}

/** The call failed. Such a chat answers for itself only: never cache it, never trust its silence. */
private fun Pick.failed() = this is Pick.Skipped && reason.startsWith(ERROR)

/**
 * The newest message with text just before the Original whose text opens with the Preview
 * stem. Only the first [MATCH_CHARS] folded characters must agree: the Preview is not a
 * faithful prefix of the Full text — it writes `[Delighted]` for an emoji, lower-cases
 * `@All`, and can drop a trailing bold run (Experiment 11). A message that opens with a
 * mention is matched on the close of the stem instead, because Lark localizes the name.
 * A textless message in the window is reported by type (`image`, `sticker`, `interactive`, …).
 *
 * How far back the window reaches depends on how much needle there is. A full-length one
 * identifies the message by itself, so it may look past a delayed push; a short or empty stem
 * — `Sender:...` with no boundary in 45 characters — leans on the window to tell messages
 * apart, so it stays close to the Original.
 */
fun pickMessage(items: List<Candidate>, stem: String, whenMs: Long): Pick {
    val folded = stem.fold()
    val needle = folded.take(MATCH_CHARS)
    val before = if (needle.length == MATCH_CHARS) BEFORE_DELAYED_MS else BEFORE_MS
    val window = items.filter { !it.deleted && it.createTime in (whenMs - before)..(whenMs + AFTER_MS) }
    window.firstOrNull { it.text.isNotEmpty() && it.text.fold().startsWith(needle) }?.let { return Pick.Found(it) }
    // A mention shows whichever of an account's names the reader prefers, so the two sides
    // disagree on the opening and on nothing else: the phone writes `@Oncall Assistant`,
    // `@all` and `@Yan Fan`, the API `@Oncall 助手`, `@所有人` and `@樊艳` (Experiment 12).
    // Both sides must open with a mention, or a shared close — a link, a sign-off — could
    // name the wrong message.
    if (folded.startsWith("@") && folded.length >= MATCH_CHARS) {
        val close = folded.takeLast(MATCH_CHARS)
        window.firstOrNull { it.text.startsWith("@") && it.text.fold().contains(close) }?.let { return Pick.Found(it) }
    }
    val newest = window.firstOrNull() ?: return Pick.Skipped("no-message")
    return Pick.Skipped(if (newest.text.isNotEmpty()) "no-match" else "type:${newest.msgType}")
}

/** How a stem and a message text are compared: no whitespace, no case (Lark's Preview keeps neither). */
private fun String.fold() = filterNot { it.isWhitespace() }.lowercase()

/** `@_user_1` placeholders → `@Name`, from the message's `mentions` (key → name). */
fun resolveMentions(text: String, mentions: Map<String, String>): String =
    Regex("@_user_\\d+").replace(text) { m -> mentions[m.value]?.let { "@$it" } ?: m.value }

/**
 * Finds the Full text behind an Original (Layer 5): chat id → newest messages of the chat
 * → [pickMessage]. Groups: title → `chats/search`. DMs: `messages/search` around the
 * Original's time, the p2p hit whose peer name is the Sender. Chat ids are cached in
 * [cacheFile] (`title` for groups, `dm:<Sender>` for DMs). A chat that cannot be read is
 * just one chat's miss ([pickIn]); a failed *search* still throws, having no candidates to
 * fall back on.
 */
class MessageFetcher(private val token: UserToken, private val cacheFile: File) {
    private val chats: MutableMap<String, String> = HashMap<String, String>().apply {
        if (cacheFile.exists()) JSONObject(cacheFile.readText()).let { j -> j.keys().forEach { put(it, j.getString(it)) } }
    }

    /** Chats that produced a Full text, most recent first. Feeds the thread fallback below. */
    private val recent = ArrayDeque<String>()

    suspend fun fullTextOf(title: String, preview: Preview, whenMs: Long): Pick = withContext(Dispatchers.IO) {
        val key = if (isDm(title, preview)) "dm:" + preview.sender else title
        val candidates = chats[key]?.let { listOf(it) } ?: searchChats(title, preview, whenMs)
        var outcome: Pick = Pick.Skipped("no-chat")
        // A truncated title can name several chats. Only the one holding the message is right,
        // so ask each in turn and remember that one — never a guess (Experiment 09).
        candidates.forEachIndexed { i, chatId ->
            val pick = pickIn(chatId, title, preview, whenMs)
            if (i == 0) outcome = pick
            if (pick is Pick.Found) {
                remember(key, chatId)
                return@withContext pick
            }
        }
        // Unambiguous and readable: keep it even on a miss. A chat that answered 400 is never
        // cached — every later Original under this title would hit the same 400 forever.
        if (candidates.size == 1 && !outcome.failed()) remember(key, candidates[0])
        // A reply inside a topic chat carries the *thread's* root text as its title, so no chat
        // search can ever find it (Experiment 09). Such a reply follows other traffic in the same
        // chat, so the chat is almost always one Larklish just used (Experiment 10).
        for (chatId in recent.take(RECENT_TRIES)) {
            val pick = pickIn(chatId, title, preview, whenMs)
            if (pick is Pick.Found) return@withContext pick
        }
        outcome
    }

    /**
     * One chat's answer. A failure here is **this chat's** answer, not the lookup's:
     * `chats/search` returns chats Max can find but not read (`230002 Bot/User can NOT be out
     * of the chat`), and one of those used to abort `fullTextOf` outright — skipping the other
     * candidates and the LRU fallback added for exactly this kind of miss (progress.md
     * 2026-08-31). The reason still reaches the record through [Pick.Skipped].
     */
    private fun pickIn(chatId: String, title: String, preview: Preview, whenMs: Long): Pick = try {
        val items = LarkHttp.getJson(
            "/open-apis/im/v1/messages",
            // Bound the request by the same window [pickMessage] uses, so a burst in a busy
            // chat cannot push the message off the page.
            mapOf(
                "container_id_type" to "chat", "container_id" to chatId,
                "sort_type" to "ByCreateTimeDesc", "page_size" to "20",
                "start_time" to ((whenMs - BEFORE_DELAYED_MS) / 1000).toString(),
                "end_time" to ((whenMs + AFTER_MS) / 1000).toString(),
            ),
            token.bearer(),
        ).getJSONObject("data").getJSONArray("items").objects().map(::candidateOf)
        Log.i(TAG, "[$title] $chatId candidates: " + items.joinToString { "${it.msgType} ${(whenMs - it.createTime) / 1000}s ago" })
        pickMessage(items, preview.stem, whenMs).also {
            Log.i(TAG, "[$title] $chatId → $it")
            if (it is Pick.Found) {
                recent.remove(chatId)
                recent.addFirst(chatId)
            }
        }
    } catch (e: IOException) {
        Log.w(TAG, "[$title] $chatId unreadable: $e")
        Pick.Skipped(ERROR + e)
    }

    /**
     * A DM Original has title `Lark` — bots and people alike (Experiment 09) — or, in theory,
     * the Sender's own name; everything else is a group. The two caches never cross: the bot
     * that DMs Max also posts in groups.
     */
    private fun isDm(title: String, preview: Preview) = title == BOT_DM_TITLE || title == preview.sender

    private suspend fun searchChats(title: String, preview: Preview, whenMs: Long): List<String> =
        if (isDm(title, preview)) listOfNotNull(dmChatId(preview, whenMs)) else groupChatIds(title)

    /** Lark truncates long titles with `...`; then every group whose name starts with the stem, else the exact name. */
    private fun groupChatIds(title: String): List<String> {
        val stem = title.removeSuffix("...")
        val hits = LarkHttp.getJson("/open-apis/im/v1/chats/search", mapOf("query" to stem, "page_size" to "20"), token.bearer())
            .getJSONObject("data").getJSONArray("items").objects()
        val match: (String) -> Boolean = if (title.endsWith("...")) { n -> n.startsWith(stem) } else { n -> n == title }
        return hits.filter { match(it.getString("name")) }.take(PREFIX_TRIES).map { it.getString("chat_id") }
    }

    /**
     * `messages/search` (user identity) around the Original's time; the p2p hit whose peer
     * name (first line of `display_info`) is the Sender. `chat_type` is ignored without a
     * `query`, so filter on `is_p2p_chat`. Polls because the index lags ~15 s.
     */
    private suspend fun dmChatId(preview: Preview, whenMs: Long): String? {
        val body = JSONObject()
            .put("start_time", ((whenMs - 60_000) / 1000).toString())
            .put("end_time", ((whenMs + 60_000) / 1000).toString())
            .put("page_size", 20)
        if (preview.stem.isNotEmpty()) body.put("query", preview.stem)
        repeat(SEARCH_TRIES) { attempt ->
            if (attempt > 0) delay(SEARCH_GAP_MS.milliseconds*attempt)
            val hits = LarkHttp.postJson("/open-apis/im/v1/messages/search", body, token.bearer())
                .getJSONObject("data").getJSONArray("items").objects()
            hits.firstOrNull {
                it.getJSONObject("meta_data").getBoolean("is_p2p_chat") && peerName(it) == preview.sender
            }?.let { return it.getJSONObject("meta_data").getString("chat_id") }
            Log.i(TAG, "dm search for [${preview.sender}]: ${hits.size} hits, no peer match (try ${attempt + 1})")
        }
        return null
    }

    private fun peerName(hit: JSONObject): String =
        Html.fromHtml(hit.getString("display_info").substringBefore("\n"), 0).toString()

    private fun remember(key: String, chatId: String) {
        if (chats[key] == chatId) return
        chats[key] = chatId
        cacheFile.writeText(JSONObject(chats as Map<*, *>).toString())
    }
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

private fun candidateOf(m: JSONObject): Candidate {
    val type = m.getString("msg_type")
    val deleted = m.optBoolean("deleted") // a recalled message stays in the list as "This message was recalled"
    val mentions = m.optJSONArray("mentions")?.objects().orEmpty().associate { it.getString("key") to it.getString("name") }
    val raw = when {
        deleted -> ""
        type == "text" -> unwrapHtml(JSONObject(m.getJSONObject("body").getString("content")).getString("text"))
        type == "post" -> postText(JSONObject(m.getJSONObject("body").getString("content")))
        else -> ""
    }
    return Candidate(m.getString("message_id"), type, m.getString("create_time").toLong(), deleted, resolveMentions(raw, mentions))
}

/**
 * Lark's editor wraps a `text` body in paragraphs — `<p>line</p><p>line</p>` — and Lark's own
 * Preview shows none of it (11 of 198 text messages, Experiment 11).
 */
fun unwrapHtml(text: String): String {
    if (!text.startsWith("<p>")) return text
    return text.replace("</p><p>", "\n").replace(Regex("</?[a-z][^>]*>"), "")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&") // `&amp;` last, or `&amp;lt;` decodes twice
}

/** One tagged piece of a `post` paragraph, with only the fields [postText] reads. */
data class PostElement(val tag: String, val text: String = "", val userName: String = "", val emojiType: String = "")

/**
 * A `post` is `title` + paragraphs of tagged elements (Experiment 08):
 * `{"title": "", "content": [[{"tag": "text", "text": "…"}], [{"tag": "img", "image_key": "…"}]]}`.
 * One line per paragraph; images become `[image]` and emoji `[Delighted]`, like Lark's own Preview.
 * Pure, so the replay harness scores the shipped rules instead of a copy of them.
 */
fun postText(title: String, paragraphs: List<List<PostElement>>): String {
    val lines = paragraphs.map { p ->
        p.joinToString("") { e ->
            when (e.tag) {
                "text", "a" -> e.text
                "at" -> "@" + e.userName
                "img" -> "[image]"
                "emotion" -> "[" + e.emojiType.ifEmpty { e.tag } + "]"
                else -> "[${e.tag}]"
            }
        }
    }
    return (listOf(title) + lines).filter { it.isNotEmpty() }.joinToString("\n")
}

/** Lark's JSON shape of a `post`, mapped onto [postText]'s pure input. */
private fun postText(post: JSONObject): String = postText(
    post.optString("title"),
    post.getJSONArray("content").let { ps -> (0 until ps.length()).map { ps.getJSONArray(it) } }.map { p ->
        p.objects().map {
            PostElement(it.getString("tag"), it.optString("text"), it.optString("user_name"), it.optString("emoji_type"))
        }
    },
)

fun defaultMessageFetcher(context: Context) =
    MessageFetcher(defaultUserToken(context), File(context.filesDir, "chats.json"))
