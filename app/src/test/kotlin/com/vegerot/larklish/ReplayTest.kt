package com.vegerot.larklish

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

// The separators `tools/larklish-helper replay fetch` writes: items, paragraphs, fields of an
// element.
private const val SEP_ITEM = '\u0001'
private const val SEP_PARA = '\u0002'
private const val SEP_FIELD = '\u0003'

/** Undo the escaping the corpus applies to every leaf string. */
private fun String.unescaped(): String {
    val out = StringBuilder(length)
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c == '\\' && i + 1 < length) {
            i++
            out.append(
                when (this[i]) {
                    'n' -> '\n'
                    't' -> '\t'
                    else -> this[i]
                }
            )
        } else {
            out.append(c)
        }
        i++
    }
    return out.toString()
}

private fun String.items() = split(SEP_ITEM).filter { it.isNotEmpty() }

private data class Original(val whenMs: Long, val title: String, val text: String)

/**
 * Replays a day of real Originals against the rules the app ships, over a corpus pulled by
 * `tools/larklish-helper replay fetch`. Experiments 10 and 11 scored the same thing in Python,
 * which measured a *copy* of the rules; this measures `pickMessage`, `postText`, `unwrapHtml`,
 * `resolveMentions` and `Preview.parse` themselves.
 *
 * The corpus is real colleagues' messages, so it is never committed. Without it this test skips.
 * Pull one, then read the breakdown:
 *
 *     tools/larklish-helper replay fetch
 *     ./gradlew testDebugUnitTest --tests '*ReplayTest*' -i
 */
class ReplayTest {
    private val corpus: File? =
        sequenceOf(
                System.getProperty("larklish.corpus"),
                "../replay-corpus",
                "replay-corpus",
            )
            .filterNotNull()
            .map(::File)
            .firstOrNull { File(it, "originals.tsv").exists() }

    private fun rows(name: String) =
        File(corpus, name).readLines().filter { it.isNotBlank() }.map { it.split("\t") }

    private fun candidatesByChat(): Map<String, List<Candidate>> =
        rows("messages.tsv").groupBy({ it[0] }) { r ->
            val kind = r[1]
            val deleted = r[3] == "1"
            val mentions =
                r[4].items().associate {
                    val f = it.split(SEP_FIELD)
                    f[0].unescaped() to f[1].unescaped()
                }
            val payload = r.getOrElse(5) { "" }
            val text =
                when {
                    deleted -> ""
                    kind == "text" -> unwrapHtml(payload.unescaped())
                    kind == "post" ->
                        payload.split(SEP_PARA).let { parts ->
                            postText(
                                parts[0].unescaped(),
                                parts.drop(1).map { para ->
                                    para.items().map {
                                        val f = it.split(SEP_FIELD)
                                        PostElement(
                                            f[0].unescaped(),
                                            f[1].unescaped(),
                                            f[2].unescaped(),
                                            f[3].unescaped(),
                                        )
                                    }
                                },
                            )
                        }
                    else -> ""
                }
            Candidate("", kind, r[2].toLong(), deleted, resolveMentions(text, mentions))
        }

    @Test
    fun theShippedRulesFindTheMessageBehindMostOriginals() {
        assumeTrue("no corpus — run `tools/larklish-helper replay fetch`", corpus != null)
        val messages = candidatesByChat()
        val titles =
            rows("titles.tsv").associate { it[0].unescaped() to it.getOrElse(1) { "" }.items() }
        val dms = rows("dms.tsv").associate { "dm:" + it[0].unescaped() to it[1] }
        val originals =
            rows("originals.tsv").map {
                Original(it[0].toLong(), it[1].unescaped(), it[2].unescaped())
            }

        // The policy MessageFetcher.fullTextOf runs, over a corpus instead of the network.
        val cache = HashMap<String, String>()
        val recent = ArrayDeque<String>()
        fun pickIn(chatId: String, stem: String, whenMs: Long): Pick =
            pickMessage(messages[chatId].orEmpty(), stem, whenMs).also {
                if (it is Pick.Found) {
                    recent.remove(chatId)
                    recent.addFirst(chatId)
                }
            }

        val rows = originals.map { o ->
            val preview = Preview.parse(o.text)
            val isDm = o.title == "Lark" || o.title == preview.sender
            val key = if (isDm) "dm:" + preview.sender else o.title
            val candidates =
                cache[key]?.let { listOf(it) }
                    ?: if (isDm) listOfNotNull(dms[key]) else titles[o.title].orEmpty()
            var outcome: Pick = Pick.Skipped("no-chat")
            var hit: Pick? = null
            var hitChat = ""
            candidates.forEachIndexed { i, chatId ->
                if (hit == null) {
                    val pick = pickIn(chatId, preview.stem, o.whenMs)
                    if (i == 0) outcome = pick
                    if (pick is Pick.Found) {
                        cache[key] = chatId
                        hit = pick
                        hitChat = chatId
                    }
                }
            }
            if (hit == null && candidates.size == 1) cache[key] = candidates[0]
            if (hit == null) {
                for (chatId in recent.take(RECENT_IN_REPLAY)) {
                    val pick = pickIn(chatId, preview.stem, o.whenMs)
                    if (pick is Pick.Found) {
                        hit = pick
                        hitChat = chatId
                        break
                    }
                }
            }
            when (val p = hit ?: outcome) {
                is Pick.Found -> "found\t$hitChat\t${p.candidate.createTime}"
                is Pick.Skipped -> "${p.reason}\t\t"
            }
        }
        // One line per Original — index, outcome, chat, create_time — for the diff against the
        // Go replay (Layer 7). Message ids are not in the corpus, so the chat and time name the pick.
        File(corpus, "outcomes-kotlin.tsv").writeText(rows.mapIndexed { i, r -> "$i\t$r\n" }.joinToString(""))
        val outcomes = rows.map { it.substringBefore("\t") }

        val found = outcomes.count { it == "found" }
        println("replay: $found of ${originals.size} Originals resolved")
        outcomes
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .forEach { (reason, n) -> println("  ${n.toString().padStart(4)}  $reason") }
        // Experiment 11 measured 119 of 210 for these rules. The floor allows for a corpus
        // pulled over a different span; a real regression drops it far below this.
        assertTrue("only $found of ${originals.size} resolved", found * 2 >= originals.size)
    }
}

/** Mirrors `MessageFetcher.RECENT_TRIES`, which is private to the main source set. */
private const val RECENT_IN_REPLAY = 2
