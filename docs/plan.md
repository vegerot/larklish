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
| Lark translation API | Works via `lark-cli api --as bot` (scope `translation:text` granted); English input passes through unchanged | `docs/experiments/00-adb.md` |
| Pixel Live Translate | Not available (Tensor-only, and this is Lineage) | — |
| Interception | A sideloaded listener receives Originals; `allow_listener` binds it at once, no restricted-settings gate | `docs/experiments/01-listener.md` |
| Autogroup summary | Arrives as a second `posted` event with null title/text; Layer 3 skips `FLAG_GROUP_SUMMARY` | same |
| Withdrawal | Did not happen with Lark desktop off; desktop-active is the likely trigger | same |

## Layers

### Layer 0 — adb only ✅ done

See `docs/experiments/00-adb.md`.

### Layer 1 — Listener ✅ done

See `docs/experiments/01-listener.md`. Gradle project (AGP 9.3.2, built-in Kotlin,
`minSdk 36`), `LarkListener` logs every Lark Original. Interception works.

### Layer 2 — Translator screen (is on-device quality good enough?)

- Compose screen: text field → button → English output. Same app, same package.
- `Translator` interface with one method; `MlKitTranslator` is the first implementation.
- ML Kit `com.google.mlkit:translate:17.0.3`, `TranslateLanguage.CHINESE` → `ENGLISH`.
  Download the ~30 MB model on first use.
- Non-Chinese input: first test what ML Kit does with English text. Add language ID
  (`com.google.mlkit:language-id`) only if the output is bad. Keep it simple.
- Evidence: `docs/experiments/02-quality.md`, a table of ~10 real previews:
  Chinese → ML Kit → Lark's own translation. Get Lark's translation with
  `lark-cli api POST /open-apis/translation/v1/text/translate --as bot` (scope granted).
  Note: on 2026-08-25 the API answered `99991400 request trigger frequency limit` to
  single calls. Retry later; space the ~10 calls out.
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

Dev machine: GNU+Linux. SDK at `~/Android/Sdk` (has `platforms/android-36`,
`build-tools/36.0.0`, `platform-tools`). No `sdkmanager` installed; none needed so far.

```sh
echo "sdk.dir=$HOME/Android/Sdk" > local.properties       # once; Gradle finds the SDK
./gradlew installDebug
adb shell cmd notification allow_listener com.vegerot.larklish/.LarkListener
adb logcat --pid="$(adb shell pidof com.vegerot.larklish)"
lark-cli im +messages-send --as bot \
  --user-id ou_4d4c9ea3307c313a134ac3ea0821935e --text "中文"   # trigger an Original
```
