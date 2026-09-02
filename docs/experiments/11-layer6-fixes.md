# 🧪 Experiment 11 — All five Layer 6 fixes, measured

Experiment 09 proposed five fixes and guessed what each was worth. This experiment
measures all five on the replay harness of Experiment 10 (`tools/replay/`), over the same
<!-- Superseded 2026-08-28: the Python harness became `ReplayTest.kt` plus
     `tools/larklish-helper replay fetch`, so it now scores the shipped rules instead of a copy.
     The numbers below were produced by the Python originals. -->
**210 Relays** (2026-08-26 → 28). Four of the five guesses were wrong.

## Guess vs. measurement

| Fix | Guessed | **Measured** | |
| --- | --- | --- | --- |
| 1 Retry the message list | ~5 | **+0** | the retry is worthless; the *window* is the bug |
| 1' Widen the window's backward edge | — | **+6** | not proposed, costs nothing |
| 2 Loose match + `emotion` | ~7 | **+7** | the prefix match carries all of it |
| 3 Disambiguate prefix titles | 6 | **+1** | |
| 4 Thread resolver (LRU 2) | ~11 | **+3** | Experiment 10 |
| 5 Retry Lark before ML Kit | 11 | **10 of 11 Relays** | not a fetch fix |

```
fix, alone                                            found    Δ   GETs
baseline (today's code)                                 101        170
1  retry the message list (forward edge +4 s → +60 s)   101   +0    170
1' widen the backward edge (15 s → 60 s)                107   +6    170
2a emotion → [emoji_type]                               103   +2    170
2b case-insensitive 12-char prefix                      108   +7    170
3  disambiguate prefix titles                           102   +1    213
4  LRU 2 fallback on no-chat                            104   +3    375

stacked, in the order worth building                  found    Δ   GETs
+ 1' window 60 s                                        107   +6    170
+ 2b prefix match                                       115   +8    170
+ 2a emotion                                            115   +0    170
+ 4  LRU 2                                              118   +3    350
+ 3  disambiguate                                       119   +1    367

101 → 119 of 210 Relays (48 % → 56 %)
```

## Fix 1 is the wrong fix. The window is too narrow.

A retry only widens the **forward** edge, and the forward edge is already generous:
moving `AFTER_MS` from 4 s to 60 s changes nothing at all. Not one message arrives after
its Original.

The **backward** edge is the one that cuts. Measured over the 116 matched Relays, the delay
from message to Original is:

| p50 | p75 | p90 | p95 | p99 | max |
| --- | --- | --- | --- | --- | --- |
| 5.1 s | 7.0 s | 10.2 s | 24.3 s | 39.8 s | 43.9 s |

93 % arrive inside 15 s — which is why E2's "~3 s" looked right — but the tail runs to
44 s. `BEFORE_MS = 15_000` throws away the top 7 %. Sixty seconds catches all of it and
saturates there (120 s and 300 s find nothing more). It costs **no extra API calls** and
introduces **no ambiguity**: with a `[-60 s, +15 s]` window, 0 of 114 stems match in more
than one of 35 chats.

So fix 1 is one constant, not a retry loop.

## Fix 2: the prefix match carries it; keep `emotion` for the reader

`emotion` → `[emoji_type]` alone is +2, and the prefix match subsumes both. Keep it anyway
— it is what the Relay *shows*: `[Delighted]` instead of `[emotion]`, matching Lark.

## Fix 5: Lark would have kept the quality

Re-translating the 18 Han fragments behind the 11 fallback Relays, through Lark, today:

- **16 translate well** — a retry would have kept Lark's output for 10 of the 11 Relays.
- **1 is genuinely unchanged** (`MR #3567 逾期提醒：feat(cli): support global --jq filter`)
  and trips the `out == text` guard for real. Its near-identical sibling `#3568`
  translates fine, so Lark's own language detection is the flaky part.
- **1 failed live, inside this experiment**, with a transport error — while the same text
  with one extra space succeeded. The failure is real and transient.

Baseline failure rate: **0 of 60** sequential calls (73 s). Failures cluster rather than
spread, which matches the single 2.5 h block in the record.

## Three behaviours the harness exposed

- **Messages get edited, and the API returns the edited text.** `Ni Sun`'s Original read
  `有没有黄包想换灰包的`; the message now reads
  `有没有黄/紫/蓝包想换灰包的(gray for yellow/purple/blue)`. Lark then re-posts the Original
  with the new Preview. A stem match against an edited message can never succeed, so some
  `no-message` rows are unfixable — and a replay of old data understates any matcher.
- **`text` bodies can contain HTML**: `<p>I have a black bag from open house…`. 11 of 198
  `text` messages carry tags. Lark's Preview strips them; `candidateOf` does not.
- Lark re-posts an Original when the message is edited, on the same key — the per-key
  cancellation (Q15) already handles it.

## Verdict — build Layer 6 in this order

1. **`BEFORE_MS` 15 s → 60 s** (+6, one constant)
2. **Case-insensitive 12-char prefix match** (+8 on top, ~5 lines)
3. **`emotion` → `[emoji_type]`** (+0 matches, better Relay text)
4. **Retry Lark once before ML Kit, and record the reason** (10 of 11 fallbacks)
5. **LRU 2 on `no-chat`** (+3, ~10 lines, +180 GETs)
6. **Disambiguate prefix titles** (+1, ~15 lines) — optional
7. ~~Retry the message list~~ — **drop it**, worth 0

Strip HTML from `text` bodies too; it is one line and the record shows 11 of 198 messages
need it.
