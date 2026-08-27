package com.vegerot.larklish

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val TAG = "MessageFetcher"
private const val BEFORE_MS = 15_000L // the message is posted before its Original (E2: ~3 s)
private const val AFTER_MS = 5_000L // clock slack between the phone and Lark

/** One message from the `messages` list, reduced to what [pickMessage] needs. Mentions resolved. */
data class Candidate(
    val id: String,
    val msgType: String,
    val createTime: Long, // epoch ms
    val deleted: Boolean,
    val text: String, // `text` messages only; "" for every other type
)

sealed interface Pick {
    data class Found(val candidate: Candidate) : Pick

    /** `no-chat`, `no-match`, or `type:<msg_type>` — recorded so the soak shows why. */
    data class Skipped(val reason: String) : Pick
}

/**
 * The newest `text` message inside `[when − 15 s, when + 5 s]` whose text starts with the
 * Preview stem. A non-`text` message there is reported by type (`post`, `image`, …).
 */
fun pickMessage(items: List<Candidate>, stem: String, whenMs: Long): Pick {
    val window = items.filter { !it.deleted && it.createTime in (whenMs - BEFORE_MS)..(whenMs + AFTER_MS) }
    window.firstOrNull { it.msgType == "text" && it.text.startsWith(stem) }?.let { return Pick.Found(it) }
    val newest = window.firstOrNull() ?: return Pick.Skipped("no-match")
    return Pick.Skipped(if (newest.msgType == "text") "no-match" else "type:${newest.msgType}")
}

/** `@_user_1` placeholders → `@Name`, from the message's `mentions` (key → name). */
fun resolveMentions(text: String, mentions: Map<String, String>): String =
    Regex("@_user_\\d+").replace(text) { m -> mentions[m.value]?.let { "@$it" } ?: m.value }

/**
 * Finds the Full text behind an Original (Layer 5): title → chat id (`chats/search`,
 * cached in [cacheFile]) → newest messages of the chat → [pickMessage].
 * Groups only; DMs come in the next commit. Throws on HTTP failure.
 */
class MessageFetcher(private val token: UserToken, private val cacheFile: File) {
    private val chats: MutableMap<String, String> = HashMap<String, String>().apply {
        if (cacheFile.exists()) JSONObject(cacheFile.readText()).let { j -> j.keys().forEach { put(it, j.getString(it)) } }
    }

    suspend fun fullTextOf(title: String, preview: Preview, whenMs: Long): Pick = withContext(Dispatchers.IO) {
        val chatId = chatId(title) ?: return@withContext Pick.Skipped("no-chat")
        val items = LarkHttp.getJson(
            "/open-apis/im/v1/messages",
            mapOf(
                "container_id_type" to "chat", "container_id" to chatId,
                "sort_type" to "ByCreateTimeDesc", "page_size" to "5",
            ),
            token.bearer(),
        ).getJSONObject("data").getJSONArray("items").objects().map(::candidateOf)
        pickMessage(items, preview.stem, whenMs).also { Log.i(TAG, "[$title] → $it") }
    }

    /** Lark truncates long titles with `...` too; search by the stem, keep the hit that starts with it. */
    private fun chatId(title: String): String? {
        chats[title]?.let { return it }
        val stem = title.removeSuffix("...")
        val hits = LarkHttp.getJson("/open-apis/im/v1/chats/search", mapOf("query" to stem, "page_size" to "20"), token.bearer())
            .getJSONObject("data").getJSONArray("items").objects()
        val id = hits.firstOrNull { it.getString("name").startsWith(stem) }?.getString("chat_id") ?: return null
        chats[title] = id
        cacheFile.writeText(JSONObject(chats as Map<*, *>).toString())
        return id
    }
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)

private fun candidateOf(m: JSONObject): Candidate {
    val type = m.getString("msg_type")
    val deleted = m.optBoolean("deleted") // a recalled message stays in the list as "This message was recalled"
    val mentions = m.optJSONArray("mentions")?.objects().orEmpty().associate { it.getString("key") to it.getString("name") }
    val raw = if (type == "text" && !deleted) JSONObject(m.getJSONObject("body").getString("content")).getString("text") else ""
    return Candidate(m.getString("message_id"), type, m.getString("create_time").toLong(), deleted, resolveMentions(raw, mentions))
}

fun defaultMessageFetcher(context: Context) =
    MessageFetcher(defaultUserToken(context), File(context.filesDir, "chats.json"))
