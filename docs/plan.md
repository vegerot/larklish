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
| Compose build | Works under AGP 9 built-in Kotlin with plugin `2.2.10`; BOM `2026.08.00` needs `compileSdk 37`, `targetSdk` stays 36 | same |

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

- Fetch for every Original. Later optimization: fetch only when the Preview ends in `...`.
- Show the whole translated text in `BigTextStyle`; input capped at 1,000 chars (the
  translation API limit). An AI summary is a 2.0 idea.
- `text` messages only. Record the `msg_type` of every candidate so the soak shows how
  common `post`, cards and images are. Any other type → `skipped`.
- Groups: title → `chats/search` (exact name, or name prefix when the title ends in
  `...`). DMs: `messages/search` around the Original's time, the `is_p2p_chat` hit whose
  peer name (first line of `display_info`) is the Sender; polled up to 4 × 5 s for the
  index lag; cached per Sender (`dm:<Sender>`). A bot DM's title is `Lark`.
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
- [ ] **Romanize only person names.** `romanize()` is right for bare 2–4-character Han
  names (陈昱萌 → Chen Yumeng) and wrong for bot or group Senders that mix Latin and Han
  (`Triton数据安全` → "Triton Shu Ju An Quan", `Argos平台报警` → "Argos Ping Tai Bao
  Jing"), where pinyin carries no meaning. Lark's engine translates those well ("Triton
  Data Security"). Change: in `Relay.kt`, romanize a Sender only when it is a bare Han
  name; send every other Han-containing Sender through `englishOf`. Add a `RomanizeTest`
  case. Evidence: `docs/experiments/05-lark-api.md` "Soak".
- [ ] **App icon (Max, late stage).** Every Relay shows Larklish's icons, not Lark's: the
  **small icon** (monochrome glyph in the status bar and shade header) is mandatory for a
  notification, and the **large icon** (right side of the row) is optional — Layer 3 copies
  the Original's group avatar there. The launcher icon is the same asset family. Today it is
  the system default (`@android:drawable/sym_def_app_icon`). Design a proper adaptive
  launcher icon + monochrome small icon once the Relay is stable; 译鸟 (translation bird) is
  the theme.
- [ ] **Tests for `Preview.parse`** (TDD, Max's call): once the Layer 2/3 experiments end,
  write the Sender/message split test-first. Cases: plain `Sender: message`,
  `Sender@you:` and `Sender@all:`, a message that itself contains `: `, no `: ` at all.
- [ ] **Update for `post` and the other message types.** Layer 5 Updates `text`
  messages only. `post` (shape: locale → `title` + paragraphs of tagged elements) comes
  first; then cards, images, files. The soak's `msg_type` counts set the order.
- [ ] **Fetch only when the Preview ends in `...`.** A performance optimization for
  Layer 5: a Preview without `...` already holds the Full text.
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
  only a seed.
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
- GNU+Linux (Debian). SDK at `~/Android/Sdk` (has `platforms/android-36`,
  `build-tools/36.0.0`, `platform-tools`). No `sdkmanager` installed; none needed so far.
  `uv` in `~/.local/bin` (installed 2026-08-27 for `tools/lark-token`).

```sh
cp debug.keystore ~/.android/debug.keystore              # once per machine: same debug signer everywhere,
                                                          #   so installDebug upgrades in place (keeps data + grants)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties       # once per machine; Studio does it on macOS
tools/lark-token --write                                  # Layers 4–5: lark.appId, lark.appSecret,
                                                          #   lark.userRefreshToken from lark-cli's store
                                                          #   (needs uv; Debian: curl -LsSf https://astral.sh/uv/install.sh | sh)
./gradlew installDebug
./gradlew testDebugUnitTest                                # JVM tests
adb shell cmd notification allow_listener com.vegerot.larklish/.LarkListener
adb shell pm grant com.vegerot.larklish android.permission.POST_NOTIFICATIONS
adb logcat --pid="$(adb shell pidof com.vegerot.larklish)"
adb shell am start -n com.vegerot.larklish/.MainActivity -f 0x10008000 \
  --es debug refresh                                      # Layer 5: force a user-token refresh, log the user
adb shell run-as com.vegerot.larklish rm files/chats.json \
  && adb shell am force-stop com.vegerot.larklish         # Layer 5: reset the chat-id cache (the service keeps it in memory)
tools/events stats                                     # the Relay record (see tools/events --help)
tools/events --since 2026-08-26T17:29 --han list       #   views: list, stats, table, pull
tools/phone open 'https://applink.feishu.cn/client/chat/open?openChatId=oc_…'   # fire a deep link; also top, shade, shot, home
lark-cli im +messages-send --as bot \
  --user-id ou_4d4c9ea3307c313a134ac3ea0821935e --text "中文"   # trigger an Original
```
