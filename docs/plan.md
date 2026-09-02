# 🗺️ Larklish — Build plan

Larklish grows like an onion 🧅. Each layer is the smallest thing that answers one
question. Commit every layer, even the ones we throw away. The settled decisions are in
the table below; `CONTEXT.md` holds the words.

## Working rules

- Keep `progress.md` short, append-only, and continuation-ready.
  - The goal of `progress.md` is that any engineer can read this plan and then
    read `progress.md` and understand what is done and what is left.
  - Summarize each action (code changes, experiments, commits, findings) in
    `progress.md` as the work happens, to hand off mid-task.
  - Use one emoji per action. End every entry with a `Next:` block.
- Mutable state (commit hashes, statuses, message ids) lives in `progress.md`,
  not here. Evidence lives in `experiments/`.
- Stop at the end of each layer. Do not continue until Max asks.
- Commit at least at the end of each layer. Do not commit half-done work. Each
  commit must build and install.
- At the start of each layer, or after a context compaction, read
  `progress.md` from the bottom and check the working copy against the last
  entry (`git status`, `git diff --stat`).
- If you need clarity the docs do not give, STOP and ASK.

## Settled by the experiments so far

| Question | Answer | Evidence |
| --- | --- | --- |
| Lark package | `com.larksuite.suite` | `pm list packages` |
| Original shape | `BigTextStyle`; title = group, text = `Sender: message`; no actions | `docs/experiments/00-adb.md` |
| Grouping | Lark sets none; Android auto-groups per package | same |
| Preview length | Lark truncates at ~64 chars | same |
| Listener grant | `cmd notification allow_listener` — no Settings UI needed | `cmd notification` help |
| Lark withdraws Originals | Yes, on its own (desktop active). Relay must follow. | same |
| Lark translation API | Works via `lark-cli api --as bot` (scope `translation:text` granted); English input passes through unchanged | `docs/experiments/00-adb.md` |
| Pixel Live Translate | Not available (Tensor-only, and this is Lineage) | — |
| Interception | A sideloaded listener receives Originals; `allow_listener` binds it at once, no restricted-settings gate | `docs/experiments/01-listener.md` |
| Autogroup summary | Arrives as a second `posted` event with null title/text; Layer 3 skips `FLAG_GROUP_SUMMARY` | same |
| Withdrawal | Lark calls `cancelAll()` (`reason=9`) whenever Max reads the chat anywhere; 7–19 s after the post while Max is at a computer | `docs/experiments/04-soak.md` |
| ML Kit quality | Gist OK in 7 of 14 rows; names, identifiers and mentions damaged in almost all; 6 rows change meaning | `docs/experiments/02-quality.md` |
| Lark API quality | Better, but not perfect: one identifier changed, one Sender dropped; rate limit `99991400` hit 5 of 14 calls | same |
| Overnight soak | 10 h, one process, 0 crashes, 9/9 Originals relayed, 0 stale Relays; ML Kit: 4 English pass-through ✅, 4 Han messages ⚠️, 1 Han title ❌ | `docs/experiments/04-soak.md` |
| Real-message benchmark | 30 real Previews: ML Kit 6 ✅ / 18 ⚠️ / 6 ❌, Lark 27 ✅ / 3 ⚠️ / 0 ❌; ML Kit translates Chinese Sender names literally | `docs/experiments/02-quality.md` Benchmark 3 |
| Message-read scopes | Granted on the CLI app under Max's user identity (`search:message`, `im:message.*_msg:get_as_user`, …). Handoff's "effectively blocked" is false for this app | same, "Scope finding" |
| Romanization | Android ICU `Han-Latin/Names` works on the phone; `romanize()` is tested (`RomanizeTest`, instrumented) | `Romanize.kt` |
| Lark Preview budget | 45 characters of message text, cut back to a space/punctuation, `...` appended; no boundary before 45 → empty Preview; newline ends it | `docs/experiments/06-full-message.md` |
| DM chat lookup | `chats/search` never returns p2p chats; `POST im/v1/messages/search` (by `from_ids` / `chat_type=p2p`) gives `meta_data.chat_id`, `position` — 15 s index latency vs 3 s for the list endpoint; cache per Sender | `docs/experiments/06-full-message.md` "E2 continued" |
| `position` AppLink | The API mints `client/chat/open?…&position=N` and `client/thread/open?…`; the phone client ignores `position` and does not handle `client/thread/open` | same |
| Deep links | Lark's tap target lives in unreadable intent extras; copying the `PendingIntent` inherits it. Documented AppLinks (`client/chat/open?openChatId=`, `openId=`, `client/bot/open?appId=`) work when we fire them; no AppLink opens a message or thread | `docs/experiments/07-deep-links.md` |
| Layer 4 soak | 36 Relays on real traffic: 0 fallback rows; 8 Han rows graded 5 ✅ / 3 ⚠️ / 0 ❌; first Han group title readable; `romanize()` mangles bot Senders with Han (`Triton数据安全` → pinyin) | `docs/experiments/05-lark-api.md` "Soak" |
| Lark API over raw HTTP | Host `open.feishu.cn` (Feishu-brand tenant); tenant token lasts 7200 s; no burst limit at 8 concurrent calls; Latin-heavy input passes through unchanged (deterministic) | `docs/experiments/05-lark-api.md` |
| lark-cli token store | AES-256-GCM blobs (12-byte IV \| ciphertext \| tag) under one master key: macOS keychain `lark-cli` / `master.key` (`go-keyring-base64:` + base64 of the base64 key), Linux raw `~/.local/share/lark-cli/master.key`. User token blob `<appId>_<openId>.enc` = JSON `StoredUAToken`; app secret `appsecret_<appId>.enc`. A second `auth login` (Debian) did not invalidate the Mac's token | `tools/lark-token`; lark-cli `internal/keychain` |
| User-token refresh on the phone | `POST authen/v2/oauth/token` (JSON, `grant_type=refresh_token`) from the seed works; every refresh rotates the refresh token; the pair persists in `filesDir/user-token.json` | `UserToken.kt`, debug hook `--es debug refresh` |
| `messages/search` for DMs | Without `query` the `chat_type` filter is ignored (mixed hits), so filter on `meta_data.is_p2p_chat`; `display_info` = `<peer name>\n<Sender>: <text>` (HTML-escaped). Bot DM Originals have title `Lark`; the Sender is the bot | `MessageFetcher.dmChatId`; commit 6 test |
| Layer 5 on the phone | Group: Relay `Sender: ...` → Update with the full English 4 s later; a newer Original on the key cancels the older Update; bot DM: first Update 7 s (search), cached 3 s | `docs/progress.md` 2026-08-27 |
| A day of real traffic | 73 Relays: 53 % of Previews truncated, 0 empty; 23 % DM-shaped (title `Lark`); Previews can span two lines; same-key re-posts < 10 s: 4 %; withdrawal median 11.5 min, p90 90 min | `docs/experiments/08-soak.md` |
| `post` messages | Everyday type (4 of 12 newest in the busiest group; a plain sentence from the mobile editor is a `post`). `body.content` = `{title, content: [[{tag, text|image_key|user_name…}]]}` | same |
| Fallback on Latin-heavy text | Both real ML Kit fallback rows made the English worse (`customFetch` → `CustomEtch`, shouting). `tools/events` had hidden them (`~` after `Sender: `) | same |
| User refresh tokens | Single-use: the phone's first refresh killed the Mac's lark-cli chain 35 min later (`token_missing`). A separate `auth login` (Debian) is a separate chain and survives | same, "Layer 5's first hours" |
| Compose build | Works under AGP 9 built-in Kotlin with plugin `2.2.10`; BOM `2026.08.00` needs `compileSdk 37`, `targetSdk` stays 36 | same |
| Message → Original delay | p50 5.1 s, p90 10.2 s, p95 24.3 s, **max 43.9 s** over 116 matched Relays. No message ever arrives *after* its Original, so only the backward edge matters | `docs/experiments/11-layer6-fixes.md` |
| Topic chats | `San Jose 1199…`, `Seller Center - FE Oncall` are `chat_mode: topic`. A reply in a thread puts the **thread's root text** in the Original's title, so `chats/search` finds nothing — but `im/v1/messages` on the chat returns the thread replies | `docs/experiments/09-soak.md` |
| Ambiguous titles | A truncated title can match many chats (`【bytedcli MR 处理】` → 6). Caching the first hit poisons the entry for good | same |
| Preview vs. Full text | The Preview is **not** a faithful prefix: `emotion` tags read `[Delighted]` not `[emotion]`, `@All` reads `@all`, and a trailing bold run can be dropped. A case-insensitive 12-character prefix match inside the window is unambiguous (0 of 114 stems matched two of 35 chats) | `docs/experiments/11-layer6-fixes.md` |
| Messages get edited | The API returns the edited text and Lark re-posts the Original. A stem match against an edited message can never succeed | same |
| HTML in `text` bodies | 11 of 198 `text` messages carry tags (`<p>…`). Lark's Preview strips them | same |
| Translate failures | Transport-level and clustered: 0 of 60 sequential calls failed, yet one failed inside a 20-call batch and its near-twin succeeded. 16 of 18 Han fragments behind the ML Kit fallbacks translate fine — one retry keeps Lark's quality for 10 of 11 Relays | same |
| Person DMs | Also carry the title `Lark`. `title == Sender` never happened in 210 Relays | `docs/experiments/09-soak.md` |
| `chats/search` is not deterministic | The same query can omit a chat that exists: 4 of 11 lookups for `Zhifu Liu, Max Coplan, Yonghao Cao` missed it, at `page_size` 20, 50 **and** 100 — the page is full every time, so this is not a paging cutoff. A title therefore resolves or not, run to run, and some `no-chat` rows are the API, not the rule. The Layer 6 LRU fallback catches these too | `docs/progress.md` 2026-08-28 |
| Delayed pushes | Lark delivers a backlog minutes late: 3 m 59 s observed, three Originals inside two seconds, two of them `no-message`. `notification.when` is the *delivery* time, not the message time, so nothing on the notification reveals it. The match window reaches back 5 min for a full-length stem and 60 s for a short or empty one | `docs/progress.md` 2026-08-28 |
| Mentions are localized | A mention shows whichever of an account's names the reader prefers, so the Preview and the Full text disagree on the opening and on nothing else: `@Oncall Assistant` / `@Oncall 助手`, `@all` / `@所有人`, `@Yan Fan` / `@樊艳`. `@所有人` has no `mentions` entry, so no amount of structure finds it. Matching the **close** of the stem instead, when both sides open with `@`, recovers 11 of 257 Originals with 0 false positives | `docs/experiments/12-soak-and-mentions.md` |
| A found chat is not a readable chat | `chats/search` returns chats Max can **find** but not **read**: `ByteDance Research` names two, and `im/v1/messages` on one answers `400 230002 Bot/User can NOT be out of the chat`. Time bounds usually mask it (`ok, items: []`), which is why it is rare. A per-chat failure is now that chat's miss, never the lookup's, and such a chat is never cached | `docs/progress.md` 2026-08-31 |
| Bot DMs are worth nothing | Every `no-chat` of the Layer 6 soak is a bot DM (`Approval`, `ByteCanteen`, `IT Security Center`, `Mail Assistant`). `messages/search` does find their chats; the peer name is localized so `dmChatId` rejects them — and the message is a `CARD` in every case. Repairing the name would only turn `no-chat` into `type:interactive`. 🚫 Do not build a bot-DM resolver | same, §1 |
| The translate limit is the tenant's | 20 QPS **per tenant, shared by every app in it** (doc: 同租户下的应用没有限流，共享本租户的 20QPS 限流). Give-ups on `99991400` fall only in Beijing working hours: 14 of 81 Han texts on a weekday, 0 overnight. A dedicated app cannot help; only what we do after the error can | `docs/experiments/14-weekday-soak.md` §3 |
| `@_all` in `text` bodies | A `text` body spells @all as the literal `@_all` and the `mentions` array never names it; the Preview reads `@all`. `resolveMentions` maps it | same §1 |
| Cards preview their `title` | For an `interactive` message Lark's Preview is the card's `title`, never marked `...`; some cards carry real `text`/`at` elements, others only an image. First target of "Update for the other message types" | same, "Also seen" |
| Weekday grade | 31 of 34 readable cut Previews Updated (91 %); 11 of the 31 through the localized-mention close rule, all correct | same §1–2 |

## Layers

### Layer 0 — adb only ✅ done

See `docs/experiments/00-adb.md`.

### Layer 1 — Listener ✅ done

See `docs/experiments/01-listener.md`. Gradle project (AGP 9.3.2, built-in Kotlin,
`minSdk 36`), `LarkListener` logs every Lark Original. Interception works.

### Layer 2 — Translator screen ✅ done

See `docs/experiments/02-quality.md`. Compose `MainActivity` (text field → ML Kit → English,
plus an `am start --es text` debug hook), `Translator` interface, `MlKitTranslator`.
The quality table has 14 rows (5 real, 9 made up). Max's judgment is pending.

### Layer 3 — Relay ✅ done

See `docs/experiments/03-relay.md`. Verified for a bot DM and a synthesized group chat:
the Relay carries the translated title, romanized-safe text, group avatar and tap intent;
the withdrawal mirror handles `REASON_CLICK` and `REASON_APP_CANCEL_ALL`. Recall does not
withdraw — Lark updates the Original in place. `POST_NOTIFICATIONS` is required.

Cancel option (Max): debug builds keep the Original next to the Relay for comparison;
release builds cancel it (`if (!BuildConfig.DEBUG) cancelNotification(...)`).

### Layer 4 — LarkApiTranslator ✅ done

See `docs/experiments/05-lark-api.md`. `LarkApiTranslator` calls Lark's engine over
`HttpURLConnection` with the CLI app's credentials from `local.properties` (→
`BuildConfig`, embedded in the APK; Max OK'd this for now). `FallbackTranslator` uses ML Kit
when Lark fails (offline, error code, or the silent pass-through on Latin-heavy text) and
prefixes the fallback output with `~` so the Relay and `events.jsonl` show the engine.
No pacing: Experiment 05 saw no rate limit. Verified on the phone 2026-08-26: (1) the
debug hook shows Lark-quality output; (2) a bot DM relays through the listener; (3) the
Latin-heavy pass-through and radios-off both produce `~` ML Kit output.

### Layer 5 — Full text in the Relay ✅ built, soaking

Lark's Preview is at most 45 characters and often empty (Experiment 06). The Original
cannot carry more (E1) and no AppLink opens a message (Experiment 07), so the Full text
comes from the Open API under Max's user identity, **one lookup per Original**: title →
chat id (`chats/search`, cached in `filesDir/chats.json`) → newest messages (`messages`
list, `ByCreateTimeDesc`) → pick by time window and Preview stem → translate → **Update**
the Relay. Two-phase Relay: post from the Preview at once, Update the same key when the
fetch returns (~1–3 s). Decisions (grilling, 2026-08-27):

- Fetch for every Original. Later optimization: fetch only when the Preview ends in `...`
  (53 % of real Previews do — Experiment 08).
- Show the whole translated text in `BigTextStyle`; input capped at 1,000 chars (the
  translation API limit). An AI summary is a 2.0 idea.
- `text` and `post` messages (`post` flattened: title, one line per paragraph, `[image]`,
  `@name` — commit 7, after Experiment 08 showed `post` is the everyday type). Any other
  type → `skipped type:<msg_type>`; `no-message` = nothing in the window, `no-match` =
  a text there did not start with the stem.
- Groups: title → `chats/search` (exact name, or name prefix when the title ends in
  `...`). DMs: `messages/search` around the Original's time, the `is_p2p_chat` hit whose
  peer name (first line of `display_info`) is the Sender; polled up to 4 × 5 s for the
  index lag; cached per Sender (`dm:<Sender>`). A DM is recognised by its title: `Lark`
  (bots) or the Sender's own name (people — shape still unverified, none in the record).
  The group and DM caches never cross (a bot that DMs Max also posts in groups).
- Lark reuses one Original key per chat, so a newer Original on the same key cancels
  the in-flight fetch. Otherwise the older Full text could overwrite the newer Relay.
- Mismatch rule: newest message inside `[when − 15 s, when + 5 s]` whose text starts
  with the Preview stem. Count mismatches during the soak before tightening it.
- Debug builds post every error as an "Errors" notification.
- Token: `tools/lark-token` copies Max's refresh token from lark-cli's store into
  `local.properties` → `BuildConfig`; the app persists rotations in
  `filesDir/user-token.json` (2 h access, 7-day sliding refresh, Experiment 06 E4).

Files: `LarkHttp.kt` (HTTP shared with `LarkApiTranslator`), `UserToken.kt`,
`MessageFetcher.kt` (pure `pickMessage` + `resolveMentions`, tested), the Update path in
`LarkListener`, `Recorder` events `updated` / `skipped`, `tools/events` `U` / `S` rows.
Commits: docs → `LarkHttp` → `UserToken` + `tools/lark-token` → `MessageFetcher` → Update →
DMs. Then a soak ≥ 1 day graded by `tools/events stats` (updated vs skipped by reason,
mismatches, share of `post`). The plan may change mid-flight; record each change here
and in `progress.md` with the reason.

Far future: a backend holds the secrets and tokens and maybe runs the fetch. Keep the
fetcher behind one interface so it can move.

### Layer 6 — Make the Update land ✅ built, soaking

Layer 5's first day Updated **34 of 91** real Relays (Experiment 09). The fetch works; the
chat resolver and the match rule lose the rest. Every fix below is measured on an offline
replay of 210 Relays (`tools/replay/`, Experiments 10–11), so the order is by measured
value, not by guess. Baseline 101 → **119 of 210** stacked.

1. **Window `BEFORE_MS` 15 s → 60 s** (+6, no extra calls). The delay from message to
   Original runs to 43.9 s; 15 s throws away the top 7 %. It saturates at 60 s. Leave
   `AFTER_MS` at 5 s — widening it to 60 s recovers nothing. *Later the same day: a
   full-length stem reaches back 5 min, for delayed pushes — see the entry below.*
2. **Case-insensitive prefix match** (+8 stacked): compare the first 12 whitespace-free
   characters, case-folded, instead of the whole stem. 0 false positives across 35 chats.
3. **`emotion` → `[emoji_type]`** in `postText` (+0 matches, but the Relay should read
   `[Delighted]` like Lark). Strip HTML tags from `text` bodies in the same commit.
4. **Retry Lark once before ML Kit, and record the reason** in the `relayed` event. All 11
   fallbacks in the record were one 2.5 h block of transport errors, and ML Kit made the
   English worse every time.
5. **LRU 2 on `no-chat`** (+3, ~180 extra GETs): try the two most recently used chats and
   take the stem match. This is the only way to reach a thread reply. Scanning every
   cached chat finds the same 3 for 4× the calls; a thread-root cache alone finds 0.
6. **Disambiguate prefix titles** (+1, optional): when a truncated title matches several
   chats, try each and cache only the one a message confirms.

Rejected: **retrying the message list** — worth 0. The forward edge of the window is
already generous, so a retry cannot find anything the first call missed.

Built 2026-08-28, one commit per fix, each verified on the phone. Two changes the replay
did not predict: the messages list now carries `start_time`/`end_time` so a burst cannot
push the message off the page, and `unwrapHtml` strips the markup Lark adds to an **edited**
message. The chat cache now holds only what a message confirmed, so the one poisoned entry
was cleared once (`rm files/chats.json`, force-stop) and cannot come back.

Then soak again and re-grade with `tools/larklish-helper events stats` — now including `fallback` rows,
which say whether the Translator hit an outage or an input Lark will not translate.

### Later (not experiments)

- [ ] Onboarding: deep-link to `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`; note the
  Android 13+ "Allow restricted settings" step for sideloaded apps.
- [x] Debug UI: list of recent Original → Relay pairs. ✅ Done 2026-08-25: `Recorder`
  appends every Relay pair and removal (with reason) to `filesDir/events.jsonl`;
  `MainActivity` lists them newest-first with a Refresh button. Shell read:
  `adb exec-out run-as com.vegerot.larklish cat files/events.jsonl`.
- [x] ~~`LarkApiTranslator` behind a setting.~~ → Layer 4. Decision history: Max
  (2026-08-25) kept ML Kit for faster iteration; Experiments 04–05 showed Lark's engine
  fixes every ML Kit failure seen in the soak, so Layer 4 makes Lark primary with ML Kit
  as fallback. The fat CLI app's secret in the APK is a Shortcut (see below).
- [x] **Romanize only person names.** ✅ Done 2026-08-31: `Translator.senderOf` romanizes a
  Sender only when `isHanName()` — bare Han, 2–4 characters — and sends every other
  Han-containing Sender to the engine. Before, a bot or group Sender became meaningless
  pinyin (`国际电商风险运营平台` → "Guo Ji Dian Shang Feng Xian Yun Ying Ping Tai",
  `Triton数据安全` → "Triton Shu Ju An Quan"); 10 of 257 Relays carried one. `buildRelay`
  now takes the Sender already in English, like the title and the message.
  Evidence: `docs/experiments/05-lark-api.md` "Soak".
- [ ] **App icon: finish the 译鸟 mark.** 2026-08-27: an AI-generated flat bird (navy
  body, orange belly, two chirp arcs, teal disc; `docs/logo/larklish-v1.png`) is in use as
  the adaptive launcher icon and, as an alpha silhouette, the notification small icon.
  Todo before it is final: (1) two **bold chirp arcs** about the size of the head — the
  current ones vanish at 24 dp; (2) a **rounded, fuller wing tip** (the taper fades at small
  sizes; rounded ends also echo Lark's strokes); (3) scale the bird ~15 % down so it fits the
  adaptive-icon **66 % safe zone** (the wing tip is at ~75 % of the radius); (4) optional: a
  lighter aqua upper wing behind the navy one as a Lark-family cue. Produce it as an SVG →
  `VectorDrawable`, replacing the raster. The **large icon** (row avatar) stays the
  Original's group avatar (Layer 3).
- [ ] **Tests for `Preview.parse`** (TDD, Max's call): once the Layer 2/3 experiments end,
  write the Sender/message split test-first. Cases: plain `Sender: message`,
  `Sender@you:` and `Sender@all:`, a message that itself contains `: `, no `: ` at all.
- [ ] **Update for the other message types.** After `text` and `post` (Layer 5),
  cards (`interactive`), images, stickers, files. The soak's `msg_type` counts set the order.
- [ ] **Translate only the Han runs when Lark passes text through.** Both real fallback
  rows of the soak were Latin-heavy sentences with a few Han words; ML Kit damaged the
  English (`customFetch` → `CustomEtch`). Instead of the whole sentence, send each Han run
  (`权限申请`) to the Translator and splice the results back. Evidence:
  `docs/experiments/08-soak.md`.
- [x] **Fetch only when the Preview ends in `...`.** ✅ Done 2026-08-31: `Preview.truncated`
  gates the Update, and a Relay that skips it records `skipped not-truncated` so a soak can
  still price the exception. 54 % of Relays never needed the fetch, and 33 of the 34 Updates
  measured from an uncut Preview returned a Full text **identical** to it. The exception is
  a `post` with a title: Lark previews only the title, with no `...`, so those are lost
  (1 of 34, a probe). Evidence: `docs/experiments/13-fetch-guard.md`.
- [ ] **Image in the Relay (far future, Max).** Lark's Original for an image says only
  `[image]`. Android can show the picture (`Notification.BigPictureStyle().bigPicture(bitmap)`).
  The Layer 5 fetch already sees these messages (`msg_type` `image`, today a
  `skipped type:image` row): `body.content` is `{"image_key": "img_…"}` and
  `GET /open-apis/im/v1/messages/{message_id}/resources/{image_key}?type=image` downloads
  it under the user identity (scope `im:resource`). Same for stickers and, later, files.
- [ ] **AI summary of the Full text (2.0 idea, Max).** For long messages, show a summary
  instead of the whole translation.
- [ ] **Tighten the Layer 5 match rule** if the soak shows mismatches (wrong message
  picked for an Original): compare more of the text, or use `messages/search`.

## Shortcuts (fix before Larklish is a public app)

Taken for velocity while Larklish serves Max and maybe Max's team. Each one is a known
gap, not an oversight. The future backend removes most of them.

- The fat CLI app's secret is in the APK (`local.properties` → `BuildConfig`). A public
  build needs a dedicated app with only `translation:text` + `im:message` scopes.
- Max's user refresh token is bootstrapped into the APK the same way
  (`tools/lark-token`). The app persists rotations in `filesDir`, so the APK copy is
  only a seed — and a consumed one: refresh tokens are single-use, so the machine that
  ran `tools/lark-token --write` needs `lark-cli auth login` again (Experiment 08).
- No sign-in screen. A public build needs the device-code flow lark-cli uses. It is also
  the fix the day the seed token dies (7 days without a refresh).
- Everything runs on-device. The backend takes the secrets, the tokens, and maybe the
  fetch.

## Commands

Dev machines (Max moves between them; `local.properties` is per machine):

- macOS with Android Studio. SDK at `~/Library/Android/sdk` (Studio wrote `sdk.dir`
  into `local.properties`; has `platforms/android-37.0`, `build-tools/36.0.0`,
  `platform-tools`, `cmdline-tools`). Build through the Studio MCP (`build_project`) or
  `./gradlew`; both work.  Prefer using Android Studio MCP over `./gradlew` generally,
    but often the CLI will be more convenient than Studio MCP.
- GNU+Linux (Debian), the desktop. SDK at `~/Android/Sdk` (has `platforms/android-36`,
  `build-tools/36.0.0`, `platform-tools`). No `sdkmanager` installed; none needed so far.
  `uv` in `~/.local/bin` (installed 2026-08-27 for `tools/lark-token`).
- GNU+Linux, the ByteDance dev box (`devbox`, home `/data00/home/max.coplan`, address
  `10.251.236.182`). A different machine from the desktop. Toolchain installed
  2026-08-28: Temurin JDK 21 in `~/.jdks/jdk-21.0.12.1+1` (the system JDK 11 cannot run
  Gradle 9.7.1) and the SDK at `~/Android/Sdk` (`cmdline-tools`, `platform-tools` 37.0.1,
  `platforms;android-37.0`, `build-tools;36.0.0`). The platform package is
  `android-37.0`, not `android-37`. `uv` and `lark-cli` in `~/.local/bin`. No phone on
  USB: `adb` reaches the Pixel 4a through a reverse tunnel from the Mac (below).

```sh
export JAVA_HOME=~/.jdks/jdk-21.0.12.1+1                   # dev box only: the system JDK 11 is too old
export ANDROID_HOME=~/Android/Sdk                         # devbox only: for Gradle 9.7.1
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  platform-tools "platforms;android-37.0" "build-tools;36.0.0"   # devbox only: once per machine
cp debug.keystore ~/.android/debug.keystore              # once per machine: same debug signer everywhere,
                                                          #   so installDebug upgrades in place (keeps data + grants)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties       # once per machine; Studio does it on macOS
tools/larklish-helper token --write                              # Layers 4–5: lark.appId, lark.appSecret,
                                                          #   lark.userRefreshToken from lark-cli's store
                                                          #   (needs uv; Debian: curl -LsSf https://astral.sh/uv/install.sh | sh)
lark-cli auth login                                       #   …then log lark-cli in again: the phone consumes
                                                          #   that refresh token (single-use, Experiment 08)
./gradlew installDebug
./gradlew testDebugUnitTest                                # JVM tests
ssh -N -o ExitOnForwardFailure=yes devbox                 # when working remotely on devbox: on the Mac: carry its adb server to the dev box
                                                          #   (~/.ssh/config: Host devbox, HostName 10.251.236.182,
                                                          #   RemoteForward 5037 localhost:5037; the DNS name
                                                          #   `devbox` is a load balancer, not the machine)
export ADB_SERVER_SOCKET=tcp:localhost:5037                # on the dev box: adb talks to the Mac's server
export PATH="$HOME/Android/Sdk/platform-tools:$PATH"       # devbox: client and server versions must match
adb shell cmd notification allow_listener com.vegerot.larklish/.LarkListener
adb shell pm grant com.vegerot.larklish android.permission.POST_NOTIFICATIONS
adb logcat --pid="$(adb shell pidof com.vegerot.larklish)"
tools/larklish-helper probe "中文消息"                          # post to the test group, then show the Relay
tools/larklish-helper probe --install --thread "中文消息"       #   …after installDebug; --thread adds a reply in a thread
tools/larklish-helper probe --debug refresh                   #   MainActivity hooks: user, refresh, fetch, thread
tools/larklish-helper chats                                   # what the chat-id cache holds
tools/larklish-helper chats --clear                           #   reset it (deletes the file *and* force-stops)
tools/larklish-helper events stats                            # the Relay record; views: list, stats, grade, pull
tools/larklish-helper events --since 2026-08-29T00:30 --han list
tools/larklish-helper events --since 2026-09-01T03:50 grade   #   the soak grade: cut Previews vs their Update outcome
tools/larklish-helper msgs 'US Global E-Commerce' --around 2026-09-02T14:06   # what a chat said then (id or title)
tools/larklish-helper chats search 'ByteDance Research' --repeat 3            # chats/search hits, and how often they appear
tools/larklish-helper translate '先别发布' --repeat 30          # one Lark translate call, with its code (rate-limit probe)
tools/larklish-helper phone shade shot.png              # adb helpers: top, home, shot, shade
tools/larklish-helper replay fetch                            # pull replay-corpus/ (gitignored: real messages)
./gradlew testDebugUnitTest --tests '*ReplayTest*' -i  #   …score the shipped rules over it
```
