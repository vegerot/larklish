# 🧪 Experiment 00 — adb only, no app

Date: 2026-08-25. Phone: Pixel 4a, LineageOS 23.2 (Android 16, SDK 36), rooted.
Lark package: `com.larksuite.suite`.

## Questions

1. What shape does a Lark Original have? (`MessagingStyle` or title/text?)
2. Can the system see Lark posts in real time?
3. Can the shell post a Relay-shaped notification, and how does Android group it?
4. Can a bot message from `lark-cli` create an Original on demand?

## Commands

```sh
adb shell dumpsys notification --noredact
adb shell "cmd notification post -S bigtext -t 'Group name' larklish 'Sender: text'"
adb shell cmd notification allow_listener <component>   # grants listener access, no Settings UI
lark-cli im +messages-send --as bot --user-id ou_… --text "中文"
```

## Findings

- ✅ Every Lark Original is `BigTextStyle`. `android.title` = group name (or `Lark` for
  bots and calendar). `android.text` = `android.bigText` = `Sender: message`.
  Mentions look like `Sender@you: …` and `Sender@all: …`.
- ✅ No actions. No `RemoteInput`. One `contentIntent` (startActivity into Lark).
- ✅ Lark sets no group. Android auto-groups (`AUTOGROUP_SUMMARY`). Channel `normal_v2`
  "Regular Messages", importance 4, `AUTO_CANCEL`, `vis=PRIVATE`, `largeIcon` bitmap 119×119.
- ✅ Lark truncates long text at ~64 chars with `...` (`--noredact` prints full strings, so
  the cut is Lark's). Short texts are complete.
- ✅ `cmd notification post` works. A shell post joins the shell package's own autogroup,
  not Lark's. So a Relay from `com.vegerot.larklish` will sit in its own group. There is
  no `cmd notification cancel`; a listener must cancel.
- ✅ A bot DM via `lark-cli` created a Lark Original within ~5 s of sending.
- ⚠️ Lark **canceled that Original itself** ~5 s later (`NotifAttentionHelper: No
  vibration for canceled notification`). The message was still unread. Lark desktop was
  active on the Mac, which is the likely cause. Design consequence: when an Original goes
  away, the Relay must go away too (`onNotificationRemoved`).
- ✅ `lark-cli api POST /open-apis/translation/v1/text/translate --as bot` works after Max
  added the `translation:text` scope in the developer console. Input
  `哈喽@Jiarui Chen @Yufan Chen ， 我发现 oec.seller.assistant_api 有个问题` → output
  `Hello @Jiarui Chen @Yufan Chen, I found a problem with oec.seller.assistant_api`.
  Mentions and identifiers survive. This is the quality bar for ML Kit in Layer 2.
- ✅ `.../text/detect` returns `zh` / `en` correctly. Translate with `source_language=zh`
  on English input (`Bits@you: Sync Task Conflicted`) returns it **unchanged**. So the
  cloud path needs no language-ID code. Layer 2 tests whether ML Kit does the same.
- ✅ Layer 0 needs no Settings UI for the listener grant: `cmd notification allow_listener`.

## Raw dump excerpt (one Original)

```
NotificationRecord(pkg=com.larksuite.suite id=1797328819 importance=4
  Notification(channel=normal_v2 flags=AUTO_CANCEL vis=PRIVATE))
  contentIntent=PendingIntent{… com.larksuite.suite startActivity …}
  extras={
    android.title=String (Seller Center Oncall...)
    android.text=String (Yinghao Li: 哈喽@Jiarui Chen @Yufan Chen ， 我发现 oec.seller.assistant_api...)
    android.bigText=String (<same as text>)
    android.template=String (android.app.Notification$BigTextStyle)
    android.largeIcon=Icon (Icon(typ=BITMAP size=119x119))
    android.showWhen=Boolean (true)
  }
```

Other live titles/texts seen: `San Jose 1199...` / `Daniel Chengyong: Hi everyone! …`,
`Lark` / `Bits@you: Sync Task Conflicted`, `US Global E-Commerce` / `Rachel Lam@all: …`,
`Lark` / `Calendar Assistant: Rachel Lam's invitation`.
