# 🗺️ Larklish — Build plan

Larklish grows like an onion 🧅. Each layer is the smallest thing that answers one
question. Commit every layer, even the ones we throw away. See `larklish-handoff.md` for
the settled decisions and `CONTEXT.md` for the words.

## Settled by the experiments so far

| Question | Answer | Evidence |
| --- | --- | --- |
| Lark package | `com.larksuite.suite` | `pm list packages` |
| Original shape | `BigTextStyle`; title = group, text = `Sender: message`; no actions | `docs/experiments/00-adb.md` |
| Grouping | Lark sets none; Android auto-groups per package | same |
| Preview length | Lark truncates at ~64 chars | same |
| Listener grant | `cmd notification allow_listener` — no Settings UI needed | `cmd notification` help |
| Lark withdraws Originals | Yes, on its own (desktop active). Relay must follow. | same |
| Lark translation API | Needs scope `translation:text`; tenant token; 1000 chars; 20 QPS | `lark-cli api` error, docs |
| Pixel Live Translate | Not available (Tensor-only, and this is Lineage) | — |

## Layers

### Layer 0 — adb only ✅ done

See `docs/experiments/00-adb.md`.

### Layer 1 — Listener (is interception possible?)

- Scaffold one Gradle project, package `com.vegerot.larklish`, Kotlin, Compose, `minSdk 36`.
- `LarkListener : NotificationListenerService`. Filter `packageName == com.larksuite.suite`.
- `onNotificationPosted`: log key, title, text, bigText, text length, `contentIntent != null`.
- `onNotificationRemoved`: log key and `reason` — this shows how Lark withdraws Originals.
- Grant: `adb shell cmd notification allow_listener com.vegerot.larklish/.LarkListener`.
- Trigger: `lark-cli im +messages-send --as bot --user-id ou_… --text "中文"`.
- Pass: logcat shows the Original within seconds. Also learn: is the text longer than the
  64-char `dumpsys` cut? Which removal reason does Lark use?
- No UI yet. Observe with `adb logcat --pid=$(adb shell pidof com.vegerot.larklish)`.

### Layer 2 — Translator screen (is on-device quality good enough?)

- Compose screen: text field → button → English output. Same app, same package.
- `Translator` interface with one method; `MlKitTranslator` is the first implementation.
- ML Kit `com.google.mlkit:translate:17.0.3`, `TranslateLanguage.CHINESE` → `ENGLISH`.
  Download the ~30 MB model on first use.
- Non-Chinese input: first test what ML Kit does with English text. Add language ID
  (`com.google.mlkit:language-id`) only if the output is bad. Keep it simple.
- Evidence: `docs/experiments/02-quality.md`, a table of ~10 real previews:
  Chinese → ML Kit → Lark's own translation. Get Lark's translation from the API once the
  `translation:text` scope exists, else by hand in the app.
- Pass: Max judges the ML Kit column "gist OK".

### Layer 3 — Relay (does the replacement work?)

- Own channel. Post the Relay with the Original's `contentIntent` and `largeIcon`,
  title and text translated. Then `cancelNotification(original.key)`.
- When the Original is removed by Lark (`onNotificationRemoved`, reason ≠ our own
  cancel), cancel the Relay too.
- Loop guard: ignore `com.vegerot.larklish`.
- Test by hand: `lark-cli` bot message → Relay appears, Original gone, no double, tap
  opens the right chat. Screenshots via `adb exec-out screencap`.
- Pass: all four hold for a group message and for a bot DM.

### Later (not experiments)

- Onboarding: deep-link to `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`; note the
  Android 13+ "Allow restricted settings" step for sideloaded apps.
- Debug UI: list of recent Original → Relay pairs.
- `LarkApiTranslator` behind a setting, if ML Kit quality disappoints.

## Commands

```sh
brew install --cask android-commandlinetools           # SDK, once
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
sdkmanager --install "platforms;android-36" "build-tools;36.0.0" "platform-tools"
./gradlew installDebug
adb shell cmd notification allow_listener com.vegerot.larklish/.LarkListener
adb logcat --pid="$(adb shell pidof com.vegerot.larklish)"
```
