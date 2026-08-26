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
