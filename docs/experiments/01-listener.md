# 🧪 Experiment 01 — Listener (Layer 1)

Date: 2026-08-25. Dev machine: GNU+Linux. Phone: Pixel 4a, LineageOS 23.2 (SDK 36).

## Questions

1. Does a sideloaded `NotificationListenerService` receive Lark Originals?
2. Is the text longer than the ~64-char cut seen in `dumpsys`?
3. Does Lark withdraw the Original, and with which reason?

## Setup

- Gradle project: AGP 9.3.2 (built-in Kotlin), Gradle 9.5.1, `minSdk 36`. No Compose yet.
- `LarkListener : NotificationListenerService`, filter `packageName == com.larksuite.suite`,
  log only. First build + install: 17 s.
- Grant from the shell, no Settings UI: `adb shell cmd notification allow_listener
  com.vegerot.larklish/.LarkListener`. The system bound the service at once
  (`dumpsys notification` lists it under "Notification listeners", and logcat shows
  `connected; active Lark notifications: 3`). No "restricted settings" gate appeared.
- Trigger: `lark-cli im +messages-send --as bot --user-id ou_… --text "测试一：…"`.

## Findings

- ✅ **Interception works.** The Original arrived in `onNotificationPosted` ~25 s after the
  `lark-cli` send (most of that is `lark-cli` + Lark server latency).

  ```
  posted key=0|com.larksuite.suite|948177842|null|10229 title=[Lark]
    text=[Max Coplan's Feishu CLI: 测试一：这是 Larklish 第一层实验的中文消息...]
    textLen=54 bigTextSame=true template=android.app.Notification$BigTextStyle
    contentIntent=true flags=0x10 group=null channel=normal_v2
  ```

- ✅ **Lark truncates before it posts.** The listener gets the same `...`-cut string that
  `dumpsys` showed. `bigText == text`. The Relay can never show more than this.
- ✅ **The autogroup summary is a second `posted` event** from the same package:
  `title=null text=null flags=0x20710` (`GROUP_SUMMARY|AUTOGROUP_SUMMARY`), key
  `…|g:Aggregate_AlertingSection`. Layer 3 must skip summaries (`flags and
  FLAG_GROUP_SUMMARY != 0`).
- ✅ **Sender for a bot DM** is the bot's name: `Max Coplan's Feishu CLI: …`. Group
  messages use the person's name (Experiment 00).
- ℹ️ **No withdrawal this time.** The Original stayed for minutes. Lark desktop is not
  running on this machine, which supports the Experiment 00 theory that an active desktop
  makes Lark withdraw the phone's Original. The removal reason is still unknown; Layer 3
  logs it when it happens.
- ✅ `stopped=false` after `adb install`, so the service binds without an Activity launch.

## Commands

```sh
./gradlew installDebug
adb shell cmd notification allow_listener com.vegerot.larklish/.LarkListener
adb logcat -s LarkListener:I
```
