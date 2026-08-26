# 🧪 Experiment 03 — Relay (Layer 3)

Date: 2026-08-25. Phone: Pixel 4a. Code: `LarkListener` + `Relay.kt` + `Preview` +
`romanize` + `MlKitTranslator`.

## Questions

1. Does the Relay replace the Original: posted, Original canceled, no double?
2. Does the copied `contentIntent` open the right chat?
3. Does the Relay go away when the Original goes away?

## Findings

- ✅ **Replace works (bot DM).** Send → `relayed` in logcat ~5 s later. `dumpsys`: exactly
  one `com.vegerot.larklish` record, zero `com.larksuite.suite` records. The shade shows
  one row: header "Larklish • now", title `Lark`, full English BigText.
- ✅ **Tap works.** `input tap` on the Relay → foreground becomes
  `com.larksuite.suite/com.ss.android.lark.nexus.NexusBotActivity` (the bot chat), and
  the Relay auto-cancels (`FLAG_AUTO_CANCEL`).
- 🐛→✅ **`POST_NOTIFICATIONS` is required.** Without the runtime permission the first
  Relay was dropped silently: `notify()` logs nothing, no record appears. Declared in the
  manifest and granted with `pm grant`. Onboarding must request it.
- 🐛→✅ **`connectedDebugAndroidTest` uninstalls the app afterwards.** That wipes the
  notification-listener grant, the `POST_NOTIFICATIONS` grant, and the ~30 MB ML Kit
  model. Symptom: listener `NOT BOUND`, `stopped=true notLaunched=true`. Fix: re-grant
  (`cmd notification allow_listener` + `pm grant`) and re-download the model.
- 📏 **First relay after a model wipe hangs on the download.** `onNotificationPosted` ran
  but `relayed` never came, because `downloadModelIfNeeded` was fetching the model. The
  debug-hook translation at 22:23 pulled the model in, and every later relay was ~1 s.
- 📏 **Lark did not truncate this test message** (full 61-char Chinese text arrived).
  Truncation (Experiment 01) is variable, not a fixed rule.
- 📏 **ML Kit inverted the message again**: `大家先别发布` ("nobody deploy yet") →
  "everyone is released" — same failure as Benchmark 2 row 26. The Relay plumbing is
  engine-independent; the engine swap is the recorded follow-up.
- ⚠️ ~~The Original's `largeIcon` did not carry over.~~ Correction (same day): it does.
  The collapsed row shows the avatar thumbnail on the right; the expanded BigText view
  hides it. The placeholder droid on the left is our **small icon** (the app-icon task).
- ⏳ **Not yet observed:** a Relay for a real group message, and `onNotificationRemoved`
  mirroring when Lark itself withdraws an Original. Both need real traffic; the listener
  stays installed and logs.

## Commands

```sh
adb shell pm grant com.vegerot.larklish android.permission.POST_NOTIFICATIONS
adb shell cmd notification allow_listener com.vegerot.larklish/.LarkListener
adb logcat -s LarkListener:I
```
