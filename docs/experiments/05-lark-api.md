# 🧪 Experiment 05 — Lark translate API, raw HTTP

Date: 2026-08-26. Run from the Linux machine with `python3 urllib` (no SDK, no `lark-cli`),
so the findings carry to `HttpURLConnection` on the phone. Scripts: `05/exp05.py`
(token, translate, combined call, sequential burst, bad token) and `05/exp05b.py`
(mixed-text probes, missing field, concurrent burst). Run:
`python3 docs/experiments/05/exp05.py <dir>` where `<dir>/app_secret` holds the secret.
They lived in the Experiment 05 commit only; this Layer 4 commit removed them.

## Questions

1. Which endpoints, headers and bodies does the phone need? Which brand host?
2. How does the rate limit behave under a burst? (Experiment 02 hit `99991400`.)
3. Can one call carry title + message? Does a newline survive?
4. What does failure look like (bad token, missing field)?
5. Can the phone reach the host?

## Credentials

The tenant is Feishu-brand (`lark-cli config show` → `"brand": "feishu"`), so the host is
`https://open.feishu.cn`, not `open.larksuite.com` (the phone's Lark client is the
international build; the tenant decides the host). The app is the fat CLI app
`cli_aa949bbb72e39cde`, which already holds `translation:text`. `lark-cli` stores its
secret on Linux as AES-256-GCM at `~/.local/share/lark-cli/appsecret_<app>.enc` with the
key in `master.key` beside it (`internal/keychain/keychain_other.go`); a 6-line
`cryptography` script decrypts it. Max OK'd embedding a key in the APK for now.

## Findings

- ✅ **Token.** `POST /open-apis/auth/v3/tenant_access_token/internal` with
  `{"app_id","app_secret"}` → `{"code":0,"tenant_access_token":"t-…","expire":7200}`.
  766 ms. No header needed.
- ✅ **Translate.** `POST /open-apis/translation/v1/text/translate`, header
  `Authorization: Bearer <token>`, body
  `{"source_language":"zh","target_language":"en","text":…}` → `data.text`. ~350–770 ms.
  `source_language` is required (`99992402 field validation failed` without it).
  Doc (`server-docs/ai/translation-v1/translate.md`): 1,000 chars per call; 20 QPS per
  tenant shared by all its apps; free-edition tenants cannot call it; optional `glossary`
  (≤128 terms, per request).
- ✅ **No burst limit seen.** 8 sequential calls (~500 ms each) and 8 concurrent calls
  (546 ms wall) all returned `code=0`. Experiment 02's `99991400` did not reproduce.
  Two calls per Original (title, message) need no pacing.
- ✅ **Newline survives.** `"【高优】容灾建设专项-2026\nChenglong Song: …需要扩容。"` →
  `"[High priority] disaster recovery construction special project -2026\nChenglong Song: …
  needs to be expanded."`. One call per Original is possible, but two calls are simpler and
  the limit is far away. Not used.
- 🚩 **Latin-heavy text passes through untranslated, deterministically.**
  `Chenglong Song: oec.supply_chain.warehouse_fulfillment 需要扩容。` came back unchanged
  3 of 3 times. Without the Sender (`oec.supply_chain.warehouse_fulfillment 需要扩容。`)
  it translated (`OEC.supply_chain warehouse_fulfillment needs expansion.`); so did
  `Chenglong Song: 需要扩容。`. The engine runs its own language detection on the whole
  string and treats a mostly-Latin string as not-Chinese (this is also Experiment 00's
  "English passes through unchanged"). Consequences: (a) send the message without the
  Sender, which Layer 3 already does; (b) treat "output == input while the input has Han"
  as a failure and fall back to ML Kit.
- ✅ **Quality on the soak rows** (Experiment 04's ML Kit failures):
  | Original | Lark |
  | --- | --- |
  | Zhifu Liu: "申请字节云IAM权限" is still awaiting your action | Zhifu Liu: "Apply for Cloud IAM permission" is still awaiting your action (字节 dropped) |
  | Yifei Cheng: 【直播开启🔛】 生态构建、进化与观测：DeepSeek Harness 技术实践… | Yifei Cheng: [Live 🔛] Ecosystem construction, evolution and observation: DeepSeek Harness technology practice… |
  | 陈昱萌😀: 先别发布，等我看一下 😅 | Chen Yumeng 😀: Don't release it yet, wait for me to take a look 😅 |
  Emoji survive; English inside Han text keeps its case; Chinese names come back romanized
  by the engine (`Chen Yumeng`), and `Zhifu Liu` stayed `Zhifu Liu`.
- ✅ **Errors.** Bad token: HTTP 400, `{"code":99991663,"msg":"Invalid access token…"}`.
  Rule for the app: HTTP ≠ 200 or `code ≠ 0` → exception → fallback.
- ✅ **Phone reaches the host.** `adb shell ping -c 1 open.feishu.cn` → 124 ms.

## Answers

1. Host `open.feishu.cn`; token endpoint + translate endpoint as above; `org.json` +
   `HttpURLConnection` suffice. Cache the token in memory for `expire` seconds.
2. No pacing needed. Fall back on any error.
3. Yes, but not needed.
4. Non-200 / non-zero `code`. Plus the silent pass-through on Latin-heavy input.
5. Yes.

## Design for Layer 4

- `LarkApiTranslator(appId, appSecret)`: in-memory token with expiry; one POST per
  `zhToEn`; throws on any error and on unchanged output.
- `FallbackTranslator(lark, mlKit)`: on exception, use ML Kit and prefix `~` so the Relay
  (and `events.jsonl`) show which engine produced the text.
- Credentials from `local.properties` → `BuildConfig` (never committed).
