# 🧪 Experiment 16 — Shared replay rules and recorded truncation

Date: 2026-09-09. Phone: Pixel 4a. Code: `Recorder`, `LarkListener`,
`tools/larklish_helper.py`, `backend/replay_test.go`.

## Changes

- Python writes raw API messages as JSONL; the Go replay decodes the SDK message
  struct and calls production `candidateOf`. The custom TSV encoding and decoder are gone.
- The phone records `Preview.truncated` on each Relay. The helper reads that decision;
  records without it are unknown and explicitly excluded from grading.
- Sharing the Backend's text rendering with `msgs` remains a TODO in `docs/plan.md`.

## Verification

- `./gradlew ktfmtFormat testDebugUnitTest installDebug` succeeded and installed on the
  Pixel. The 21 Kotlin unit tests passed using Gradle's existing cached results.
- Ruff formatting and lint passed. A mixed-record check verified one unknown, one cut,
  and one complete Preview: only the cut Preview entered the Update denominator.
- The long test message arrived complete even with `probe --idle`; its Relay at
  `23:03:27Z` recorded `truncated=false` and `skipped not-truncated`.
- A second test deliberately ended in `...`. Its Relay at `23:04:01Z` recorded
  `truncated=true`, then received an Update through ByteFaaS at `23:04:07Z`. This checks
  the phone's decision and recording; it does not demonstrate natural Lark truncation.
- `events grade` reported 746 Relays: 1 cut, 1 complete, 744 without recorded truncation
  excluded; Updated 1 of 1 cut Previews. The old records were preserved.

## Replay comparison

Rebuilt the interrupted sample using the same 165 Originals and saved DM resolution:

```sh
tools/larklish-helper replay fetch \
  --file replay-corpus/source-events.jsonl \
  --chats replay-corpus/source-chats.json \
  --out replay-corpus
go -C backend test ./... -v
```

The corpus contains 324 raw messages across 28 chats, including 178 `text`, 88 `post`,
and 32 `interactive` messages. Every raw message's chat id belongs to the corpus.
The real messages remain ignored in `replay-corpus/`.

All 14 other Go tests passed. The replay resolved **67 of 165**, failing its existing
50% threshold: 71 `no-chat`, 9 `type:interactive`, 9 `no-match`, 7 `no-message`,
1 `type:system`, and 1 `type:image`. Of the 71 `no-chat` rows, 60 have title `Lark` (DMs).

To distinguish a refactor regression from the sample's existing misses, loaded the
helper at `2ca2aa26d4ec` with `sl cat --rev 2ca2aa26d4ec`, encoded the same raw data with
its TSV writer, and ran the committed replay through a temporary Go `-overlay`. Both versions resolved
67 of 165 and **all 165 outcome lines were identical**. The old replay failed the same
threshold. The refactor preserves outcomes; the full Go suite still reports this
pre-existing, sample-dependent failure. The threshold was left unchanged.
