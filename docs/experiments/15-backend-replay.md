# 🧪 Experiment 15 — The Go Lookup replays the corpus exactly like the Kotlin one

Layer 7 moves the Lookup (title → chat id → newest messages → pick → Full text) from
`MessageFetcher.kt` to a Go Backend. Max's gate: before the Kotlin code goes, replay the whole
existing corpus on both and compare per Original — same, or better in Go.

## Setup

- Corpus: `replay-corpus/` as pulled on 2026-08-31 (257 Originals, 24 titles, 4 DM senders,
  454 messages; real colleagues' messages, never committed).
- Kotlin: `ReplayTest.kt`, now writing one line per Original to `outcomes-kotlin.tsv`:
  `index, outcome, chat, create_time` (message ids are not in the corpus, so the chat and the
  time name the pick).
- Go: `backend/replay_test.go` runs the real `Fetcher.FullTextOf` over a `corpusSource` and
  writes `outcomes-go.tsv` in the same shape.

```sh
./gradlew testDebugUnitTest --tests '*ReplayTest*' -i
go -C backend test ./... -run Replay -v
diff replay-corpus/outcomes-kotlin.tsv replay-corpus/outcomes-go.tsv
```

## Result

| | Kotlin | Go |
| --- | --- | --- |
| found | 148 | 148 |
| `no-chat` | 61 | 61 |
| `no-message` | 30 | 30 |
| `type:interactive` | 12 | 12 |
| `no-match` | 5 | 5 |
| `type:video_chat` | 1 | 1 |

**`diff` prints nothing: 257 of 257 lines identical**, including the chat and the
`create_time` of every found message.

## The one place the two could have differed

Kotlin's `take(12)` / `takeLast(12)` count UTF-16 units; Go counts runes. An emoji is two
units and one rune, so a stem with an emoji inside its first 12 folded characters gets a
needle one character shorter in Kotlin, and the `needle.length == 12` test that widens the
window to 5 min can flip. Measured before the port: **5 of 257 stems** carry such an emoji.
None of the 5 changed outcome — the extra character never disagreed with the Full text, and
the window reach never decided the pick. Go keeps runes on purpose: a character is a character,
and the unit count was an accident of the JVM.

Other semantics checked and found equal for this text: `unicode.IsSpace` vs Kotlin's
`isWhitespace` (differ only on `U+0085` and `U+001C–U+001F`, absent from the corpus),
`strings.ToLower` vs `lowercase()` (zh/en only), RE2 vs `java.util.regex` for
`@_(user_\d+|all)` and `</?[a-z][^>]*>`, and `strings.Split` keeping trailing empty fields
like Kotlin's `split`.

## Verdict

Same. `MessageFetcher.kt`, `PickMessageTest`, `MentionsTest`, `MessageTextTest` and
`ReplayTest` can go once the app calls the Backend (commit 7).
