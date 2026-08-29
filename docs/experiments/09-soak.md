# 🧪 Experiment 09 — Soak 2026-08-27 → 28: Layer 5 for a day

Source: `events.jsonl` (431 events, pulled 2026-08-28 16:21 PDT), read with
`tools/events --since 2026-08-27T23:09` (the Layer 5 build went live 16:09 PDT).
**92 Relays, 91 of them real traffic** (1 probe). Every root cause below is reproduced
against the live Open API today, not guessed from the record.

## The grade

| Measure | Value |
| --- | --- |
| Relays | 92 |
| **Updated** | **34 (37 %)** — `post` ×23, `text` ×11 |
| Skipped | 57 — `no-chat` ×21, `no-message` ×17, `no-match` ×10, `type:interactive` ×6, `type:sticker` ×1, `type:video_chat` ×1, `error` ×1 |
| Update latency (34) | median **2.3 s**, max 20 s |
| `no-message` give-up | median **1.8 s** after the Original |
| Truncated Previews (`...`) | 39; **16 of them Updated (41 %)** |
| ML Kit fallback (`~`) | 11, all inside one 2.5 h block |
| Crashes | 0 |

Two chats produce every success: `San Jose 1199…` (21 ok / 7 fail) and the DM title `Lark`
(9 ok / 18 fail). Fifteen other chats scored 0. So the Update works, and the **chat
resolver is the bottleneck**.

`post` is 2 of every 3 Updated messages. Experiment 08 called this; the day confirms it.

## 1. Thread replies can never resolve a chat (≈11 `no-chat`)

`San Jose 1199 Coleman Avenue - Office Life` and `Seller Center - FE Oncall` have
`chat_mode: topic`. For a reply inside a thread, Lark puts **the thread's root text in the
title**, not the chat name: `I have a blue bag...`, `Boba Order`, `Afternoon Tea and...`,
`Zhifu Liu, Max...`. `chats/search` returns 0 useful hits for those.

```
GET /open-apis/im/v1/chats/search?query=I have a blue bag  → 2 hits, neither is a chat
GET /open-apis/im/v1/chats/search?query=Afternoon Tea and  → 0 hits
```

The messages themselves are reachable. `im/v1/messages?container_id_type=chat` on a
**topic** chat returns the thread replies too, newest first, mixed across threads:

```
16:04:28 text  thread=omt_19f78622fb8f5928  we're really falling victim to the gacha
15:50:12 text  thread=omt_19f798f7730f076b  also selling 1 ticket for 9/4 …
```

Only the chat identity is missing. The Sender and the time are known, and the chat is
almost always one Larklish already resolved.

## 2. A truncated title matches many chats, and the cache keeps the first (6 `no-message`)

```
GET /open-apis/im/v1/chats/search?query=【bytedcli MR 处理】 → 6 hits
  oc_7d94…  【bytedcli MR 处理】!2658 fix: use real command for quick start   ← cached
  oc_466f…  【bytedcli MR 处理】!3570 fix(tika): keep the conversation's mode…
  oc_f549…  【bytedcli MR 处理】!3567 feat(cli): support global --jq filter
  … 3 more
```

`groupChatId` takes the first prefix hit and `remember()` writes it to `chats.json`
forever. Every later MR notification then reads a chat whose newest message is from
08-26 → `no-message`. The prefix rule needs a message to disambiguate, and an ambiguous
title must not be cached.

## 3. `no-message` is mostly give-up-too-early

`fullTextOf` fetches the message list **once**, median 1.8 s after the Original.
`dmChatId` already polls (4 tries, linear backoff); the message list does not.

## 4. The stem match is too strict (≈7 of 10 `no-match`)

Both causes reproduced from the real messages:

- **Emoji.** `{"tag": "emotion", "emoji_type": "Delighted"}` → `postText` writes
  `[emotion]`, Lark's Preview writes `[Delighted]`. Same for `[Smirk]`, `[Laugh]`.
- **The Preview is not a faithful prefix.** Rachel Lam's post is
  `"Hi " + at(user_name "All") + " Happy Friday! " + bold("FOR SAN JOSE OFFICE ONLY")`,
  and Lark's Preview reads `Hi @all Happy Friday!Hope everyone has started packing...` —
  lower-case `@all`, and the trailing bold run is **dropped**.

`startsWith` on the whole stem fails on both. A case-insensitive match on a bounded prefix
(~12 squashed characters) passes both, and the ±15 s window keeps it unambiguous.

## 5. The ML Kit fallback fires on transient network errors, and it makes text worse

All 11 fallbacks sit in one 2.5 h block (19:09 → 21:29 PDT). The trigger was not
`out == text`: the same strings translate well through Lark today.

| Text | Lark today | ML Kit that evening |
| --- | --- | --- |
| `@bytedcli 历史 MR 处理 定时跟进 Codebase MR !3568「fix(tika)…」` | `@Bytedcli Historical MR processing, follow up Codebase MR! 3568 "fix (tika)…"` | `~@BYTEDCLI History MR Handling Timing Follow CodeBase MR! 3568 "FIX (Tika): …"` |
| `【bytedcli MR 处理】…` | `[bytedcli MR processing]! 3568 fix (tika)` | `~[BytedCli MR] …` (drops 处理) |

The failure is at the transport layer, and it repeats. One live call today failed the same
way and the immediate retry succeeded:

```
Post "https://accounts.feishu.cn/oauth/v3/token": read: connection reset by peer
```

So Lark deserves a retry before ML Kit takes over. `FallbackTranslator` also records no
reason, so the record cannot tell an outage from a bad input.

## 6. Person DMs also carry the title `Lark`

`[Lark] Emily Xie: Yes` is a person, not a bot. The plan expected `title == Sender` for
people; that never happened. `title == "Lark"` covers both, so `chatId()` is already
right — but the comment in `MessageFetcher.kt` is not.

## 7. Small facts

- 10 of the 21 `no-chat` rows have the title `Lark`, and nearly all are notifications with
  no message to fetch: `[Sticker]`, bot cards, `… started the meeting "…"`, recalls.
- 4 skips are `… recalled a message.` — the Update has nothing to add there.
- A reaction posts the quoted message as the title: `[[image]] Emily Xie responded to your
  message: [Laugh]`.
- `type:interactive` ×6 are the ByteCanteen, Approval and Bits bot cards.
- Withdrawals: 58 of 61 removals are `app_cancel_all` (Lark clearing its own shade).

## What the fixes are worth

| Fix | Recovers |
| --- | --- |
| Retry the message list, like `dmChatId` already does | ~5 `no-message` |
| Case-insensitive prefix match + `emotion` → `[emoji_type]` | ~7 `no-match` |
| Disambiguate a prefix title by its messages; never cache an ambiguous one | 6 `no-message` |
| Resolve an unknown title against the recently used chats (threads) | ~11 `no-chat` |
| Retry Lark before ML Kit, and record the reason | 11 fallbacks |

34 / 91 → about 63 / 91. The rest are cards, stickers, recalls and system notices, which
carry no Full text at all.
