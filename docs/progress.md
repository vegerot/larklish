# Progress

Append-only, newest last. `plan.md` holds the plan; `experiments/` holds the
evidence behind each decision. `../CONTEXT.md` holds the words.

## Layer 0 — adb only

### 2026-08-25 — Architecture and name settled; handoff written

- 📐 Decision (Max): `NotificationListenerService` → ML Kit zh→en on-device →
  Relay with the Original's `contentIntent` → cancel the Original. It sits
  downstream of Lark's rules engine, so the three Tiers come for free.
- 📐 Decision (Max): name **Larklish**, package `com.vegerot.larklish`,
  Chinese name 译鸟. Lyrebird and Mockingbird rejected (homophone / "mock").
- ✂️ Rejected (Max, after a lark-cli research agent): polling under the user
  identity, a bot with event subscriptions, third-party cloud translation,
  Tasker, D-Bus, browser extension. Reasons in `../larklish-handoff.md`.
- 📄 `../larklish-handoff.md` (settled decisions, open questions, cloud
  fallbacks) and `../larklish-conversation.md` (full planning transcript).

Next:

- Grill the plan (`/grill-with-docs`). First version = basic experiments:
  can we intercept at all, and how do we translate.

### 2026-08-25 — Grilling round 1: phone facts read, no code written

- 🔍 Dev machine (Mac at the time): `adb` present, Java 23, no Android SDK,
  no Gradle, no Android Studio.
- 🔍 Phone: Pixel 4a `sunfish`, LineageOS 23.2 (Android 16, SDK 36), Play
  Services present, no device owner (no MDM). Four notification listeners
  already enabled, so the ROM allows them.
- 🔍 Lark package on the phone: `com.larksuite.suite` (international Lark).
- 📏 `adb shell dumpsys notification --noredact`: every Lark Original is
  `BigTextStyle`, `title` = group name (or `Lark` for bots and calendar),
  `text` = `bigText` = `Sender: message`, mentions look like `Sender@you:`
  and `Sender@all:`. No actions, no `RemoteInput`, one `contentIntent`,
  channel `normal_v2`, Android autogroups. Long texts end in `...` at ~64
  chars. Saved to `experiments/00-dumpsys-notification.txt`.
- 🔍 Sub-agent 1 (facts): `com.google.mlkit:translate:17.0.3`, minSdk 23,
  ~30 MB zh model; `--noredact` prints full strings (AOSP source), so the
  `...` cut is Lark's; Android 13+ "restricted settings" can block a
  sideloaded listener grant [CONFIRM on this ROM].
- 📐 Decision: skip the Pixel Live Translate check. It is Tensor-only, and
  this phone runs Lineage.
- 📐 Decisions (Max, round 1): CLI-only toolchain; logcat as the only UI plus
  the mandatory grant button; one project, incremental commits, throwaway
  layers still committed; save the ML Kit vs Lark side-by-side.
- 📐 Glossary accepted (Max): Original, Relay, Preview, Sender, Tier.

Next:

- Round 2 questions. Then Layer 0: adb-only experiments, no app.

### 2026-08-25 — Round 2 answered; CLAUDE.md, CONTEXT.md, references written

- 🔍 Root works (`su -c id` → `u:r:magisk:s0`). `cmd notification` has
  `post`, `allow_listener`, `disallow_listener`, no `cancel`. Lark's
  databases are SQLCipher-encrypted (`*_encrypted_*.db`), so root does not
  help read cached translations.
- 🔍 `lark-cli` on the Mac: app `cli_aa949bbb72e39cde`, brand `feishu`, bot
  ready. Sub-agent 2: no translate command; raw form is `lark-cli api POST
  /open-apis/translation/v1/text/translate --data '{…}'`; scope
  `translation:text`; bot scopes are set in the developer console, not with
  `auth login`.
- 🔍 Sub-agent 3 (Open Platform docs): translate body `{source_language,
  target_language, text, glossary?}` → `data.text`; sibling `/text/detect` →
  `data.language`; tenant token only; 1,000 chars per call; 20 QPS per
  tenant; not on the free tier; `glossary` takes 128 pairs. No API reads
  per-chat mute or Tier state. Only `text` and `post` messages carry flat
  text; Lark renders the other previews itself.
- 📐 Decisions (Max, round 2): layer order = listener first, translator
  second, Relay third; one app id for everything; Compose with modern
  Kotlin; for the quality table try the Lark API first, then bot messages
  to self; commit evidence unredacted ("don't be paranoid"); pass-through
  of non-Chinese text only if it costs no extra code.
- 📄 `../CLAUDE.md` (project facts, onion rules, commands, STE, references
  incl. `https://open.larkoffice.com/llms.txt`), `../CONTEXT.md` (glossary
  with `_Avoid_` lists). Memories `lark-references`, `larklish-test-phone`,
  `lark-translation-api` written on the Mac.

Next:

- Run Layer 0: logcat during a real post, `cmd notification post` a
  Relay-shaped notification, try the translate API.

### 2026-08-25 — Experiment 00 done: Lark withdraws Originals; scope missing

- 📏 `cmd notification post -S bigtext` works. The post joined the shell
  package's own autogroup, not Lark's. So a Relay from our package sits in
  its own group.
- 🐚 Gotcha: `adb shell cmd notification post -t "Seller Center Oncall" …`
  loses the quotes on the phone side and re-splits on spaces (tag became
  `Center`). Wrap the whole remote command in one quoted string.
- 🐚 Gotcha: a background `adb logcat` died on an unquoted zsh glob. Quote
  patterns.
- 📏 `lark-cli im +messages-send --as bot --user-id ou_4d4c…` created a Lark
  Original within ~5 s (`message_id om_x100b67d77a74aca0d47fc271419a031`).
- 📏 Lark canceled that Original itself ~5 s later
  (`NotifAttentionHelper: No vibration for canceled notification …948177842`).
  The message was unread (`read_users` empty). Lark desktop was active on
  the Mac. 🕵️ Leading hypothesis: desktop active → phone alert withdrawn.
- 📐 Design consequence: when an Original goes away, the Relay must go away
  too (`onNotificationRemoved`). Added to `plan.md` Layers 1 and 3.
- ⚠️ Translate API answered `app_scope_not_applied` (99991672), missing
  `translation:text`, with a console URL to apply.
- ⏭️ STOP and ASK — Max, add `translation:text` to app
  `cli_aa949bbb72e39cde` in the developer console and release a version.
- 📄 `experiments/00-adb.md`, `experiments/00-dumpsys-notification.txt`
  (filtered to the 6 Lark records after a first commit included the full
  dump), `plan.md`. Branch renamed `master` → `main`.
- 📦 Committed `10b9c9a` "Plan Larklish and record Experiment 00 (adb only)".

Next:

- Retry the translate API after Max adds the scope.
- Q3: install the CLI toolchain and start Layer 1.

### 2026-08-25 — Lark translation API works; detect and pass-through confirmed

- ✅ Scope added by Max. Translate of `哈喽@Jiarui Chen @Yufan Chen ， 我发现
  oec.seller.assistant_api 有个问题` → `Hello @Jiarui Chen @Yufan Chen, I
  found a problem with oec.seller.assistant_api`. Mentions and identifiers
  survive [HARD]. This is the quality bar for ML Kit in Layer 2.
- ✅ `/text/detect` → `zh` and `en` correct. Translate with
  `source_language=zh` on English input returns it unchanged, so the cloud
  path needs no language-ID code.
- 📄 `experiments/00-adb.md` and `plan.md` updated (Layer 2 uses the API for
  the Lark column).
- 📦 Committed `1d849fd` "Record working Lark translation API call".
- 📦 Committed `8f107d0` "add AGENTS.md" (a copy of the first `CLAUDE.md`,
  without the References section) and `f7d6d00` "add convo"
  (`../convo.md`, the session export).

Next:

- Q3 still open: install the toolchain and start Layer 1, the listener.

## Layer 1 — Listener

### 2026-08-25 — Moved to the Linux dev machine; plan commands updated

- 🔍 New machine: GNU+Linux (Debian, `apt`). Phone attached (serial
  `08041JEC218600`). Java 21. SDK at `~/Android/Sdk` with
  `platforms/android-36`, `build-tools/36.0.0`, `platform-tools`. No
  `sdkmanager`, no `ANDROID_HOME`, no Gradle or kotlinc on PATH.
- 🔍 `lark-cli` at `/usr/local/bin/lark-cli`: bot identity ready, user
  identity missing. The plan uses only `--as bot`, so this is enough.
- 🐛 Translate API answered `99991400 request trigger frequency limit`
  (retryable) to two single calls. Cause unknown; not needed for Layer 1.
- 📄 `plan.md` Commands rewritten for Linux (`local.properties` →
  `~/Android/Sdk`, no `brew`); Layer 2 note about the rate limit; real
  `lark-cli` trigger command with Max's `open_id`.
- 📄 Memory rewritten on this machine: `lark-references`,
  `larklish-test-phone` (now includes the Linux state), `lark-translation-api`.
- 📦 Committed `9405ad4` "Update plan commands for the Linux dev machine".

Next:

- Layer 1 (Max: "Go!").

### 2026-08-25 — Layer 1 done: the listener receives Lark Originals

- 📐 Toolchain (Claude's call): AGP 9.3.2 (latest stable, Kotlin built in,
  no separate Kotlin plugin), Gradle 9.5.1 (already in
  `~/.gradle/wrapper/dists`, so no download). No Compose until Layer 2
  uses it.
- 🧱 `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`
  (`minSdk = targetSdk = compileSdk = 36`), `AndroidManifest.xml` with the
  listener service (`BIND_NOTIFICATION_LISTENER_SERVICE`, `exported=false`,
  no Activity), `LarkListener.kt` that logs `posted` / `removed` for
  `com.larksuite.suite`. Wrapper generated with the cached Gradle binary.
- ✅ `./gradlew installDebug`: BUILD SUCCESSFUL in 17 s, 35 tasks, installed
  on the Pixel 4a. First build of this project, so no baseline.
- 🔑 Recipe: `adb shell cmd notification allow_listener
  com.vegerot.larklish/.LarkListener` bound the service at once. `dumpsys
  notification` lists it as a bound listener; logcat shows `connected; active
  Lark notifications: 3`. No restricted-settings gate on this ROM [HARD].
  `stopped=false` after `adb install`, so no Activity launch was needed.
- 📏 Trigger: bot DM `om_x100b67d1139a00a0d472745480de0a8`. The Original
  arrived in `onNotificationPosted` ~25 s after the send:
  `title=[Lark] text=[Max Coplan's Feishu CLI: 测试一：这是 Larklish
  第一层实验的中文消息...] textLen=54 bigTextSame=true contentIntent=true
  flags=0x10 group=null channel=normal_v2`.
- 📏 Lark truncates before it posts. The listener gets the same `...` string
  as `dumpsys`. The Relay can never show more than this.
- 📏 The autogroup summary arrives as a second `posted` event: `title=null
  text=null flags=0x20710` (`GROUP_SUMMARY|AUTOGROUP_SUMMARY`). Layer 3 must
  skip `FLAG_GROUP_SUMMARY`.
- 📏 No withdrawal in several minutes with Lark desktop off. This supports
  the desktop-active hypothesis. The removal reason is still unknown.
- 📄 `experiments/01-listener.md`; `plan.md` Layer 1 marked done, three
  settled-facts rows added.
- 📦 Committed `0009aed` "Layer 1: listener logs Lark Originals".

Open:

- Which `reason` code does Lark use when it withdraws an Original? Layer 3
  logs it when it happens.

Next:

- ▶️ Layer 2, awaiting Max's go: Compose translator screen, ML Kit zh→en,
  quality table `experiments/02-quality.md` (~10 Previews, ML Kit vs the
  Lark API, calls spaced out because of the rate limit).

### 2026-08-25 — Studio build clean; SDK bump reverted; JVM file absorbed

- 🔍 Claude Code now runs inside Android Studio on the Linux machine.
  `build_project` from the IDE: success, 0 problems. Inspections:
  `LarkListener.kt` and `build.gradle.kts` clean; `AndroidManifest.xml`
  warns "Should explicitly set `android:icon`"; `settings.gradle.kts` notes
  `dependencyResolutionManagement.repositories` is `@Incubating` (Gradle
  API, not actionable).
- ⏸️ Icon warning deferred to Layer 3, which needs a small icon for the
  Relay anyway (Claude's call, Max informed).
- 🐛→✅ The working copy had `compileSdk`/`targetSdk` bumped 36 → 37 (Studio
  import or a hand edit; not by Claude). Reverted to 36 with `sl revert`
  (Max's call: support only the SDK 36 test phone).
- 🧱 `gradle/gradle-daemon-jvm.properties` (Studio-generated, pins the Gradle
  daemon to JVM 21) amended into the Layer 1 commit with `sl amend --to`.
- ↩️ Correction: that amend changed two hashes cited above. Layer 1 is now
  `79ac30d` (was `0009aed`); the progress-log commit is `cba943a` (was
  `f2be037`).
- 📐 Decision (Max): use Sapling (`sl`) in this repo. `.git/sl` is present;
  `sl ssl` shows the stack above `origin/main` at `f7d6d00`. Nothing pushed.
- 🐚 Gotcha: `sl absorb` only absorbs edits to files already in the stack. For
  a new file use `sl add FILE && sl amend --to REV FILE`. After the amend,
  `sl status` showed a stale `M` on the file; `sl diff` was empty.
- 🧪 Test status of `LarkListener`: manual end-to-end only (Experiment 01,
  real bot DM → logcat). Compiled by the CLI and by Studio. No unit or
  instrumented tests exist yet.

Next:

- ▶️ Layer 2, awaiting Max's go. Add unit tests when Layer 3 introduces
  logic (skip summaries, parse Sender, relay, cancel); the phone stays the
  integration test.

### 2026-08-25 — Layer 1 manual test redone: pass in 5 s

- 🔁 Reinstalled with `targetSdk=36` (`./gradlew installDebug`); the system
  rebound the listener at once (new pid, "active Lark notifications: 5").
- ✅ Bot DM `om_x100b67d2272c14a0d4662b1f6cbd6a1` sent 21:07:55, received by
  `onNotificationPosted` 21:08:00. Same shape as run 1. Log in
  `experiments/01-listener.md` ("Re-run").
- 📏 `textLen=73` vs 54 in run 1: Lark's cut is not a fixed character count.
  Both cuts sit right before a punctuation mark — clause boundary [CONFIRM].
- 📏 Lark reused notification id `948177842` for the same chat, so a second
  message in one chat arrives as an update of the same key.

Next:

- ▶️ Layer 2, awaiting Max's go.

## Layer 2 — Translator screen

### 2026-08-25 — Layer 2 done: translator works; ML Kit damages names

- 📐 Toolchain (Claude's call): Compose BOM `2026.08.00`, Compose compiler plugin
  `2.2.10` (= AGP 9.3.2's built-in Kotlin), ML Kit `translate:17.0.3`,
  `kotlinx-coroutines-play-services:1.11.0`, `activity-compose:1.13.0`.
- 🧱 `Translator` (one method), `MlKitTranslator` (download model if needed,
  then translate; both via `Task.await()`), `MainActivity` (Compose: text field,
  button, output; `--es text` extra translates at once and logs
  `Translator: in=[…] out=[…]`). Manifest: `INTERNET`, launcher Activity,
  explicit icon (`@android:drawable/sym_def_app_icon`) — the icon lint warning
  is gone. The app now shows in the drawer.
- 🐛→✅ Build failed: Compose runtime 1.12.0 needs `compileSdk 37`. Set
  `compileSdk = 37`, kept `targetSdk = 36` (they are separate; the error text
  says so).
- ↩️ Correction: Studio's earlier `compileSdk` bump to 37 was needed after
  all. Only the `targetSdk` revert stands (Max's call).
- 🐛→✅ Manifest merger failed on an XML comment containing `--es`. XML comments
  cannot hold `--`. Reworded the comment.
- ✅ Studio `build_project`: success, 0 problems. Inspections: all Kotlin files
  and the manifest clean; `app/build.gradle.kts` warns `targetSdk 36` is not
  the latest (expected). CLI `installDebug`: 8 s.
- 📏 First call downloaded the zh→en model and translated within seconds
  (21:24:31). No Wi-Fi-only condition; the phone was on Wi-Fi.
- 📏 Quality table, 14 Previews (5 real from the shade, 9 made up):
  ML Kit gist OK in 7 of 14; names/identifiers/mentions damaged in almost
  every row (`Jiarui` → `Jiaroi`, `oec.seller.assistant_api` →
  `OEC.Seller.Assistant_API`, `@you` → `@ you`); meaning changed in rows
  4, 6, 8, 11, 13, 14. Full table in `experiments/02-quality.md`.
- 🚩 Lark's engine also erred: row 10 changed an identifier; row 11 dropped
  the Sender. Rate limit `99991400` on 5 of 14 sequential calls; row 2 never
  got through in 4 tries.
- 🐚 Four CLI gotchas (zsh `mapfile`, `adb shell` eats loop stdin, `am start`
  same-intent delivery, `rg` without PCRE2) recorded in
  `experiments/02-quality.md`.
- 📐 Standing preference (Max): build through Android Studio when it is not
  harder; CLI stays fine for install + capture loops. Saved to memory.
- 📄 `experiments/02-quality.md`; `plan.md` Layer 2 marked done, three
  settled-facts rows added.

Open:

- ⏭️ STOP and ASK — Max: is the ML Kit column "gist OK" for a notification
  Preview? The table says: gist yes in half the rows, names damaged nearly
  everywhere. Options: keep ML Kit; switch to `LarkApiTranslator`; or ML Kit
  with the Sender split off before translation (removes every Sender-name
  error at no cost).

Next:

- Max writes `Preview.parse` in `Preview.kt` (stub in the working copy): the
  rule that splits `Sender: message`. It decides what the Translator sees.
- ▶️ Layer 3 (Relay) after Max's go.

### 2026-08-25 — Benchmark 2 graded: Lark clearly better; ML Kit stays for now

- 📐 Decision (Max): Claude writes the assessment (Max does not read Chinese).
  Rule: stick to ML Kit for faster iteration; record the Lark API as a
  follow-up if clearly better; abandon ML Kit only if unusably bad.
- 🧪 Benchmark 2: 30 made-up Previews in the register of Max's groups, dense
  with ByteDance/TikTok/Lark jargon. Same debug hook for ML Kit. Lark calls
  spaced 3 s apart: 0 rate-limit errors in 30 (vs 5 of 14 back-to-back).
- 📏 Grades (Claude): ML Kit 3 ✅ / 13 ⚠️ / 14 ❌; Lark 26 ✅ / 4 ⚠️ / 0 ❌.
  Row 26 `大家先别发布` ("nobody deploy yet") → ML Kit "everyone is released
  first": inverted, in fluent English. Table, readings, and assessment in
  `experiments/02-quality.md` ("Benchmark 2", "Assessment").
- 📐 Verdict (Claude, under Max's rule): ML Kit is usable as a hint (who,
  where, roughly what), not as a reading of engineering chat. Borderline,
  not unusable. Keep ML Kit through Layer 3; `LarkApiTranslator` is the
  first follow-up after Layer 3 and is expected to become the default.
- 📏 "Damage the Sender", defined: with `Sender: ` inside the sentence, ML Kit
  re-cases or misspells the name and splits `@you` → `@ you`; Lark dropped
  `Yufan Chen: ` twice (rows 11 and 23, both messages start with 收到).
- 📄 `plan.md` Later: `LarkApiTranslator` follow-up with constraints; TDD
  tests for `Preview.parse` after the experiments (Max's call); 2.0 idea
  (Max): longer and/or AI-summarized notifications — needs message text the
  listener never gets (user-identity message-read API).
- 🧱 `Preview.kt` committed as a documented stub (`data class Preview`,
  `parse` = `TODO()`), to be written test-first later.
- ⚠️ Caveat: Benchmark 2 is Claude's corpus, not real messages. Reading real
  group messages needs the `lark-cli` user identity (`search:message` and
  message-read scopes), missing on this machine, maybe tenant-gated.

Next:

- ▶️ Layer 3 (Relay) on Max's go, with ML Kit behind `Translator`.
- Optional: Max runs `lark-cli auth login` for `search:message` to pull real
  Previews into the benchmark.
- 📄 Follow-up (Max): `plan.md` Later now lists a proper app icon — small
  (monochrome, shown on every Relay) and launcher — for a late stage.

### 2026-08-25 — Real-message benchmark: verdict holds; message scopes granted

- 🔑 Recipe: `lark-cli auth login --scope "search:message" --no-wait --json`
  → open `verification_url` in Chrome (Claude drove Chrome Dev via the
  browser tools; Max picked "Browser 2 (Linux)") → click Authorize →
  `lark-cli auth login --device-code <code>`. User identity is now ready on
  this machine (token to 23:57, refresh for 7 days).
- 🚩 The token's "previously granted" scopes include `search:message`,
  `im:message.group_msg:get_as_user`, `im:message.p2p_msg:get_as_user`,
  `im:message:readonly`, `im:chat.user_setting:read`. The handoff's claim
  that message-read scopes are "effectively blocked" is false for this app.
  Noted in `../larklish-handoff.md` (dated update) and `plan.md`.
- 🔍 `lark-cli im +messages-search --as user --is-at-me` (14 days): 50 hits,
  38 Chinese. `--query 的 --chat-type group` (3 days): 50 hits. Many real
  Senders have Chinese names (温天信, 王蓝翔, 弋鹏玮).
- 🧪 Benchmark 3: 30 real messages flattened to Preview shape, both engines,
  Lark calls spaced 3 s (0 rate-limit errors). Tables, grades, and the
  revised assessment in `experiments/02-quality.md`.
- 📏 Grades: ML Kit 6 ✅ / 18 ⚠️ / 6 ❌; Lark 27 ✅ / 3 ⚠️ / 0 ❌. ML Kit gist
  is better on real plain chat than on jargon, but it translates Chinese
  Sender names literally (夏银璐 → "Summer Silver", 幸湘熔 → "Fortunately")
  and doubles or triples names in mentions. Lark romanizes or keeps them.
- 📐 Verdict unchanged (Max's rule): ML Kit through Layer 3, Lark API as
  the first follow-up. `Preview.parse` is now mandatory: the Sender must
  never go through ML Kit.
- 🐚 `lark-cli` message search needs `--as user`; the bot identity cannot see
  Max's groups.

Open:

- Chinese Sender names in the Relay: keep the characters (as Lark shows
  in-app) or romanize (as the Lark API does)? Max's call; keeping is free.

Next:

- ▶️ Layer 3 (Relay) on Max's go.
- 2.0 "full message" idea is feasible now (message-read scopes granted);
  still recorded only.

### 2026-08-25 — romanize() done test-first on the phone

- 📐 Decision (Max): Chinese Sender names are useless to him; romanize them
  if it is trivially easy. It was.
- 🧪 TDD, instrumented (ICU data lives on the phone): `RomanizeTest` under
  `app/src/androidTest`. Seam: `romanize(sender: String): String`. Slices:
  (1) 温天信 → "Wen Tianxin" red → green with ICU `Han-Latin/Names;
  Latin-ASCII`; (2) no-Han Senders unchanged (`Bits`, `Max Coplan`,
  `Daitao(DT) Song`) red → green; (3) Argos平台报警 → "Argos Ping Tai Bao
  Jing" red (ICU spaces the syllables; my name-join glued them) → green by
  joining only bare 2–4 character Han names.
- ✅ `./gradlew connectedDebugAndroidTest`: 3 tests, 0 failed, 12 s. Added
  `androidx.test.ext:junit:1.3.0`, `androidx.test:runner:1.7.0`,
  `AndroidJUnitRunner`.
- 🐚 Gotcha: one run reported "Instrumentation run failed due to Process
  crashed" with no stack trace; the rerun passed. Runner flake, not code.
- 🐚 Gotcha: editing a Kotlin file with `rstrip("}\n")` removed two braces
  and made three "runs" fail in <1 s. A build that fails in under a second
  is a compile error, not a test result. Rewrite the file whole.
- 📄 `plan.md`: Layer 3 Relay text = `romanize(sender) + ": " +
  translate(message)`; settled-facts row. `02-quality.md`: decision noted.

Next:

- ▶️ Layer 3 (Relay) on Max's go. `Preview.parse` test-first as its first
  slice, then wire `romanize` and `Translator` into the Relay.

## Layer 3 — Relay

### 2026-08-25 — Layer 3 core done: the Relay replaces the Original

- 📐 Decision (Max): TDD is optional for experiments; do not let tests lock
  in a design. `Preview.parse` and `romanize` keep their tests; the Relay
  has none.
- 🧱 `Preview.parse` (4 unit tests, JVM): split at the first `: `, `@you` /
  `@all` → `Mention`, no-colon text is all message. `Relay.kt`: Relay =
  Original's `contentIntent` + `largeIcon` + `when`, our channel `relay`,
  text = `romanize(sender)[@mention]: englishOf(message)`. `LarkListener`:
  skip `FLAG_GROUP_SUMMARY`, translate only Han text, `notify(key, 1, …)`
  then `cancelNotification(key)`; on removal (reason ≠ our own listener
  cancel) cancel the Relay.
- 🐛→✅ First Relay was dropped silently: `POST_NOTIFICATIONS` was never
  declared or granted. Manifest + `pm grant` fixed it.
- 🐚 Gotcha: `connectedDebugAndroidTest` uninstalls the app afterwards,
  wiping the listener grant, the permission grant, and the ML Kit model.
  Re-grant and expect the first translation to re-download ~30 MB.
- 🐚 Gotcha: `adb shell cmd statusbar expand-notifications` before `input
  tap`; the first tap landed on the launcher because the shade was closed.
- ✅ End-to-end (bot DM): send 22:28:21 → `relayed` 22:28:26; `dumpsys`
  shows 1 larklish record, 0 larksuite; expanded shade shows "Larklish •
  now / Lark / <English BigText>"; tap → `NexusBotActivity` (the right
  chat) and the Relay auto-cancels. Screenshots in the session scratchpad;
  evidence in `experiments/03-relay.md`.
- 📏 Lark did not truncate this message; truncation is variable. ML Kit
  inverted `先别发布` again ("everyone is released") — engine swap stays
  the first follow-up.
- ⏳ Waiting on real traffic: group-message Relay and Lark's own withdrawal
  (`onNotificationRemoved` mirror). The listener stays installed and logs.

Next:

- Watch real traffic for a group Relay and a withdrawal; record both in
  `experiments/03-relay.md`.
- Then the follow-ups in plan.md "Later": `LarkApiTranslator`, app icon,
  onboarding.
- 🧱 Follow-up (Max): `cancelNotification` now runs only in release builds
  (`!BuildConfig.DEBUG`); debug keeps the Original beside the Relay for
  comparison. Verified: after a send, dumpsys shows 1 larksuite record and
  1 larklish record.
- 📏 Follow-up finding: Lark posts no notification for the chat that is open
  in the foreground. The silent test run happened because the tap test left
  the bot chat open; `input keyevent KEYCODE_HOME` fixed it.
- 📏 Follow-up finding: the Original's id changed (948177842 → 948177873)
  after the previous notification was tapped, so the id is per-chat only
  while the notification lives.
- ↩️ Correction: the Relay's `largeIcon` does carry over (avatar thumbnail
  on the collapsed row); the expanded view just hides it. Only the small
  icon is still the placeholder. `experiments/03-relay.md` corrected.

### 2026-08-25 — Pending Layer 3 paths closed synthetically; recall surprise

- 📐 Max challenged "needs real traffic". Correct: both paths were
  synthesizable with granted scopes.
- 🧪 Group Relay: `+chat-create` made `Larklish 测试群`
  (`oc_e2feee77ba20b6c5285d22fb8aa90eb4`, Max + bot), bot posted Chinese.
  Relay title `LarkLish Test Group` (title goes through `englishOf`), group
  avatar carried, tap → `ChatWindowActivity` (the right chat).
- 🚩 Recall does **not** withdraw: `im messages delete` (Max confirmed the
  high-risk `--yes`) made Lark update the Original in place to "… recalled
  a message." Same key, a posted event, no removal. The Relay updated too.
- ✅ Withdrawal mirror: tap on the Original → `reason=1` (click) → Relay
  canceled. Opening Lark → two `reason=9` (app cancel all) → mirrored,
  including the harmless no-op cancel for the summary key. End state: zero
  records both sides.
- 📏 Android auto-groups our Relays as well (own summary appears at 2+).
- 🐚 Gotcha: a collapsed notification group swallows child taps; tap the
  `2 ⌄` expander first (`input tap` on its coordinates), then the child.
- 📄 `experiments/03-relay.md` + `plan.md`: Layer 3 marked done.

Next:

- Follow-ups from plan.md "Later": `LarkApiTranslator`, app icon,
  onboarding. The test group stays for future experiments.

### 2026-08-25 — Emoji: gate extracted and tested; two engine defects found

- 🐛 Max found it: the translator screen turned a lone 😀 into **"Bamboo"**.
  The screen was sending everything to ML Kit; only the listener had a gate.
- 🧱 Gate extracted: `Han.kt` (`Char.isHan`, `String.hasHan`, ICU-free) and
  `Translator.englishOf` extension. Both the Relay and the screen use it;
  `MlKitTranslator` stays engine-only.
- 🧪 Unit tests (JVM): `EnglishOfTest` with a fake `Translator` proves emoji
  and English never reach the engine and Han+emoji goes whole; `PreviewTest`
  gains emoji in Sender and message. `RomanizeTest` gains emoji cases on the
  phone: 😀 unchanged, `陈昱萌😀` → "Chen Yu Meng😀" (ICU glues the emoji to
  the last syllable). 9 JVM + 5 instrumented, all green.
- 🐛→✅ First run: `ExceptionInInitializerError` — putting `hasHan` in
  `Romanize.kt` dragged the ICU `Transliterator` init onto the JVM, where
  `android.icu` is a stub. Han helpers moved to their own ICU-free file.
- 🔑 Recipe: run instrumented tests WITHOUT the uninstall side effect:
  `./gradlew installDebug installDebugAndroidTest` then `adb shell am
  instrument -w com.vegerot.larklish.test/androidx.test.runner.
  AndroidJUnitRunner`. Grants survive; only the process dies — toggle
  `allow_listener` to rebind.
- 📏 Probes: 😀 → 😀 through the screen (gate works); ML Kit deletes emoji
  inside Chinese text (`…😀，…👍🏼` → none in output). Engine defect;
  revisit with `LarkApiTranslator`. Noted in `experiments/02-quality.md`.

Next:

- Follow-ups from plan.md "Later": `LarkApiTranslator`, app icon,
  onboarding.

### 2026-08-25 — Recorder + events UI: the soak now produces a dataset

- 🐛 Max asked "are we recording all the notifications?" — we were not.
  Logcat is a ring buffer, wiped by reboot and by every `adb logcat -c`.
- 🧱 `Recorder`: appends JSONL to `filesDir/events.jsonl` — one `relayed`
  line per Relay (Original title/text + Relay title/text) and one `removed`
  line per removal with its reason (all reasons, including our own cancel).
  `LarkListener` writes; `MainActivity` reads.
- 🧱 Events UI in `MainActivity`: below the translator, newest first —
  Relay text prominent, Chinese Original underneath, removal rows with the
  reason code. Refresh button rereads the file.
- ✅ Live test: bot DM at 23:20 → one `relayed` JSONL line with both texts;
  the UI shows the pair. Shell read works:
  `adb exec-out run-as com.vegerot.larklish cat files/events.jsonl`.
- 📄 `plan.md`: the "Debug UI: recent Original → Relay pairs" Later item is
  done.

Next:

- Soak overnight; read `events.jsonl` for the quality report and the
  desktop-withdrawal reason.
- Then `LarkApiTranslator`.

### 2026-08-25 — Redeploy saga: adb wedge, silent uninstall, soak re-armed

- 🐛→✅ Redeploy failed with ddmlib `TimeoutException`. `adb devices` still
  said `device` while `adb shell` timed out — the state listing lies; probe
  with a real shell command. A genuine `adb kill-server` + `start-server`
  plus a few seconds of re-enumeration fixed it (`lsusb` showed the phone
  present throughout).
- 🐚 Gotcha: `pkill --full 'gradlew installDebug'` matches the shell that
  runs it (the pattern is inside its own command line) and kills it.
  Exit 144 with no output = self-inflicted.
- 🚩 Something fully uninstalled and reinstalled the app at 23:46 (new uid,
  fresh `firstInstallTime`; likely a Studio deploy replacing the CLI-signed
  install). That silently wiped: `events.jsonl` (test rows only, no loss),
  the ML Kit model, and the `POST_NOTIFICATIONS` grant — which would have
  dropped every Relay overnight without a trace.
- ✅ Re-armed for the soak: `pm grant … POST_NOTIFICATIONS` (verified),
  listener rebound (`connected`), model re-downloaded via the debug hook.
- 📏 One real Original (Lanxiang Wang, Seller group) arrived while the
  listener was unbound; it has no Relay. Expected: posted events are not
  delivered retroactively.

Next:

- Soak overnight; read `events.jsonl` in the morning.
- Then `LarkApiTranslator` (plan discussion parked; Max denied the plan
  gate for now).

### 2026-08-26 — Soak analysis

- 📊 Read `events.jsonl` after 10 h of real traffic: 9 Relays, 9 removals. One
  process the whole night (pid 10106, up 10:06:45), zero crash-log entries,
  9/9 bound-window Originals relayed, zero stale Relays in the shade. Full
  tables in `docs/experiments/04-soak.md`.
- 📏 Withdrawal reason answered: always `9` (`REASON_APP_CANCEL_ALL`). Lark
  calls `cancelAll()` when Max reads the chat on any device; 7–19 s after the
  post while awake, 7 h for the Original that arrived after Max slept. The
  Experiment 03 guess (`reason=8`) was wrong. `plan.md` table updated.
- ⚠️ ML Kit on real traffic: English Previews pass through untouched (Han
  gate works); Han messages keep the gist but lose name/identifier case
  ("zhifu liu", "OEC.supply_Chain"); the group title 【高优】容灾建设专项-2026
  became "[High excellent] Disaster-Disaster Construction Special -2026" four
  times. Not unusably bad; `LarkApiTranslator` stays the first follow-up.
- 🐛 Recorder defects seen in the data (not fixed yet): the `text` column is
  rebuilt from `Preview` and drops `@you`/`@all`; removal rows include Lark's
  autogroup summary key.

Next:

- Fix the two recorder defects (record raw `EXTRA_TEXT`; skip summary removals).
- Then `LarkApiTranslator` (plan discussion parked; Max denied the plan
  gate for now).

### 2026-08-26 — Recorder fixes from the soak

- 🐛→✅ `Recorder.relayed` now gets the raw `EXTRA_TEXT`, so `@you`/`@all`
  survive in the `text` column. Verified with a bot DM that mentions Max:
  row reads `Max Coplan's Feishu CLI@you: …` on both sides.
- 🐛→✅ `onNotificationRemoved` skips `FLAG_GROUP_SUMMARY`, like the posted
  path. Verified: a `cancelAll` (`reason=9`) with two Originals live produced
  two removal rows and no summary row.
- ✅ `installDebug` upgrade kept the 18 soak rows, the grant, and the model.
  Listener rebound. JVM tests green.

Next:

- `LarkApiTranslator` (plan discussion parked; Max denied the plan gate for
  now).

### 2026-08-26 — Experiment 05: Lark translate API over raw HTTP

- 🔑 Tenant is Feishu-brand → host `open.feishu.cn`. Reused the CLI app
  `cli_aa949bbb72e39cde` (has `translation:text`); decrypted its secret from
  `lark-cli`'s AES-GCM file store into the scratchpad (never printed).
- 🧪 From the Linux machine with `urllib`: token endpoint (7200 s), translate
  endpoint, 8 sequential + 8 concurrent calls with no rate limit, newline
  survives a combined call, error shape HTTP 400 + `code`. Phone pings the
  host at 124 ms. Doc: 1,000 chars/call, 20 QPS per tenant.
- 🚩 Latin-heavy input (`Chenglong Song: oec.… 需要扩容。`) passes through
  untranslated, 3 of 3 times; without the Sender it translates. The app
  must treat unchanged output as a failure.
- ✅ Lark fixes every ML Kit failure from the soak (names, case, emoji,
  "High excellent"). Full tables in `docs/experiments/05-lark-api.md`.
- 📄 `plan.md`: Layer 4 added (Lark primary, ML Kit fallback with `~`
  prefix, credentials via `local.properties` → `BuildConfig`).

Next:

- Implement Layer 4 and verify on the phone.

### 2026-08-26 — Layer 4: LarkApiTranslator with ML Kit fallback ✅

- 🧩 `LarkApiTranslator.kt`: `HttpURLConnection` + `org.json`, in-memory tenant
  token renewed a minute before `expire`, throws on non-200 / `code ≠ 0` / unchanged
  output. `FallbackTranslator.kt`: ML Kit on failure, output prefixed `~`;
  `defaultTranslator()` wires both. `LarkListener` and `MainActivity` use it.
- 🔑 Credentials: `local.properties` (`lark.appId`, `lark.appSecret`, gitignored)
  → `buildConfigField` → `BuildConfig`. `testOptions.unitTests.isReturnDefaultValues`
  so `android.util.Log` works in JVM tests.
- 🧪 `FallbackTranslatorTest` (2 JVM tests): primary result unmarked, fallback
  marked `~`. All 11 JVM tests green.
- ✅ Phone: hook `【高优】容灾建设专项-2026` → `[High priority] disaster recovery
  construction special project -2026`; `陈昱萌😀: 先别发布…` → `Chen Yumeng 😀:
  Don't release it yet…`; the Latin-heavy pass-through case →
  `~Chenglong Song: OEC.supply_Chain…` (ML Kit, marked).
- ✅ Bot DM through the listener → Relay `The fourth layer is online: Feishu
  translation priority, ML Kit fallback 🚀`; row in `events.jsonl`.
- ✅ Radios off → `UnknownHostException` in ~200 ms → `~Please complete the
  grayscale release…`. Radios restored, host reachable again.
- 📄 `plan.md`: Layer 4 done; `local.properties` keys in Commands.

Next:

- Soak Layer 4 on real traffic; count `~` rows in `events.jsonl`.
- Later items unchanged (icon, onboarding, release-build check).

### 2026-08-26 — Back on the Mac: toolchain verified, docs updated

- 🔍 Dev machine is the Mac again, inside Android Studio. SDK at
  `~/Library/Android/sdk` (Studio wrote `sdk.dir`); `sdk` here is SDKMAN, not
  the Android SDK. Studio `build_project`: success, 0 problems.
  `./gradlew testDebugUnitTest`: 10/10 green.
- 📊 `events.jsonl` after Layer 4 (13 Relays): 0 `~` rows, so every call
  reached Lark's engine. One Han message: `出1.5 k美金，换人民币🙏[FistBump]`
  → `Sell 1.5k USD for RMB 🙏 [FistBump]` ✅. New removal reason `12`
  (`REASON_GROUP_SUMMARY_CANCELED`, Max swiped the group away) mirrored fine.
- 📄 Max removed `larklish-handoff.md` and `larklish-conversation.md`
  (`69c66f0`). `CLAUDE.md` (a symlink to `AGENTS.md`) and `plan.md` now point
  at `plan.md` + `progress.md` instead. `plan.md` Commands rewritten for macOS.

Next:

- Keep soaking Layer 4; the row to watch is a Han group title through Lark.
- Later items unchanged: app icon, onboarding, release-build check, dedicated
  `translation:text`-only Lark app.

### 2026-08-26 — First deploy from the Mac: same signer, data kept

- 🔑 Recipe: the phone's install is signed by the Linux debug keystore
  (`CD:5B:25:F8…`). Studio had generated a new Mac keystore (`9A:3D:D0:12…`),
  which would have failed as a signature mismatch (or forced an uninstall
  that wipes `events.jsonl`, the grants, and the ML Kit model). Max supplied
  the Linux keystore as `debug.keystore.base64`; decoded to
  `~/.android/debug.keystore` (Mac one kept as `debug.keystore.mac-studio-2026-08-26.bak`).
  `apksigner verify --print-certs` on the pulled `base.apk` confirmed the match.
- ✅ `./gradlew installDebug` (42 s) upgraded in place: `events.jsonl` 74 rows,
  byte-identical to the pre-deploy copy; `POST_NOTIFICATIONS` still granted;
  listener rebound at once (`connected; active Lark notifications: 4`);
  `no_backup/com.google.mlkit.translate.models/en_zh` still present.
- ✅ End-to-end: bot DM sent 15:55:40 → `relayed` 15:55:46 (6 s). Relay text
  `Mac deployment test: This notification should be translated from Feishu
  engine, without tilde 🍎` — Lark engine, no `~`, emoji kept.
- 📏 `events.jsonl` grew 43 → 74 rows between 12:00 and 15:50 PDT on real
  traffic with the Layer 4 build; not yet graded.

Next:

- Grade the 31 new Layer 4 rows (look for `~` rows and Han group titles).
- Later items unchanged: app icon, onboarding, release-build check, dedicated
  `translation:text`-only Lark app.

### 2026-08-26 — Layer 4 soak graded: 0 fallbacks, Lark 5 ✅ / 3 ⚠️ / 0 ❌

- 📊 36 Relays since the Layer 4 build (10:29 PDT): no `~` rows, 28 English
  pass-throughs untouched, removals `9` ×11 and `12` ×4 all mirrored.
- 📏 8 Han rows graded (Claude): 5 ✅ / 3 ⚠️ / 0 ❌. First Han group title through
  Lark (商家经营 / Seller… → "Merchant Operating/Seller…") is readable. Idioms go
  literal (卧龙凤雏 → "sleeping dragon and a phoenix"). Tables in
  `experiments/05-lark-api.md` "Soak".
- 🚩 Finding: `romanize()` turns bot Senders with Han into pinyin words
  (`Triton数据安全@you` → `Triton Shu Ju An Quan@you`). Lark's engine translates
  those correctly. Proposal: romanize only bare 2–4-character Han names; send
  other Han Senders through the Translator. Max's call.
- 📄 `plan.md` settled-facts row added.

Next:

- Max decides on the Sender rule (romanize person names only?).
- Decide `debug.keystore.base64` (commit or ignore); push the stack when Max says.
- Later items unchanged: release-build check, onboarding, app icon, dedicated
  `translation:text`-only Lark app.

### 2026-08-26 — tools/events: the Relay record, with views

- 🧱 `tools/events` (stdlib Python): pulls `events.jsonl` from the phone
  (`adb exec-out run-as … cat`) or reads a saved copy (`--file`). Views:
  `list` (Original → Relay pairs, removals with reason names, local time),
  `stats` (Relays, fallbacks, Han text/title, English pass-through, removal
  reasons, time span), `table` (Markdown `Original | Relay | Grade` for the
  experiment docs), `pull OUT`. Filters: `--since ISO-UTC`, `--han`,
  `--fallback`, `--no-removed`, `--utc`.
- ✅ Verified on the phone: `--since 2026-08-26T17:29 stats` reproduces the soak
  grade inputs (36 relayed, 0 fallback, 8 Han rows). Extend it when a new view
  is needed (Max).
- 📄 `plan.md` Commands: the raw `adb exec-out` line replaced by the tool.

Next:

- Max decides on the Sender rule (romanize person names only?).
- Decide `debug.keystore.base64` (commit or ignore); push the stack when Max says.

### 2026-08-26 — debug.keystore committed

- 🔑 `debug.keystore` (the Linux debug signer, `CD:5B:25:F8…`) is in the repo
  root. Decision (Max): a debug key is not a secret (password `android`,
  debug builds only); sharing it stops the signature-mismatch trap between
  machines. `plan.md` Commands: `cp debug.keystore ~/.android/debug.keystore`
  once per machine. `local.properties` (Lark app secret) stays ignored.

### 2026-08-26 — Experiment 06: the Original cannot carry the full message

- 🧪 E1: extras of a live Original hold no extra text (`subText` null, no
  `textLines`/`messages`); the tap intent (`NotificationTransferActivity`) has
  extras that `dumpsys` never prints, root or not.
- 📏 E5, ten bot DMs: Lark keeps **45 characters** of message text, backs up to
  the last space/punctuation, appends `...`; a newline ends the Preview; **no
  boundary before 45 → empty Preview** (`Sender:...`). Han and Latin count the
  same. Tables in `experiments/06-full-message.md`.
- 📐 Consequence: the full text can only come from the Open API under the user
  identity, keyed by chat (source B). `plan.md` 2.0 item rewritten; settled-facts
  row added.

Next:

- E2: `lark-cli --as user` chat lookup by title + latest-message latency
  (user identity on this Mac is `needs_refresh`).
- E4: user token lifetime for a phone-side fetch.

### 2026-08-26 — Experiment 06 continued: source B works, token lifetime known

- ✅ E2 (`lark-cli api --as user`): `chats/search` by exact title → 1 hit; by the
  truncated title prefix → 2 hits (filter by name prefix); `messages` list
  (`ByCreateTimeDesc`) returns the full text as `body.content` JSON; a bot DM
  was readable 3.0 s after the send, on the first poll; 10 back-to-back calls,
  no rate-limit error.
- ✅ E4: user token 2 h, refresh token 7 days sliding (`lark-cli auth status`),
  `offline_access` granted.
- ⏳ Open: DM lookup by person name; Max's call on per-Original lookups under
  the user identity. Sketch recorded in `experiments/06-full-message.md`.

Next:

- Max decides: pursue the full-message layer (sketch), or park it.
- Parked: push the stack (9 commits above `origin/main`); Later items.

### 2026-08-27 — Experiment 07: deep links

- 🔍 Lark's tap: `NotificationTransferActivity (has extras)` → `MainActivity` →
  the chat activity. The extras hold the target; nothing prints them (root or
  not) and a `PendingIntent` is opaque. Copying it (Layer 3) is the only way.
- ✅ AppLinks fired by us (`am start VIEW`): `client/chat/open?openChatId=`
  (group → `ChatWindowActivity`, topic group → `ThreadWindowActivity`),
  `openId=` (DM), `client/bot/open?appId=` (bot) — all four URL forms work.
- ❌ No AppLink opens a message or thread; `messageId=` is ignored and the
  `client/message/link/open?token=` token is client-minted (no API). The API
  does expose `thread_id`, `root_id`, `parent_id`, `mentions` per message.
- 🧱 `tools/phone` (`top`, `open URL [shot]`, `shade [shot]`, `shot`, `home`)
  from the experiment's one-liners (Max's ask).
- 📄 `experiments/07-deep-links.md`; `plan.md` settled-facts row + Commands.

Next:

- Max decides on the full-message layer; the tap target stays Lark's
  `PendingIntent` either way.

### 2026-08-27 — Full-message experiments continued: DMs, search API, `position`

- ❌ `chats/search` returns groups only, never p2p chats (bot name: 0 hits; person
  name: group hits only).
- ✅ `POST /im/v1/messages/search` (user identity) returns `meta_data.chat_id`,
  `is_p2p_chat`, `position`, `message_app_link` per hit — the way to learn a DM's
  chat id, then cache it per Sender. Index latency **15 s** (list endpoint: 3 s).
- ❌ `position` in `client/chat/open` and the `client/thread/open` link are not
  honored by the phone client (opened at the unread divider / did nothing).
  Lark's own tap keeps its private extras; copying the `PendingIntent` stays.
- 📐 Design update: two-phase Relay — post from the Preview now, update the same
  Relay with the full text when the fetch returns. Recorded in
  `experiments/06-full-message.md`; two settled-facts rows in `plan.md`.

Next:

- Layer 5 plan (Max's go): sign-in screen + token store → `MessageFetcher`
  (group by title, DM by cached chat id / search) → full-text Relay update.

### 2026-08-27 — Layer 5 plan settled; docs

- 📐 Grilling done (19 questions, Max's answers). Decisions in `plan.md` Layer 5;
  words **Full text** and **Update** in `CONTEXT.md`; new `plan.md` section
  **Shortcuts** (what to fix before a public app); four new Later items (`post`
  and other types, fetch-only-when-truncated, AI summary, match-rule tightening).
- 🗂️ Commits planned: docs → `LarkHttp` extraction → `UserToken` +
  `tools/lark-token` → `MessageFetcher` + tests → Update in `LarkListener` → DMs.

Next:

- Commit 2: extract `LarkHttp.kt` from `LarkApiTranslator` (no behavior change).

### 2026-08-27 — Layer 5 commits 2–3: `LarkHttp`, `UserToken`, `tools/lark-token`

- 🔧 `LarkHttp.kt` (`postJson`, `getJson`) extracted from `LarkApiTranslator`;
  debug hook still gives Lark-quality output.
- 🔍 Experiment: lark-cli's store decrypts with ~20 lines of Python (`cryptography`,
  AES-GCM). macOS master key = keychain item with a `go-keyring-base64:` prefix
  (double base64); Linux = raw `master.key`. Verified on the Mac and on the Debian
  box (`ssh 10.251.236.182`; Debian's app-secret blob decrypts to the same secret).
- 🧱 `tools/lark-token [--write]`: masked summary; `--write` puts `lark.appId`,
  `lark.appSecret`, `lark.userRefreshToken` into `local.properties`. A `uv` script
  (inline `cryptography` dep); `uv` installed on Debian for it.
- 🔑 `UserToken.kt`: seed from `BuildConfig.LARK_USER_REFRESH_TOKEN`, refresh 5 min
  early, persist rotations in `filesDir/user-token.json`, a new seed replaces the
  file. Verified on the phone: seed → refresh → `user_info` = Max's open_id;
  a forced second refresh rotated again (`…yDLg` → `…lddQ`).
- 📎 Max ran `lark-cli auth login` on Debian mid-experiment: the Mac's token kept
  working, so Lark allows concurrent user tokens per user+app (at least across
  logins).

Next:

- Commit 4: `MessageFetcher.kt` + `pickMessage` / `resolveMentions` tests (test-first).
- Check after 15:57 PDT that the Mac's lark-cli still refreshes (its access token
  expires then) — i.e. the phone's rotations did not break the Mac's chain.

### 2026-08-27 — Layer 5 commit 4: `MessageFetcher`

- 🧪 Test-first: `PickMessageTest` (6 cases: stem match, empty stem, window
  edges, recalled messages, `type:post`, no-match) and `MentionsTest` (3),
  plus `Preview.parse("Bits:...")` → sender `Bits`, message `...`, stem `""`.
  The record holds 6 such empty-Preview rows (all from the E5 probes).
- 🔍 Live shapes via `lark-cli api --as user … --params '{…}'` (inline `?query`
  is dropped by lark-cli — that cost 10 minutes): `messages` items carry
  `msg_type`, `create_time` (string ms), `body.content` (JSON string),
  `deleted` (a recalled message stays in the list), `mentions[].key/name`
  for `@_user_N` placeholders.
- 🧱 `MessageFetcher.kt`: `Candidate`, `Pick.Found/Skipped(reason)`,
  `pickMessage` (window `[when − 15 s, when + 5 s]`, newest `text` whose text
  starts with the stem; else `no-match` or `type:<msg_type>`), `resolveMentions`,
  chat-id cache `filesDir/chats.json`. Debug hook `--es debug fetch --es title T`.
- ✅ On the phone: 60-char bot message → hook → `Found(…full text…)`, cache written.

Next:

- Commit 5: the Update in `LarkListener` (+ cancel per key), `Recorder`
  `updated`/`skipped`, `tools/events` `U`/`S` rows and stats, debug UI, error
  notification in debug builds.

### 2026-08-27 — Layer 5 commit 5: the Update ✅ end-to-end

- 🧱 `LarkListener.update()`: after the Preview Relay, `fetcher.fullTextOf(title,
  preview, sbn.postTime)` → `englishOf(fullText.take(1000))` → `notify` on the same
  key → `recorder.updated`; `Pick.Skipped` → `recorder.skipped(reason)`; any
  exception → `skipped("error: …")` + an "Errors (debug)" notification in debug
  builds. One `Job` per Original key; a newer Original cancels the older Update.
- 🔕 `Relay.kt`: `setOnlyAlertOnce(true)` so the Update replaces the text silently.
- 📊 `tools/events`: `U` rows (`msgType`, Full text → Relay text), `S` rows
  (reason); `stats` prints updated/skipped counts, message types, skip reasons.
  `MainActivity` lists Updates and skips under their Relay.
- ✅ Phone, test group: 61-char Han bot message → Relay `Sender: ...` → 4 s later
  the Relay shows the full English text (two sentences, second line kept).
  Two sends 2.5 s apart: message 2's fetch returned `Found` but its Update was
  cancelled by Original 3; only message 3's Update landed (`tools/events list`).

Next:

- Commit 6: DMs — `messages/search` (`chat_type=p2p`) for the chat id, cached
  per Sender in `chats.json`.
- Then soak ≥ 1 day; grade with `tools/events stats`.

### 2026-08-27 — Layer 5 commit 6: DMs ✅ — Layer 5 built, soak starts

- 🔍 Probe: `messages/search` ignores `chat_type` when `query` is empty (15 mixed
  hits), but every hit has `meta_data.is_p2p_chat` and a `display_info` whose first
  line is the peer name (`Argos平台报警`, `Max Coplan's Feishu CLI`) — the Sender.
  Bot DM Originals carry title `Lark`.
- 🐛 Prefix match bit: the commit-5 build cached `Lark` → `Larklish 测试群`. Group
  lookup is now exact unless the title ends in `...`. A stale entry survives an
  `rm chats.json` because the service holds the map in memory → `force-stop`
  (Commands).
- 🧱 `MessageFetcher.dmChatId`: search `[when − 60 s, when + 60 s]` (+ `query` = stem
  when non-empty), first `is_p2p_chat` hit whose peer name == Sender, up to 4 tries
  5 s apart; cached as `dm:<Sender>`.
- ✅ Phone: bot DM with an empty Preview → Update 7 s after the Relay (search); second
  DM → 3 s (cache). Group and cancel paths unchanged (commit 5).
- 📌 Layer 5 is built: 6 commits (`869059`…). The soak runs on real traffic from now.

Next:

- Soak ≥ 1 day, then `tools/events --since 2026-08-27T22:38 stats`: updated vs
  skipped by reason (`no-chat` = DM lookups that missed, `no-match` = picker misses,
  `type:*` = non-text traffic), and read the `U` rows for wrong picks (Q11).
- Check after 15:57 PDT that the Mac's lark-cli still refreshes its own token (the
  phone rotated its chain twice today).
- Parked: push the stack (now 19 commits above `origin/main`); Later items in `plan.md`.

### 2026-08-27 — Soak review (the one we skipped): Experiment 08

- 📊 `docs/experiments/08-soak.md`: 73 real Relays in a day. 53 % truncated Previews,
  0 empty, 23 % DM-shaped, Previews can span two lines, withdrawal median 11.5 min.
  Han rows: Lark 12 ✅ / 4 ⚠️ / 0 ❌; ML Kit fallback 2 ❌; romanize 2 ❌.
- 🐛 `tools/events` hid every fallback (`~` after `Sender: `) — fixed; the two real
  fallbacks are Latin-heavy sentences ML Kit made worse → Later item "translate only
  the Han runs".
- 🔍 `post` is the everyday type (4 of 12 newest in `San Jose 1199…`; a plain mobile
  sentence is a `post`); its content is text paragraphs → Q6 rule → commit 7.
- 🔑 Refresh tokens are single-use: at 15:57 the Mac's lark-cli lost its user token
  (`token_missing`) because the phone had rotated the copied one. Re-login started
  (device code, Max confirms in the browser). Shortcuts + Commands + tool docstring say so.

Next:

- Finish the Mac re-login (`lark-cli auth login --device-code …` after Max confirms).
- Commit 7: flatten `post` messages in `MessageFetcher`; whitespace-insensitive stem
  match (two-line Previews); verify with a bot `post` into the test group.

### 2026-08-27 — Layer 5 commit 7: `post` messages ✅

- 🧱 `postText()` flattens a `post` (title, one line per paragraph; `text`/`a` → text,
  `at` → `@user_name`, `img` → `[image]`); `pickMessage` compares whitespace-free
  (a two-line Preview or a flattened post still starts with the stem); new reason
  `no-message` (empty window) next to `no-match`; the fetcher logs its candidates.
- 🐛 Found on the way: the `dm:<Sender>` cache was consulted before the group path, so a
  group post by the bot (which also DMs Max) listed the *DM* chat. Now the Original's
  shape decides (title `Lark` or title == Sender → DM), and the caches never cross.
- ✅ Bot `post` (title + 2 paragraphs + `@Max`) into the test group → Relay shows the
  title (Lark's Preview for a post) → Update shows all three lines in English with the
  mention resolved.
- ❓ Person DMs: none in the record yet; assumed title == Sender. Watch for `no-chat`
  rows with a person's name as title.

Next:

- Soak on real traffic (Layer 5 is complete: 7 commits). Grade with
  `tools/events --since 2026-08-27T23:09 stats` after ≥ 1 day.
- ~~Finish the Mac lark-cli re-login~~ ✅ done (Max logged in again; `--as user` works).

### 2026-08-27 — App icon v1 (译鸟)

- 🎨 Max picked an AI-generated flat bird (navy body, orange belly, two chirp arcs on a
  teal `#00d3b2` disc): `docs/logo/larklish-v1.png`. Cut the bird out of the disc
  (pillow/numpy under `uv`), placed it at 60 dp inside the 108 dp adaptive canvas →
  `res/drawable-xxxhdpi/ic_launcher_foreground.png`; solid-color background; the same
  alpha silhouette as `ic_launcher_monochrome.png` (themed icons) and as the 24 dp
  notification small icon `ic_notification.png` (Relay + error notification).
- ✅ `dumpsys notification`: the Relay's icon is now an app resource; installed in place.
- 📝 The finish-line todo (bold arcs, rounded wing tip, 66 % safe zone, aqua wing,
  SVG → `VectorDrawable`) is the "App icon" item in `plan.md` Later.

Next:

- Soak continues; icon polish per the todo when the artist delivers a vector.

### 2026-08-28 — Soak 2: Layer 5 for a day (Experiment 09)

- 📊 91 real Relays, **34 Updated (37 %)**, median 2.3 s. `post` is 2 of 3 Updated
  messages. No crashes. Full write-up: `docs/experiments/09-soak.md`.
- 🔍 The chat resolver is the bottleneck; only 2 chats ever succeed. Five root causes,
  each reproduced against the live Open API today:
  1. **Threads**: a topic chat puts the thread's root text in the title, so
     `chats/search` finds nothing. The replies *are* in the chat's own message list.
  2. **Ambiguous prefix**: `【bytedcli MR 处理】...` matches 6 chats; we cache the first
     one forever → every later MR is `no-message`.
  3. **No retry** on the message list (we give up 1.8 s after the Original).
  4. **Strict match**: `emotion` tags become `[emotion]` not `[Delighted]`, and Lark's
     Preview drops a trailing bold run, so `startsWith(stem)` fails.
  5. **Fallback on transport errors**: all 11 `~` rows are one 2.5 h block; Lark
     translates the same strings well today, ML Kit mangles them.
- 🆕 Person DMs also carry the title `Lark`; `title == Sender` never happened.

Next:

- Layer 6 "make the Update land" — the five fixes above, ~34/91 → ~63/91.

### 2026-08-28 — Which thread resolver? (Experiment 10)

- 🧪 Replayed all 210 Relays (08-26 → 28) offline against the live API through the three
  candidate resolvers. Scripts in `tools/replay/`; write-up in
  `docs/experiments/10-thread-resolver.md`.
- 📉 The ceiling is **3 messages in 3 days**. Twelve Relays have a title `chats/search`
  cannot resolve, but 9 are reactions, recalls or calendar reminders with nothing to fetch.
- 🥇 **LRU 2 wins**: +3 for +71 GETs. Scanning every cached chat finds the same 3 for
  +278 GETs. A thread-root cache alone finds **0** — it can never earn the first hit.
- 🛡️ 0 of 105 stems match in more than one of 35 chats, so the LRU fallback cannot pick
  the wrong chat.
- 🚫 Running the fallback after `no-message`/`no-match` too: **+0** found, +134…+415 GETs.
- ⚖️ Re-ranked Layer 6: the loose match is worth **+7**, the thread resolver **+3**, prefix
  disambiguation **+1** (most `【bytedcli MR 处理】` messages are `interactive` cards).

### 2026-08-28 — All five Layer 6 fixes measured (Experiment 11)

- 🧪 Ran every proposed fix through the Experiment 10 harness (210 Relays). Four of my
  five guesses were wrong. Write-up: `docs/experiments/11-layer6-fixes.md`.
- ❌ **Fix 1 (retry the message list) is worth 0.** The forward edge is already generous —
  no message ever arrives after its Original. The **backward** edge is the bug: the
  message-to-Original delay runs p50 5.1 s, p95 24.3 s, **max 43.9 s**, so
  `BEFORE_MS = 15_000` throws away the top 7 %. `60_000` recovers **+6** for zero extra
  API calls and zero ambiguity. One constant, not a retry loop.
- ✅ Fix 2 **+7** (the prefix match carries it; `emotion` alone is +2 and subsumed, but keep
  it — the Relay should show `[Delighted]`, not `[emotion]`).
- ✅ Fix 3 **+1**, fix 4 **+3**, fix 5 **10 of 11 Relays** (16 of 18 Han fragments translate
  fine through Lark today; 1 is genuinely `unchanged`; 1 transport failure reproduced live,
  and 0 of 60 sequential calls failed — failures cluster).
- 📈 Stacked: **101 → 119 of 210 Relays (48 % → 56 %)**, GETs 170 → 367.
- 🆕 Messages get **edited** after posting and the API returns the edited text, so a stem
  match can never succeed on those. `text` bodies can contain HTML (`<p>…`): 11 of 198.

Next:

- Layer 6 in the measured order: window 60 s → prefix match → emotion → translate retry →
  LRU 2 → (optional) disambiguate. Drop the message-list retry.

### 2026-08-28 — Layer 6 built: make the Update land

Six commits, one per fix, each built, installed and verified on the phone.

- **fix 1** — the match window reaches back **60 s**, not 15 s (the delay runs to 43.9 s).
  The messages list now carries the same window as `start_time`/`end_time`, so a burst in a
  busy chat cannot push the message off the page. The forward edge stays 5 s.
- **fix 2** — the stem match folds case and compares only the first 12 whitespace-free
  characters. Lark's Preview is not a faithful prefix of the Full text.
- **fix 3** — `unwrapHtml` strips the `<p>` markup Lark adds when a sender **edits** a
  message, and a `post`'s `emotion` element reads `[Delighted]` from `emoji_type`.
- **fix 4** — Lark is retried once before ML Kit takes over, and every fallback appends its
  reason to `events.jsonl` (`tools/events` shows `F` rows). The one deterministic failure —
  Lark returning Latin-heavy input unchanged — is `NotTranslated` and is not retried.
- **fix 5** — when a title names no chat, the two most recently used chats are asked. This
  is the only way to reach a reply inside a thread. New `--es debug thread` hook proves it.
- **fix 6** — every chat a truncated title names is asked (up to 5), and only the one that
  answers is cached. The poisoned `【bytedcli MR 处理】...` entry was cleared once.

Verified end to end on the phone after each commit; the debug hook resolved a title that
names no chat through the chat just used, and the ambiguous title asked 5 chats and cached
nothing. Replay says 101 → 119 of 210 Relays (48 % → 56 %).

Next:

- Soak ≥ 1 day and re-grade: `tools/events --since 2026-08-29T00:30 stats`. Watch the new
  `fallback` reasons, and whether `no-chat` and `no-match` fall as the replay predicts.

### 2026-08-28 — tools/probe, tools/chats, and what a spotty wifi showed

- 🛠️ `tools/probe` (post to the test group, wait, print the Relay and the Update; also the
  MainActivity debug hooks) and `tools/chats` (show / reset the chat-id cache). They replace
  the two rituals this session ran most: 28 sends, 37 logcat calls and 19 installs out of
  359 Bash calls, and the reset that `plan.md` had to spell out in prose.
- 🐞 Four bugs found while verifying them, all fixed: a missing phone read as a missing
  cache; `adb logcat` blocks on `- waiting for device -` forever; `--debug` did nothing when
  MainActivity was already running (needs `-f 0x10008000`, the flag the documented command
  had); and `adb exec-out` folds the device's stderr into stdout, so a missing file arrives
  as text and broke `json.loads`.
- 📶 **A delayed push defeats any window.** The phone's wifi went spotty for ~8 minutes;
  Lark then delivered the backed-up Originals at 18:15 for messages sent at 18:11 and 18:13,
  so both were `no-message` — the message is minutes older than `sbn.postTime`. Experiment 11
  measured max 43.9 s on a normal day and no gain past 60 s, so **do not widen the window
  for this**: it only appears when push is delayed, and a wider window costs a worse pick on
  every ordinary Original. Let the next soak say how often it really happens.

### 2026-08-28 — one tool: `tools/larklish`

- 🧰 `tools/events`, `tools/probe`, `tools/chats` and `tools/phone` are now groups of one
  entry point. Each keeps the shape it had, and the views it had become sub-subcommands:
  `tools/larklish events --since … list`, `… phone shade shot.png`, `… chats --clear`.
- 🔐 `tools/lark-token` stays its own file — it is the only piece with dependencies of its
  own (a `uv` script needing `cryptography`). `tools/larklish token` execs it and passes
  arguments through, so there is still one entry point to remember.
- ✅ Equivalence checked, not assumed: `events` `pull`, `stats`, `list`, `table` and the
  filter flags all diff clean against the old tool, as do `chats` and `phone top`. `probe`,
  `probe --debug thread` and `token` were re-run against the phone.
