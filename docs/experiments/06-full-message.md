# 🧪 Experiment 06 — Where can the full message come from?

Date: 2026-08-26. Zero code: `dumpsys`, `lark-cli` bot DMs, `tools/events`.
Question behind it (Max): Lark notifications show only the first line, so they cannot be
read aloud or expanded. Could Larklish show the full message, or the first few lines?

## E1 — Does the Original carry more than `EXTRA_TEXT`?

- Extras keys of a live Original: `android.title android.text android.bigText
  android.subText(null) android.infoText android.largeIcon android.template
  android.reduced.images android.remoteInputHistory android.appInfo android.showWhen
  android.showChronometer android.progress* androidx.core.app.extra.COMPAT_TEMPLATE`.
  No `textLines`, no `messages`, no Lark-private key. `bigText == text`.
- The tap intent targets `com.ss.android.lark.notification.export.transfer.
  NotificationTransferActivity (has extras)`. `dumpsys activity intents` prints the
  component but never the extras, with or without root. The chat/message id is in there,
  unreadable from outside Lark.
- ❌ **Answer: no.** The Original is the truncated string and an opaque intent.

## E5 — Lark's truncation rule

Ten bot DMs (`lark-cli im +messages-send --as bot`), read back from `events.jsonl` with
`tools/events --since … list`. Lengths are code points of the message part (after
`Sender: `).

| Probe | Sent | Posted | Note |
| --- | --- | --- | --- |
| a | 249 Han, no punctuation | `...` (empty) | nothing before the budget to cut at |
| b | 2 lines (16 + 32) | line 1, no `...` | newline ends the Preview |
| c | 46 Han, `，` at 34 | 34 + `...` | cut back to the last `，` |
| d | 45 Han, ends with `。` | whole | fits the budget exactly |
| e | 141 Han, first `，` at 120 | `...` (empty) | same as a |
| f | 110 Han, `，` every 11 | 43 + `...` | cut at the 4th `，` (position 44), comma dropped |
| g | 59 Han, no punctuation | `...` (empty) | same as a |
| h | 31 Han, no punctuation | whole | under the budget |
| i | 228 Latin, spaces only | 41 + `...` | cut back to a space |
| j | 109 Latin, commas | 44 + `...` | cut back to a space, not the comma |

**Rule:** keep at most **45 characters** of the message; if the message is longer, back up
to the last boundary before position 45 (space for Latin; `，。、：` for Han; a newline
always ends it), drop the boundary, append `...`. **No boundary before 45 → empty
Preview (`Sender:...`).** Han and Latin count the same (code points). Earlier runs agree
(Experiment 01: 29 and 48 message chars; Experiment 03: an in-budget message came whole).

Consequences:
- A Chinese message without punctuation in its first 45 characters shows **nothing**.
  That is common in chat. Max's complaint is worse than "first line only".
- The Preview is at most ~45 characters, so the Relay can never carry more than that from
  the Original alone. The full text must come from somewhere else.

## Remaining sources (not run yet)

- **B. Open API by chat** (user identity): title → chat id → latest messages → text.
  Needs E2 (lookup + timing with `lark-cli --as user`) and E4 (user token lifetime on the
  phone). Message-read scopes are already granted on the CLI app.
- **C. Open API by text search**: `search:message` with the truncated Preview. Needs E3
  (index latency). Useless for the empty-Preview case above, which is exactly the case
  that hurts most; B does not have that problem.
