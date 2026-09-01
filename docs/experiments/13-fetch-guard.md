# 🧪 Experiment 13 — Is an uncut Preview really the whole message?

Layer 5 fetches the Full text for **every** Original. `plan.md` has carried the obvious
optimization since 2026-08-27: a Preview that Lark did not cut already holds the message, so
there is nothing to fetch. This measures whether that is true, over the record's
**257 Relays** (`events.jsonl`, 2026-08-25 → 31).

## How much is wasted

| | |
| --- | --- |
| Relays | 257 |
| Preview cut by Lark (`...`) | 119 (46 %) |
| **Preview complete** | **138 (54 %)** |

So **more than half of all fetches, and the translate call that follows each one, are spent
on a message we already have.**

## Is the uncut Preview complete?

Every Update whose Preview did **not** end in `...`, comparing the Preview stem against the
Full text the API returned (whitespace removed on both sides):

| | |
| --- | --- |
| Updates from an uncut Preview | 34 |
| **Full text identical to the Preview** | **33** |
| Full text longer | **1** |

The one exception is a `post` **with a title**:

```
Preview    富文本测试三                    (6 characters, no `...`)
Full text  富文本测试三 + two paragraphs   (50 characters)
```

Lark previews a titled `post` by showing **only its title**, and does not mark the cut. It
was a probe of ours, not real traffic, but the shape is real: any titled `post` is lost by
this guard. Nothing on the notification distinguishes it from a genuinely short message, so
the guard cannot be made exact — only abandoned or accepted.

## Decision

Accept it. 33 of 34 is the measured rate, the loss is one message shape, and the saving is
half of all API traffic — including half of the translate calls behind the `99991400` rate
limit that now drives the ML Kit fallback.

A skipped Relay records `skipped not-truncated`, so `tools/larklish events stats` still shows
the bucket and a later soak can price the exception instead of guessing at it.

⚠️ Reading a soak after this change: `not-truncated` is **by design**, not a failure. The
grade to read is Updates against Relays whose Preview *was* cut — the denominator Experiment
12 already argued for.
