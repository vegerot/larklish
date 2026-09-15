# iOS 27 notification automation: Larklish feasibility

Research date: 2026-09-15. Documentation and published experiments only; no
Lark notification has been tested on an iOS 27 device in this investigation.

## Conclusion

An iOS version that reads an Original's Preview and posts an English notification
is now a credible project. The notification automation is available on iOS 27,
not just macOS 27. It supplies the missing event and Preview input.

The documented mechanism does **not** give us Android's notification ownership:
we cannot promise to cancel Lark's Original, inherit its tap action, or withdraw
our English notification when Lark withdraws the Original. Start by testing
Preview translation with both notifications present.

This is a feasibility finding, not a new implementation layer or a claim of
production reliability. Existing Android and deployment decisions remain in
`plan.md` and `progress.md`.

## What the evidence establishes

### Apple documentation

- [WWDC26: What's new in Shortcuts](https://developer.apple.com/videos/play/wwdc2026/310/),
  0:57–3:25: the notification automation runs on receipt from a selected app and
  supports filtering by notification text. Apple demonstrates a delivery
  notification triggering home actions. This is a system Shortcuts feature;
  the sending app need not implement a custom Shortcuts action for that trigger.
- Apple's current [iOS 27 release notes](https://developer.apple.com/documentation/ios-ipados-release-notes/ios-ipados-27-release-notes)
  and [macOS 27 release notes](https://developer.apple.com/documentation/macos-release-notes/macos-27-release-notes)
  contain Shortcuts fixes and remaining issues. Neither retrieved Shortcuts
  section explicitly documents the notification-input fix or guarantees delivery.
- [Show Notification](https://support.apple.com/en-euro/guide/shortcuts/apd2175adcab/ios)
  creates a system notification without pausing the shortcut for an alert dialog.
- [UNUserNotificationCenter](https://developer.apple.com/documentation/usernotifications/unusernotificationcenter)
  manages an app's own notifications. Its delivered-notification queries and
  removal methods are explicitly scoped to that app.
- [UNNotificationServiceExtension](https://developer.apple.com/documentation/usernotifications/unnotificationserviceextension)
  modifies appropriately configured remote notifications for the app containing
  the extension. Putting an extension in Larklish would not intercept Lark's pushes.
- [AppIntent.supportedModes](https://developer.apple.com/documentation/appintents/appintent/supportedmodes)
  supports running an action entirely in the background. This is the integration
  point for a future native Larklish action invoked by the user's automation.
- [TranslationSession](https://developer.apple.com/documentation/translation/translationsession)
  processes translations on-device. Its
  [installed-language initializer](https://developer.apple.com/documentation/translation/translationsession/init(installedsource:target:))
  supports contexts without UI, requires an explicit source language, and needs
  both language packs installed. The initializer dates to iOS 26, so it is
  available for an iOS 27 implementation.
- [Translate settings](https://support.apple.com/en-au/guide/iphone/iphddb6e7264/ios)
  document language downloads and On-Device Mode. This alone does **not** prove
  how the built-in Shortcuts Translate Text action behaves on the target build;
  test that separately before claiming a shortcut-only solution stays offline.

Documentation quality matters: the WWDC26 page's chapter summary says “iOS 26”
despite the new automation discussion; contemporary iOS 27 demonstrations establish
the version here. Several Apple Support automation pages still describe the old
editor and trigger list. Search snippets for the release notes also lagged behind
the current documents. The release notes and Translation API were read from
Apple's `.md` versions after the HTML pages returned JavaScript shells.

### Published hands-on evidence

| Source | Observation and limit |
| --- | --- |
| [Derek Seaman, June 12](https://www.derekseaman.com/2026/06/home-assistant-notifications-that-run-apple-shortcuts-yes-really.html) | Tested notification automation on iPadOS 27 developer beta 1. Unfiltered receipt worked, but text filtering was broken. His macOS/iOS statements here included extrapolation from iPad. |
| [Shortcuts beta megathread, June 16](https://www.reddit.com/r/shortcuts/comments/1u720hi/whats_new_in_shortcuts_in_ios_27_beta_megathread/) | Early testers reported missing title/body input and broken filters. These are historical beta results. |
| [iOS developer beta 5 parsing report](https://www.reddit.com/r/iOSBeta/comments/1vksp7d/ios_27_db_5_shortcuts_notification_parsing_fixed/) | Testers report title/body extraction and filters working. Tap the Notification variable to select its properties. One commenter still reported missed triggers. |
| [Derek Seaman, August 10; updated August 30](https://www.derekseaman.com/2026/08/ios-27-flips-the-script-home-assistant-can-now-control-your-apple-devices.html) | Working iPhone automation filters notification text and changes Focus while locked. Calls for developer beta 5 or later. This proves that workflow, not locked Lark translation. |
| [Expense-tracker author, August 12](https://www.reddit.com/r/shortcuts/comments/1vme6ho/ios_27_beta_finally_lets_my_expense_tracker_run/) | Describes passing banking notification input into an app with Run Immediately on beta 5. Replies include success and empty-input failures, even on beta 5. |
| [WalletPal's integration guide](https://walletpalapp.github.io/apple-shortcuts-notification-trigger) | Shows Title, Subtitle, and Body selected from Shortcut Input and passed to separate app-action parameters. Strong evidence that text is usable downstream, beyond trigger filtering. |
| [Beard.fm walkthrough](https://wiki.beard.fm/how-tos/how-to-create-automations-that-trigger-from-app-notification) | Demonstrates selecting an app and Title/Subtitle/Message filters, then music playback and HomePod handoff. |
| [Mac beta discussion](https://www.reddit.com/r/MacOSBeta/comments/1u6x0rw/does_macos_27_include_the_notification_trigger_in/) | Early platform-availability discussion; weaker than the concrete iPhone demonstrations for this decision. |

The distinction is crucial: “a notification can trigger a shortcut” alone would
not establish translation feasibility. Downstream access to the Preview is the
decisive new capability. The early missing-input reports are not evidence of a
permanent platform prohibition, and later successes are not proof of universal
reliability.

## Mapping to Larklish

| Requirement | iOS assessment |
| --- | --- |
| React to an Original | Supported mechanism: user configures Notification automation for Lark, Run Immediately. No Mac is needed in the runtime path. |
| Read Preview | Title, Subtitle, and Body are exposed by the trigger. Lark's specific field layout and redaction remain untested. Do not assume Android's `Sender: message` layout. |
| Translate Preview | Smallest candidate: Translate Text action, after verifying its provider/offline behavior. Native route: background App Intent using Apple's on-device TranslationSession. Translation quality needs the same real-Preview evaluation as Android. |
| Post English notification | Show Notification for a prototype; UNUserNotificationCenter for a native app with its own notification identity. |
| Suppress or edit Original | No supported cross-app mechanism found. Own-app notification APIs and service extensions do not provide this. |
| Preserve original tap target | No documented trigger field for Lark's private navigation payload or action. A future app could use a separately resolved chat link; this needs iOS verification. |
| Mirror withdrawal/read state | No notification-removal trigger or cross-app delivered-notification query was established. Do not promise this behavior. |
| Full text and Update | The trigger supplies a Preview, not arbitrary Full text. Existing Backend Lookup is potentially reusable, subject to iOS Preview matching, timestamps, authentication, network reachability, and measured runtime. |
| Run while locked | Demonstrated for other workflows. Verify Lark input and the entire chosen translation action while locked. |

The current [Backend contract](../../backend/server.go) already accepts
`{title, text, whenMs, userToken}` and returns Full text, English, and chat ID.
Requests also require the Backend's shared token in an `Authorization: Bearer`
header, separately from the Lark user token in the request body.
It does not expose a standalone Preview-translation route. Reusing Lookup is a
later extension; it does not restore Android notification ownership. An automation's
execution time is also not automatically equivalent to Android's `sbn.postTime`.

## Smallest useful experiment

1. On the target iPhone running iOS 27, create a Lark Notification automation with
   Run Immediately. Start without text filters.
2. First echo Title, Subtitle, and Body into Show Notification with clear labels.
   Observe an Original while unlocked, then while locked. Record the OS build,
   Lark settings, received fields, and delay. This isolates input access from
   translation failures.
3. Add translation to English and display its output with Show Notification.
   Preserve the Sender separately once the iOS Preview layout is known. Test a
   human group message, a direct message, mixed English/Chinese, and a long Preview.
4. Test downloaded-language translation offline with a fixed sample independently
   of notification delivery. Then test the complete workflow while locked online.
   Confirm the chosen translation action and its data handling on that build.
5. Try Lark's notification presentation settings one at a time: previews hidden,
   sound off, and Notification Center delivery without banners. Check Focus and
   whether desktop activity suppresses mobile delivery. Do not assume turning
   Allow Notifications off preserves the trigger.

If this passes, a small native app can expose one background action accepting
the Preview, translate it, and own the English notifications. That gives us a
place for grouping, duplicate handling, and eventual Lookup. First prove the
basic workflow; no need to port the Android listener or build a Backend change
before that experiment.

## Scope of the review

Searched Apple documentation, WWDC, release notes, developer discussions, and
community posts through the research date. Followed the concrete implementations
and contradictory beta reports listed above. This is a broad review of relevant
public material, not a claim to have read every internet post. No published
Lark-specific iOS 27 test was found. No phone settings, automation, app, or Backend
were changed by this research.
