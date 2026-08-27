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
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "MessageFetcher"

/** the message is posted before its Original (E2: ~3 s)*/
private const val BEFORE_MS = 15_000L

/** clock slack between the phone and Lark*/
private const val AFTER_MS = 5_000L

/** a bot DM's Original has this title; the Sender is the bot*/
private const val BOT_DM_TITLE = "Lark"

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

sealed interface Pick {
    data class Found(val candidate: Candidate) : Pick

    /** `no-chat`, `no-message` (nothing in the window), `no-match`, or `type:<msg_type>` — recorded so the soak shows why. */
    data class Skipped(val reason: String) : Pick
}

/**
 * The newest message with text inside `[when − 15 s, when + 5 s]` whose text starts with
 * the Preview stem, whitespace ignored (Lark's Preview drops the newlines of a `post` and
 * keeps those of a `text`, Experiment 08). A textless message there is reported by type
 * (`image`, `sticker`, `interactive`, …).
 */
fun pickMessage(items: List<Candidate>, stem: String, whenMs: Long): Pick {
    val window = items.filter { !it.deleted && it.createTime in (whenMs - BEFORE_MS)..(whenMs + AFTER_MS) }
    val squashedStem = stem.squash()
    window.firstOrNull { it.text.isNotEmpty() && it.text.squash().startsWith(squashedStem) }?.let { return Pick.Found(it) }
    val newest = window.firstOrNull() ?: return Pick.Skipped("no-message")
    return Pick.Skipped(if (newest.text.isNotEmpty()) "no-match" else "type:${newest.msgType}")
}

private fun String.squash() = filterNot { it.isWhitespace() }

/** `@_user_1` placeholders → `@Name`, from the message's `mentions` (key → name). */
fun resolveMentions(text: String, mentions: Map<String, String>): String =
    Regex("@_user_\\d+").replace(text) { m -> mentions[m.value]?.let { "@$it" } ?: m.value }

/**
 * Finds the Full text behind an Original (Layer 5): chat id → newest messages of the chat
 * → [pickMessage]. Groups: title → `chats/search`. DMs: `messages/search` around the
 * Original's time, the p2p hit whose peer name is the Sender. Chat ids are cached in
 * [cacheFile] (`title` for groups, `dm:<Sender>` for DMs). Throws on HTTP failure.
 */
class MessageFetcher(private val token: UserToken, private val cacheFile: File) {
    private val chats: MutableMap<String, String> = HashMap<String, String>().apply {
        if (cacheFile.exists()) JSONObject(cacheFile.readText()).let { j -> j.keys().forEach { put(it, j.getString(it)) } }
    }

    suspend fun fullTextOf(title: String, preview: Preview, whenMs: Long): Pick = withContext(Dispatchers.IO) {
        val chatId = chatId(title, preview, whenMs) ?: return@withContext Pick.Skipped("no-chat")
        val items = LarkHttp.getJson(
            "/open-apis/im/v1/messages",
            mapOf(
                "container_id_type" to "chat", "container_id" to chatId,
                "sort_type" to "ByCreateTimeDesc", "page_size" to "5",
            ),
            token.bearer(),
        ).getJSONObject("data").getJSONArray("items").objects().map(::candidateOf)
        Log.i(TAG, "[$title] candidates: " + items.joinToString { "${it.msgType} ${(whenMs - it.createTime) / 1000}s ago" })
        pickMessage(items, preview.stem, whenMs).also { Log.i(TAG, "[$title] → $it") }
    }

    /**
     * A DM Original has title `Lark` (bots) or the Sender's own name (people, expected — none
     * seen yet, Experiment 08); everything else is a group. The two caches never cross: the
     * bot that DMs Max also posts in groups.
     */
    private suspend fun chatId(title: String, preview: Preview, whenMs: Long): String? {
        val isDm = title == BOT_DM_TITLE || title == preview.sender
        val key = if (isDm) "dm:" + preview.sender else title
        chats[key]?.let { return it }
        val found = if (isDm) dmChatId(preview, whenMs) else groupChatId(title)
        return found?.let { remember(key, it) }
    }

    /** Lark truncates long titles with `...`; then any group whose name starts with the stem, else the exact name. */
    private fun groupChatId(title: String): String? {
        val stem = title.removeSuffix("...")
        val hits = LarkHttp.getJson("/open-apis/im/v1/chats/search", mapOf("query" to stem, "page_size" to "20"), token.bearer())
            .getJSONObject("data").getJSONArray("items").objects()
        val match: (String) -> Boolean = if (title.endsWith("...")) { n -> n.startsWith(stem) } else { n -> n == title }
        return hits.firstOrNull { match(it.getString("name")) }?.getString("chat_id")
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

    private fun remember(key: String, chatId: String): String {
        chats[key] = chatId
        cacheFile.writeText(JSONObject(chats as Map<*, *>).toString())
        return chatId
    }
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

private fun candidateOf(m: JSONObject): Candidate {
    val type = m.getString("msg_type")
    val deleted = m.optBoolean("deleted") // a recalled message stays in the list as "This message was recalled"
    val mentions = m.optJSONArray("mentions")?.objects().orEmpty().associate { it.getString("key") to it.getString("name") }
    val raw = when {
        deleted -> ""
        type == "text" -> JSONObject(m.getJSONObject("body").getString("content")).getString("text")
        type == "post" -> postText(JSONObject(m.getJSONObject("body").getString("content")))
        else -> ""
    }
    return Candidate(m.getString("message_id"), type, m.getString("create_time").toLong(), deleted, resolveMentions(raw, mentions))
}

/**
 * A `post` is `title` + paragraphs of tagged elements (Experiment 08):
 * `{"title": "", "content": [[{"tag": "text", "text": "…"}], [{"tag": "img", "image_key": "…"}]]}`.
 * One line per paragraph; images become `[image]` like Lark's own Preview.
 */
private fun postText(post: JSONObject): String {
    val paragraphs = post.getJSONArray("content").let { ps -> (0 until ps.length()).map { ps.getJSONArray(it) } }
    val lines = paragraphs.map { p ->
        p.objects().joinToString("") { e ->
            when (val tag = e.getString("tag")) {
                "text", "a" -> e.getString("text")
                "at" -> "@" + e.optString("user_name")
                "img" -> "[image]"
                else -> "[$tag]"
            }
        }
    }
    return (listOf(post.optString("title")) + lines).filter { it.isNotEmpty() }.joinToString("\n")
}

fun defaultMessageFetcher(context: Context) =
    MessageFetcher(defaultUserToken(context), File(context.filesDir, "chats.json"))
