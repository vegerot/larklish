# 🧪 Experiment 14 — A weekday soak of Layer 6 + the fetch guard

`tools/larklish-helper events --since 2026-09-01T03:50 stats`, 2026-09-01 03:50 → 09-02 21:06 UTC
(1.7 days, Tuesday and Wednesday in Beijing). **111 Relays.** Experiment 12 asked for a
weekday soak; this is it. The three things the last progress entry said to watch, in order.

## 1. The grade

`not-truncated` is by design (Experiment 13), so the denominator is Relays whose Preview
Lark cut.

| | |
| --- | --- |
| Relays | 111 |
| Preview complete → `skipped not-truncated` | 56 |
| **Preview cut** | **55** |
| Updated (`post` 23, `text` 8) | **31** |
| `no-chat` | 17 |
| `type:interactive` | 2 |
| canceled by a newer Original on the key | 2 |
| `no-match` | 3 |

Every one of the 17 `no-chat` rows is a `[Lark]` bot DM: Approval ×9, Mail Assistant ×2,
"started the meeting" ×3, ByteTech, Workplace Resources, Safety & Security, Kani. Experiment
12 closed that bucket — they are cards, worth 0 — and this soak agrees.

The two `type:interactive` rows are a `【bytedcli MR 处理】` reminder and a `值班提醒` from
`Feedback Assistant (VA)`: cards, correctly skipped. The two canceled Updates each had a
newer Original on the same key one second later, which is the rule working.

So of the **34 readable, cut Relays** (55 − 17 − 2 − 2), **31 Updated: 91 %.** Experiment 12
scored 11 of 13 on a weekend.

### The three `no-match`

| Original | Newest message in the window | Verdict |
| --- | --- | --- |
| Yinghao Li: `[share group]Yinghao Li, Raymond Kim, …` | a `share_chat` card | worth 0 |
| Haochen Yang: `✨ You're Invited! \| PDI‑GEC New Grad Training…` | an `interactive` card; a `text` 1 s later took the key | worth 0 |
| Rachel Lam@all: `Hi @all Labor Day event setup is ready! …` | the `text` message, body `Hi @_all Labor Day…` | **a bug** |

**A `text` body spells @all as the literal `@_all`, and the `mentions` array never names
it.** So `resolveMentions` left `@_all` in the Full text, the 12-character head became
`hi@_alllabor` against the Preview's `hi@alllabor`, and the head rule missed. The close rule
did not fire because only one side opens with `@` (the Preview opens with `Hi`).

Fix: `resolveMentions` now maps `@_all` → `@all`, the spelling the Preview uses. The same
`@_all` had leaked into the *text* of two Updated Relays (`Good morning @_all !`,
`@_all：请大家暂停…`), so the fix also cleans those. `ReplayTest` stays at 148 of 257 —
the corpus predates any `@_all` message — and a probe on the phone with
`<at user_id="all"></at>` reads `@all` in the Update.

## 2. The localized-mention rule on real traffic

Experiment 12 shipped the "close of the stem" rule unverified on the phone. **11 of the 31
Updates came through it**, all the right message:

| Preview opens | Full text opens | count |
| --- | --- | --- |
| `@Oncall Assistant` | `@Oncall 助手` | 7 |
| `@Weiyi Yang @Meng Sun` | `@杨伟一 @孙梦` | 1 |
| `@Dong Liu` | `@刘冬` | 1 |
| `@Liping Yu @Huacheng Sun` | `@余丽平 @孙华成` | 1 |
| `@all：` | `@_all：` | 1 |

A third of all Updates. Without the rule the grade would have been 20 of 34.

## 3. The rate limit

| | |
| --- | --- |
| Han texts sent to Lark | 81 |
| gave up after the retry on `99991400` | **14 (17 %)** |
| `NotTranslated` (Lark returned the input) | 6 |
| Relays with any ML Kit output | 17 of 111 |

Every `99991400` give-up sits in **Beijing working hours**:

```
Beijing 10h  3      Beijing 14h  1
Beijing 11h  6      Beijing 17h  4
```

Nothing between 18h and 09h Beijing, over two nights. Inside a bad hour about half the calls
fail, interleaved with successes; 30 of 30 probe calls succeeded at 14:22 PDT (05:22 Beijing).
This session ran no `lark-cli` during the soak, so the load is not ours.

The API doc settles whose quota it is:

> 单租户限流：20QPS，同租户下的应用没有限流，共享本租户的 20QPS 限流

**Per tenant, shared by every app in the tenant.** A dedicated Larklish app would draw from
the same 20 QPS as everything else ByteDance runs against `translation/v1/text/translate`.
The quota is busy when Beijing is at work, and nothing on our side changes that.

What we control is what happens after a `99991400`:

- The retry runs 0.8 s after the first call, inside the same busy second. A pause would
  help only if the bad windows are short; the hour table says they last minutes.
- Or post the ML Kit text at once (as now) and keep retrying Lark on a slower clock, then
  Update again when it answers. That keeps Lark's quality on the Relay a minute later.

Not changed. 17 of 111 Relays read ML Kit English; readable in every case checked.

### `NotTranslated` is a different problem

All 6 are Latin-heavy texts with a few Han words: `@Oncall 助手 …`, `MR #3245 逾期提醒：…`,
`N0.79 UK 4PL 时效提升…`, a bilingual apartment `post`. Lark returns them unchanged;
ML Kit then Title-Cases the English and mangles the identifiers, every time. This is the
Later item "Translate only the Han runs when Lark passes text through", now priced at 6 of
111 Relays.

## Also seen

- **The fetch guard lost nothing measurable.** Four uncut Previews looked like more than
  their Preview (`Labor Day Celebration…`, `📸 2026 Open House Photo Drop!`, a 风险平台
  report, `HelloAI x 抖音AI夜校…`). The first three are `interactive` cards — the Preview is
  the card's `title`, and the API has no flat text to give. The fourth was a recalled
  `post`, likely the titled-post exception Experiment 13 accepted (1 of 56 vs 1 of 34).
- **Cards carry the Preview as their `title`**, and some carry real content: the 风险平台
  report has `text` and `at` elements a flattener could read, while others hold only an
  image and "请升级至最新版本客户端". Order of the Later item "Update for other message
  types" stands: cards first.
- **A cached chat Max cannot read.** `ByteDance Research 技术交流群` (`oc_e4bc5f9d…`) sits
  in `chats.json`: an unambiguous title answered `ok, items: []` under time bounds (the
  masked `230002`), and `if (candidates.size == 1 && !outcome.failed()) remember(…)`
  caches a clean miss. All three Originals under that title were cards, so the harm is 0 so
  far. Caching only on `Found` would close it.
