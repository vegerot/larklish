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
| Layer 4 soak | 36 Relays on real traffic: 0 fallback rows; 8 Han rows graded 5 ✅ / 3 ⚠️ / 0 ❌; first Han group title readable; `romanize()` mangles bot Senders with Han (`Triton数据安全` → pinyin) | `docs/experiments/05-lark-api.md` "Soak" |
| Lark API over raw HTTP | Host `open.feishu.cn` (Feishu-brand tenant); tenant token lasts 7200 s; no burst limit at 8 concurrent calls; Latin-heavy input passes through unchanged (deterministic) | `docs/experiments/05-lark-api.md` |
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
  as fallback. A production build needs a dedicated `translation:text`-only app instead
  of the fat CLI app's secret.
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
- [ ] **2.0 idea (Max): longer notifications and/or AI-summarized notifications.** Lark cuts
  the Preview to the first clause (Experiment 01). Showing the whole message needs the
  message text, which the listener never gets: it would need the message-read API under
  the user identity (polling was rejected at the start; the scopes are granted now, see
  "Message-read scopes" above) or another source. A summary of the full message
  needs the same access plus a summarizer. Record only; not in the current layers.

## Commands

Dev machines (Max moves between them; `local.properties` is per machine):

- macOS with Android Studio. SDK at `~/Library/Android/sdk` (Studio wrote `sdk.dir`
  into `local.properties`; has `platforms/android-37.0`, `build-tools/36.0.0`,
  `platform-tools`, `cmdline-tools`). Build through the Studio MCP (`build_project`) or
  `./gradlew`; both work.  Prefer using Android Studio MCP over `./gradlew` generally,
    but often the CLI will be more convenient than Studio MCP.
- GNU+Linux (Debian). SDK at `~/Android/Sdk` (has `platforms/android-36`,
  `build-tools/36.0.0`, `platform-tools`). No `sdkmanager` installed; none needed so far.

```sh
cp debug.keystore ~/.android/debug.keystore              # once per machine: same debug signer everywhere,
                                                          #   so installDebug upgrades in place (keeps data + grants)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties       # once per machine; Studio does it on macOS
echo "lark.appId=cli_…" >> local.properties                # Layer 4: Lark app credentials
echo "lark.appSecret=…" >> local.properties               #   (from lark-cli's keychain store)
./gradlew installDebug
./gradlew testDebugUnitTest                                # JVM tests
adb shell cmd notification allow_listener com.vegerot.larklish/.LarkListener
adb shell pm grant com.vegerot.larklish android.permission.POST_NOTIFICATIONS
adb logcat --pid="$(adb shell pidof com.vegerot.larklish)"
tools/events stats                                     # the Relay record (see tools/events --help)
tools/events --since 2026-08-26T17:29 --han list       #   views: list, stats, table, pull
lark-cli im +messages-send --as bot \
  --user-id ou_4d4c9ea3307c313a134ac3ea0821935e --text "中文"   # trigger an Original
```
