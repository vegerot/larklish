# 🧪 Experiment 12 — Soak 2026-08-28 → 31: Layer 6 on real traffic

Source: `events.jsonl` (563 events, pulled 2026-08-31 13:26 PDT), read with
`tools/larklish-helper events --since 2026-08-29T04:30` (the Layer 6 build went live 08-28 21:18).
**27 Relays over 2.6 days** — a weekend, so the volume is a third of Experiment 09's day.
Every root cause below is reproduced against the live Open API, not read off the record.

## The grade

| Measure | Experiment 09 (Layer 5) | **This soak (Layer 6)** |
| --- | --- | --- |
| Relays | 92 | 27 |
| Updated | 34 (37 %) | **12 (44 %)** |
| **Truncated Previews Updated** | **16 of 39 (41 %)** | **11 of 13 (85 %)** |
| Update latency | median 2.3 s, max 20 s | median 2.7 s, p90 4.4 s, max 11.9 s |
| `no-message` | 17 | **0** |
| `no-match` | 10 | 2 |
| `no-chat` | 21 | 10 |
| ML Kit fallback (`~`) | 11 | 2 |
| Crashes | 0 | 0 |

**Truncated Previews are the real denominator.** A Relay whose Preview already holds the
whole message loses nothing when the Update is skipped. 14 of the 15 skips are that case.
Of the 13 Relays that did need an Update, 11 got one.

`no-message` — the biggest failure bucket of Experiment 09, and the one the needle-dependent
window of 08-28 was built for — is **gone**. Not one Original in 2.6 days.

## 1. Every `no-chat` is a bot DM, and every one of them is a card

All 10: `Approval` ×5, `ByteCanteen` ×3, `IT Security Center` ×1, `Mail Assistant` ×1.
`messages/search` does find their chats — 15 p2p hits each — so the search is not the
problem. Two things are:

```
display_info  "字节餐厅\n字节餐厅: …picked up at the pickup point till 14:00…"
meta_data     {"is_p2p_chat": true, "type": "CARD", "chat_id": "oc_9a0c63e0753…"}
```

- **The peer name is localized.** `dmChatId` keeps the hit whose peer name is the Sender.
  The API says `字节餐厅`, `审批`, `IT安全中心`, `邮箱助手`; the phone's Lark says
  `ByteCanteen`, `Approval`, `IT Security Center`, `Mail Assistant`. They never agree.
- **The message is a `CARD` in all four cases.** So repairing the name would turn
  `no-chat` into `type:interactive` and recover **nothing**.

🚫 Do not build a bot-DM resolver. The whole bucket is worth zero messages.

## 2. Lark localizes mentions, so a message that opens with one never matches

The two `no-match` rows are the same recurring shape — a `post` in a `topic` chat:

```
Preview  Zhiqiang Wang: @Oncall Assistant MPA流水线发布卡住，重试之后失败
API      @Oncall 助手 MPA流水线发布卡住，重试之后失败      (create_time 6 s before the Original)
```

The chat resolved, the message was in the window, and the needle — the first 12 folded
characters, `@oncallassis` — could not match `@oncall助手mpa`.

A mention shows whichever of an account's names the reader prefers. It is not only bots:

| Preview (phone) | Full text (API) | |
| --- | --- | --- |
| `@Oncall Assistant` | `@Oncall 助手` | a bot, ×6 |
| `@all` | `@所有人` | ×2 — no `mentions` entry names it |
| `@Yan Fan @Lydia Chengcen Liu` | `@樊艳 @刘成岑 Lydia` | colleagues with two names |
| `@Yicheng Yang` | `@杨亦诚` | ×1 |

The two sides disagree on the opening and on **nothing else**.

### Three candidate rules, measured

Scored over the refreshed corpus — **257 Originals**, 2026-08-25 → 31 — with only the
matcher swapped (`tools/larklish-helper replay fetch`, then the scorer of this experiment):

```
rule                                    found   Δ    no-match
head (shipped)                            137        16
head + body after the leading mentions    146  +9      7
head + close of the stem                  148 +11      5
head + body + close                       148 +11      5
head + close, both sides opening with @   148 +11      5   ← shipped
```

- **body** — strip the mentions the message opens with, require what is left inside the
  stem. Needs the `mentions` array, and loses the two `@all` rows: no `mentions` entry
  names `@所有人`.
- **close** — require the last 12 folded characters of the stem inside the text. Needs no
  structure at all and catches every case.
- **the `@` guard costs nothing.** Requiring both sides to open with a mention keeps all 11
  and shuts the rule off everywhere else, so a shared close — a link, a sign-off — can
  never name the wrong message.

All 11 recovered picks were checked by hand: **every one is the right message**, differing
from its Preview only in the mention. `no-match` 16 → 5.

Shipped in `pickMessage`; `ReplayTest` scores the same 148 of 257 that the scorer predicts.

## 3. Both fallbacks are one rate limit, and the retry cannot help

```
java.io.IOException: Lark /open-apis/translation/v1/text/translate:
    http 400 code 99991400 request trigger frequency limit
```

Two, six hours apart — not a burst of ours: each Relay makes at most 3 translate calls and
the documented ceiling is 20 QPS. The quota belongs to the **app**, and the app is the fat
`lark-cli` app that other tools share (see Shortcuts in `plan.md`).

`FallbackTranslator` retries once with no pause. That retry was measured against
*transport* failures (Experiment 11) and is useless against a frequency limit — the second
call is immediate. Not changed: 2 events in 2.6 days, and the ML Kit fallback already
produced readable English both times. Revisit if the count grows, and pause before the
retry rather than widening it.

## What is left

| Bucket | n | Worth |
| --- | --- | --- |
| `no-chat` (bot DMs) | 10 | **0** — cards, §1 |
| `type:interactive` | 3 | 0 — a card has no text to show |
| `no-match` | 2 | fixed by §2 |

Every remaining skip in this soak is either fixed or worthless. The next real signal has to
come from a weekday soak on the new rule.
