# 🧪 Experiment 10 — Which thread resolver? (Layer 6 fix 4)

Experiment 09 found that a reply inside a **topic** chat puts the *thread's root text* in
the Original's title, so `chats/search` cannot resolve it. Three ways to recover the chat
were on the table. This experiment replays the whole Relay record through all three and
counts what each recovers.

## Method

Offline replay, no phone. Scripts in `tools/replay/` (throwaway):
<!-- Superseded 2026-08-28: the Python harness became `ReplayTest.kt` plus
     `tools/larklish-helper replay fetch`, so it now scores the shipped rules instead of a copy.
     The numbers below were produced by the Python originals. -->

1. `search.py` — every distinct group title (24) through `chats/search`, keeping **all**
   prefix hits, not just the first.
2. `fetch.py` — `im/v1/messages` for every chat that any title or the phone's `chats.json`
   points at (35 chats), `start_time`/`end_time` over 2026-08-26 → 28. 3 chats refuse
   (`code 230002`, Max is not a member).
3. `strategies.py` — ports `postText`, `resolveMentions` and `pickMessage` to Python, then
   replays all **210 Relays** and scores each strategy by messages found and by extra
   `im/v1/messages` GETs.

The replay reads today's fully indexed API, so it assumes a perfect retry (fix 1) and no
network failures. The window is `[relayed − 16 s, relayed + 4 s]`; widening it to 40 s
moves the totals by 2, so the result is not sensitive to it.

## The ceiling: 3 messages in 3 days

Twelve Relays have a title that `chats/search` cannot resolve. Only **3** of them have a
message worth fetching:

| Rows | Title | What it is | Fetchable |
| --- | --- | --- | --- |
| 3 | `I have a blue bag...` | thread replies in `San Jose 1199…` | ✅ |
| 4 | `I have a blue bag...` ×2, `[image]`, `My project is going...` ×2 | recalls, and reactions (`… responded to your message: [Laugh]`) | ❌ |
| 2 | `Boba Order`, `Afternoon Tea and...` | calendar `[Reminder]` notices | ❌ |
| 3 | the rest | recalled-message notices | ❌ |

So no strategy can beat **+3 in three days ≈ 1 per day**.

## The three strategies

210 Relays, loose match (case-insensitive, 12 squashed characters):

| Strategy | Found | GETs | vs baseline |
| --- | --- | --- | --- |
| baseline — `chats/search` only | 108 | 170 | — |
| **A: LRU 1** | 109 | 206 | +1 for +36 GETs |
| **A: LRU 2** | **111** | **241** | **+3 for +71 GETs** |
| A: LRU 3 | 111 | 273 | +3 for +103 |
| A: LRU 5 | 111 | 328 | +3 for +158 |
| **B: scan every cached chat** | 111 | 448 | +3 for +278 GETs |
| **C: thread-root cache alone** | 108 | 170 | **+0** |
| C + A(3) | 111 | 275 | +3 for +105 |

- **A(2) wins.** Two chats is the whole prize; 3, 5 and "all" add nothing. A thread reply
  always follows other traffic in the same chat, so the right chat is at the front of the
  LRU list.
- **B buys nothing for 4× the calls.** The cache reached 35 chats in three days and keeps
  growing; B's cost grows with it, its yield does not.
- **C alone recovers nothing**, exactly as expected: it can never earn the first hit. Put
  on top of A it *added* 2 GETs on this record, because a thread title rarely repeats
  while its chat is still cold. Drop it.

## Safety

For all 105 Relays whose stem matches anything, the stem matched in **exactly one** of the
35 chats. No stem matched two. A 12-character case-insensitive prefix inside a ±20 s
window identifies a chat on its own, so the LRU fallback cannot pick the wrong chat here.

## Two things the same harness settled

- **Do not run the fallback after `no-message` / `no-match`.** It recovers **+0** and costs
  +134 GETs (A) or +415 GETs (B). Only `no-chat` deserves it.
- **The loose match is worth more than this whole fix**: +7 on its own (101 → 108 over the
  210 Relays). Prefix disambiguation (fix 3) is worth **+1** — most `【bytedcli MR 处理】`
  messages are `interactive` cards with no text, so the right chat does not help.

## Verdict

Do fix 4 as **A with N = 2**: on `no-chat`, try the two most recently used chats, first
stem match wins. ~10 lines, +0.34 GETs per Original, +1 Update per day. It is the smallest
of the Layer 6 fixes — the loose match is 2× the value for the same effort.
