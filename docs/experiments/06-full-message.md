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

## E2 — Source B: Open API by chat, under the user identity

All calls `lark-cli api … --as user` from the Mac (the CLI's user identity auto-refreshed).

- ✅ **Chat by title.** `GET /open-apis/im/v1/chats/search?query=<title>`: the exact title
  `San Jose 1199 Coleman Avenue - Office Life` → 1 hit (`oc_7be4…`). The truncated title
  Lark puts in the Original (`San Jose 1199...`) → 2 hits; pick the one whose `name`
  starts with the prefix. Han title `高优` → 8 hits, the right group among them, so
  filter by name there too. 2.5 s for the first call (token refresh), then ~1 s.
- ✅ **Latest messages.** `GET /open-apis/im/v1/messages?container_id_type=chat&
  container_id=oc_…&sort_type=ByCreateTimeDesc&page_size=3` → `items[].body.content` is
  JSON `{"text": "…"}` for `text` messages; `msg_type`, `create_time` (ms), `sender`
  (open_id) come with it. Mentions appear as `@_user_1` placeholders; the `mentions`
  array maps them to names. ~1.9 s through lark-cli.
- ✅ **Latency.** Bot DM sent → the message was in the list on the **first poll,
  3.0 s later** (lark-cli start-up included), full 70-character text intact while the
  phone's Original showed 31 characters + `...`.
- ✅ **Burst.** 10 sequential list calls: no error code; 17 s total = 1.7 s each, dominated
  by CLI start-up (raw HTTP is ~0.4–0.8 s per Experiment 05).
- ⏳ Not run: a **DM** whose title is the person's name (or `Lark` for bots) — whether
  `chats/search` returns p2p chats by the person's name [CONFIRM]. Fallback: the
  `contacts` search by name → `open_id` → `chats` with `p2p` type.

## E4 — User token lifetime

`lark-cli auth status`: user access token 2 h (`expiresAt`), refresh token **7 days,
sliding** (`refreshExpiresAt` = last refresh + 7 d), scope list includes `offline_access`.
A phone app that refreshes at least weekly stays signed in; the sign-in itself is the
device-code flow lark-cli uses (verification URL + code, one tap on the phone).

## Answer so far

The Original cannot carry the full message (E1) and Lark cuts it to ≤ 45 characters,
often to nothing (E5). Source B works: title → `chats/search` → `messages` list gives the
full text within ~3 s of the post, under the user identity, whose token can live on the
phone (E4). Open: DM lookup (E2 ⏳), and Max's call on doing per-notification lookups
under the user identity (the handoff rejected *polling*; this is one lookup per Original).

## Sketch (not a plan yet)

`LarkListener` → `Preview.parse` → if the Preview ends in `...` (or is empty), fetch:
chat id (cached per title) → latest message whose `create_time` ≥ Original `when` − 5 s
and whose text starts with the Preview stem → translate the full text → Relay with
`BigTextStyle` (≤ ~450 characters shown expanded; Lark's translate limit is 1,000).
Otherwise relay as today. A user token on the phone: device-code sign-in screen in
`MainActivity`, refresh weekly.
