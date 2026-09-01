package com.vegerot.larklish

enum class Mention { YOU, ALL }

/** What Lark appends when it cuts a message at 45 characters (Experiment 06). */
private const val ELLIPSIS = "..."

/**
 * A Lark Preview split into its parts. Lark posts `text` as `Sender: message`
 * (Experiment 00); a mention makes it `Sender@you: message` or `Sender@all: message`.
 * The Sender never goes through the Translator (Experiment 02).
 */
data class Preview(val sender: String, val mention: Mention?, val message: String) {
    /** The message without Lark's `...`; what the Full text must start with (Layer 5). */
    val stem: String get() = message.removeSuffix(ELLIPSIS)

    /**
     * Lark cut the message, so the Full text holds more than this Preview does. When it did
     * not cut, the two are the same and there is nothing to fetch (Experiment 13).
     */
    val truncated: Boolean get() = message.endsWith(ELLIPSIS)

    companion object {
        fun parse(text: String): Preview {
            // No boundary in the first 45 characters → Lark posts `Sender:...` (Experiment 06 E5).
            if (text.endsWith(":...") && !text.contains(": ")) return parse(text.dropLast(3) + " ...")
            val split = text.indexOf(": ")
            if (split < 0) return Preview(sender = "", mention = null, message = text)
            val head = text.substring(0, split)
            val message = text.substring(split + 2)
            val mention = when {
                head.endsWith("@you") -> Mention.YOU
                head.endsWith("@all") -> Mention.ALL
                else -> null
            }
            val sender = if (mention == null) head else head.dropLast("@you".length)
            return Preview(sender, mention, message)
        }
    }
}
