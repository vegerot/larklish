# Experiment 35 — 22-day soak analysis, Full-text English fix, URL wrapper, and ByteCloud PPE switch

Date: 2026-10-20. Analyzed the 22-day Recorder cohort (`2026-09-28 17:00` → `2026-10-20 14:19` PDT)
from the Pixel 4a, diagnosed the October 16 Railway outage and the `not-translated` Full-text
skips, shipped the ML Kit URL-preservation wrapper and the Full-text unchanged-English pass-through
fix (`354bf0b`), and switched the phone and Backend deployment to ByteCloud PPE (`ppe_deploy_i18n_1`).

## 1. 22-day soak summary (`2026-09-28` → `2026-10-20`)

Excluding the test group (`Larklish 测试群`), the Recorder captured `774` Relays over 22 days:

| Category | Count | Notes |
| --- | ---: | --- |
| Total Relays | 774 | `356` cut Previews (`...`), `418` complete Previews |
| Local-only complete Previews (`complete-no-han`) | 263 | No Han in message body; early Relay is final |
| Backend Updates (`updated`) | 326 | `219` Full-text (`170` `post`, `49` `text`), `107` `preview` |
| Non-updated Backend calls | 185 | Detailed breakdown below |

For updated cut Previews, Relay-to-Update wall-clock latency had a median of **`3.002 s`** and a
nearest-rank `p90` of **`5.196 s`**.

### Breakdown of the `185` non-updated Backend calls

| Outcome | Count | Root cause |
| --- | ---: | --- |
| `skipped error` | 61 | `58` were `HTTP 404` after `2026-10-16 14:57 PDT` when the Railway trial expired (`"Your trial has expired. Please select a plan to continue using Railway"`); `3` prior to October 16 were transient phone Wi-Fi disconnects (`Software caused connection abort`, `Unable to resolve host`). |
| `skipped no-chat` | 43 | `32` `[Lark]` system/bot notifications (Mail Assistant, Feishu ESOP, Compliance Governance Assistant, meeting-started alerts, Approval, Kani) and `11` `[Reminder]` calendar notifications whose title is the meeting title rather than a group chat. |
| `skipped type:interactive` / `type:general_calendar` | 23 | `22` interactive card messages and `1` calendar share (expected unsupported message types). |
| `skipped translation-failed:not-translated` | 22 | `20` were cut Previews (`source: full-text`) where Lookup found the full message in Lark, `hasHan(message)` was `true` because of a localized `@mention` (e.g. `@Oncall 助手`), a Chinese emoji tag (`[送心]`, `[派对]`, `[挥手]`), or a short Chinese phrase inside a predominantly English post, and Lark's translation API returned the message unchanged (`translated == text`). `2` were complete Previews (`source: preview`). |
| `skipped no-message` | 1 | A cut notification Preview had an English-localized leading mention (`@Stargazer Assistant`) and omitted a trailing `[Thinks]` emoji, whereas the Backend's bot token received `@星舟 助手` plus `[Thinks]` at the end of the body. Because `stripLeadingMentions` in `backend/pick.go` only stripped leading `@mention` tokens when the remaining suffix matched, the trailing emoji difference prevented a match. Left untouched (1 instance in 22 days). |
| `canceled` | 35 | Superseded by a newer notification on the same key or withdrawn before the Backend call completed. |

## 2. Fixes shipped (`354bf0b`)

### Full-text unchanged-English pass-through (`backend/server.go`, `backend/server_test.go`)

Previously, `backend/server.go` treated `messageTranslation.Failure != ""` identically for both
`source == "preview"` and `source == "full-text"`: when Lark's `/open-apis/translation/v1/text/translate`
returned `not-translated` (input unchanged), the Backend discarded the resolved Full text and
returned `Outcome: "skipped"`, `Reason: "translation-failed:not-translated"`. For a cut Preview,
that left the phone displaying the truncated 45-character Preview even though the Backend had
already fetched the complete English message.

With the fix:
- When `source == "full-text"` and `messageTranslation.Failure == "not-translated"`, the Backend
  sets `english = &message`, records `{"field": "message", "reason": "not-translated"}` in
  `Failures`, and proceeds to return `Outcome: "found"`.
- When `source == "preview"` and `messageTranslation.Failure == "not-translated"`, the Backend
  still returns `Outcome: "skipped"` so the phone retains its early ML Kit Relay.

### URL preservation in on-device ML Kit translation (`Translator.kt`, `UrlTranslationTest.kt`)

Following Experiment 34, `Translator.englishOf` masks `https?://[^\s\p{IsHan}<>]+` substrings with
numbered `URL0`, `URL1`, ... placeholders (skipping any `URLn` already present in the input) before
calling `zhToEn`, and restores the exact URLs afterward. If ML Kit drops or duplicates a marker,
`englishOf` falls back to translating the unmasked text rather than substituting a wrong URL.
Verified with unit tests (`./gradlew testDebugUnitTest`) and on-device instrumentation tests
(`UrlTranslationTest`).

### Instrumentation test gotcha (`connectedDebugAndroidTest`)

Running `./gradlew connectedDebugAndroidTest` installs both `app-debug.apk` and
`app-debug-androidTest.apk`, runs the tests, and then **uninstalls both APKs** when finished,
wiping `/data/data/com.vegerot.larklish/` (`files/user-token.json`, `files/events.jsonl`, and
`shared_prefs/backend.xml`). Always back up `/data/data/com.vegerot.larklish/files/` before running
`connectedDebugAndroidTest`, then reinstall `app-debug.apk`, re-bind `LarkListener`, and restore or
re-seed `user-token.json`.

## 3. ByteCloud PPE deployment and phone cutover

- **SCM build**: Pushing `354bf0b` to `code.byted.org/max.coplan/larklish` (`main`) built SCM
  artifact `oec/seller/larklish:1.0.0.39` (`编译成功`).
- **FaaS & Bits state**:
  - Earlier PPE function `t648e7x4` had been garbage-collected by ByteFaaS after weeks of
    inactivity (`404 function_not_found`).
  - Production service `kpb2dvsn` (`coplan.lark.larklish` in `sg` / `faas-sg`) remains healthy on
    `1.0.0.20` (`1.0.2`), which predates `/lookup`. We created FaaS SCM revision `1.0.3`
    (`tlpcjxmx7n`) for `oec/seller/larklish:1.0.0.39`, but direct CLI release on `kpb2dvsn` is
    blocked by the OEC ByteCycle freeze policy (`国际化电商_Faas未走bytecycle平台发布拦截策略_20221118`).
  - Created Bits PPE task **`2950156`** (`Larklish PPE Backend 1.0.0.39`, cloned from `2888453` on
    lane `ppe_deploy_i18n_1` with `scm=oec/seller/larklish,version=1.0.0.39`) and ran
    `bytedcli --site i18n-tt bits develop quick-run --dev-id 2950156 --control-planes CONTROL_PLANE_I18N --wait`
    to `SUCCEEDED` (build `1244950546178` / `1244960923650`).
- **Phone verification**:
  - Seeded a fresh user refresh token from `lark-cli` (`uv run --script tools/larklish-helper token --write`),
    installed `app-debug.apk`, bound `LarkListener`, and verified `debug user` (`ou_4d4c9ea3307c313a134ac3ea0821935e`).
  - Removed `shared_prefs/backend.xml` so the phone uses `BackendSettings.DEFAULT_URL`
    (`https://shop.tiktokglobalshop.com/_/test/demo/larklish`) with `BuildConfig.LARKLISH_BACKEND_TT_ENV = "ppe_deploy_i18n_1"`.
  - Verified complete-Preview translation from the phone via `debug fetch` (`title="Larklish 测试群"`,
    `text="Bot: 中文消息"`): `Lookup(outcome=found, source=preview, english=Chinese message, title=Larklish test group)`.
  - Verified cut-Preview Full-text Lookup from the phone via `debug fetch` (`title="Larklish 测试群"`,
    `text="..."`): authenticated user-token `GET /open-apis/im/v1/chats/search` (`200`) and
    app-token `GET /open-apis/im/v1/messages` (`200`).
