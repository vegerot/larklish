package com.vegerot.larklish

/**
 * A Lark Preview split into its parts. Lark posts `text` as `Sender: message`
 * (Experiment 00). A mention makes it `Sender@you: message` or `Sender@all: message`.
 *
 * Why split: Experiment 02 shows both translators damage the Sender when it is part of the
 * sentence (ML Kit: `Jiarui` → `Jiaroi`; Lark: dropped `Yufan Chen: ` entirely).
 * The plan: translate only [message], then put [sender] back in front.
 */
data class Preview(val sender: String, val message: String) {
    companion object {
        /**
         * TODO(Max): split a Lark `text` into Sender and message.
         *
         * Decide:
         * - Split at the first `: `? (Messages can contain `: ` too, but the Sender never
         *   contains `: `, so the first one is the boundary.)
         * - The `@you` / `@all` suffix on the Sender: keep it in [sender], drop it, or
         *   move it into [message]? It is the only signal that the Original is a mention.
         * - No `: ` at all (a bot could post plain text): whole text as [message] with an
         *   empty [sender], or treat it as an error?
         */
        fun parse(text: String): Preview = TODO()
    }
}
