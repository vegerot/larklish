# Larklish — Project Handoff

Context transfer from a planning conversation. Everything under **Settled
Decisions** was argued through already — avoid re-proposing the rejected
alternatives listed there unless the situation changes and an alternative
becomes more attractive.

---

## The problem

I work at a Chinese company (ByteDance) and only speak English. The company chat tool is
Lark (Feishu in China; roughly Slack). Lark translates messages **inside the app** but not
in **notification previews**, which makes the notification shade useless to me.

I have three tiers of per-group notification settings in Lark:

1. A few groups notify on **all messages**
2. Most groups notify on **@mentions only**
3. Some groups are **fully blocked** (spammy bots that always @ me)

Any solution must respect those tiers.

---

## Settled decisions

| Decision | Value |
| --- | --- |
| **Name** | Larklish |
| **Package** | `com.vegerot.larklish` |
| **Chinese name** (optional, README/store) | 译鸟 ("translation bird") |
| **Platform** | Android |
| **Core mechanism** | `NotificationListenerService` |
| **v1 translation** | ML Kit on-device zh→en (~30 MB offline model, free) |
| **Extensibility** | Translation behind a one-method interface |

### Architecture

```
Lark posts notification
  → NotificationListenerService receives it (downstream of Lark's own rules engine)
  → filter to Lark's package
  → extract preview text
  → ML Kit zh→en, on-device
  → post replacement notification, copying the original's contentIntent + actions
  → cancelNotification() on the original so there are no doubles
```

### Why this architecture won

The listener sits **downstream** of Lark's notification rules. By the time a notification
reaches the status bar, Lark has already applied mutes, @mention-only settings, and bot
blocks. So all three tiers are inherited for free, stay in sync when I change them in-app,
and require **no API scopes and no ByteDance tenant approval**.

### Translator interface

```kotlin
interface Translator {
    suspend fun zhToEn(text: String): String
}
```

`MlKitTranslator` is v1. `LarkApiTranslator` / `VolcanoTranslator` sit behind a setting if
on-device quality disappoints (see Cloud fallbacks below).

---

## Rejected alternatives — avoid re-proposing

**Polling via lark-cli under my user identity.** Investigated in depth by a research agent.
Blockers:

- The only relevant API (`chat_user_setting/batch_query`) exposes **two booleans**
  (muted, @all-muted) against my **three tiers**. The mapping is unconfirmed.
- Worse: @you mentions break through mute, so tier 3 (blocked spam bots that always @ me)
  likely has **no encoding at all** — polling would notify me most loudly for exactly the
  chats I silenced.
- No unread counts, no badge state, no push for user identities. Real-time message events
  exist only for bots.
- User-token scopes for reading my own messages go through ByteDance security review and
  are reportedly effectively blocked for personal tools.
  - **Update 2026-08-25:** false for my own CLI app. `lark-cli auth login` granted
    `search:message` and the `im:message.*_msg:get_as_user` scopes with one click. The
    other blockers above still stand. See `docs/experiments/02-quality.md`.
- The real notification engine (true three-tier settings, unread counts) lives in an
  internal Lark service that is client-only RPC.

**Lark bot with event subscriptions.** Bot must be added to every group to see it; needs
tenant approval; sees the raw firehose and forces reimplementing my filtering as a shadow
copy of my rules that silently drifts.

**Third-party cloud translation (Google, DeepL).** Piping work chat to a third party is
exactly what corporate DLP policies target.

**Tasker + AutoNotification.** Fine for an evening prototype of the UX, not the build.

**Desktop D-Bus tap / browser extension.** Wrong surface — the problem is my phone.

**Pixel Live Translate.** Worth a five-minute check that it doesn't already solve this,
but not a thing to build on.

---

## Implementation plan (proposed — not yet reviewed)

1. **Manifest + service.** Declare the service with
   `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` and the
   `android.service.notification.NotificationListenerService` intent filter.
2. **Onboarding flow.** Notification access can't be granted by a normal permission
   dialog — deep-link to `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` and check
   enablement on resume.
3. **Package filter.** Verify Lark's actual package name on-device before hardcoding
   (Feishu and international Lark may differ). **TODO: confirm.**
4. **Text extraction.** Read `EXTRA_TITLE` / `EXTRA_TEXT`, and handle `MessagingStyle`
   (`EXTRA_MESSAGES`) for group chats, which Lark likely uses. Inspect a real
   `StatusBarNotification` before committing to a parser.
5. **Language detection.** Use ML Kit language ID to skip translation on messages that are
   already English — avoids mangling English text and saves work.
6. **Translation.** Download the zh model on first launch (Wi-Fi-only condition), handle
   the not-yet-downloaded state gracefully.
7. **Re-post.** Own notification channel; copy the original's `contentIntent` so tapping
   still opens the right chat; preserve actions and the inline-reply `RemoteInput`; then
   `cancelNotification(key)` on the original.
8. **Loop guard.** Ignore notifications from our own package.

---

## Open questions and risks

- Does Lark use `MessagingStyle`, or plain title/text? Determines the extraction path.
- Does copying `contentIntent` reliably deep-link to the correct chat?
- Preview text is short and context-light — machine translation quality may be gist-level.
  Acceptable escape hatch: tap through to Lark, where in-app auto-translate gives the good
  translation anyway.
- Notification grouping/summary notifications may need separate handling.

---

## Cloud fallbacks (only if on-device quality is genuinely annoying)

**Lark Open Platform Translation API** — `POST /open-apis/translation/v1/text/translate`
(plus a language-detect sibling). Same engine as the in-app translate, so notification
translations would match what I see when I open the chat. Crucially this needs only the
**translation scope**, not message-reading scopes — a much easier approval ask.

**Volcano Engine Machine Translation (火山翻译)** — ByteDance's MT service at
`translate.volcengineapi.com`, AK/SK auth, 55 languages, zh↔en is its home turf. Requires
real-name verification (China-region signup friction); check BytePlus for the
international equivalent. Pay-as-you-go past the free quota.

Both keep the data inside ByteDance infrastructure, which is a far easier compliance story
than a third party.

---

## First task

Scaffold the listener: manifest and service declaration, the notification-access
permission flow, `MessagingStyle` text extraction, and the ML Kit model download on first
launch.
