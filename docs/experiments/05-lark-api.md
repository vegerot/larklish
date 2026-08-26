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

## Soak on real traffic (2026-08-26, 10:29 → 16:06 PDT)

Source: `events.jsonl`, 36 Relays since the Layer 4 build went live. Times in the file
are UTC.

- ✅ **0 fallback rows.** No Relay text starts with `~`: every call reached Lark's engine.
  28 pure-English Previews passed through untouched (Han gate).
- 📏 Removals: `reason=9` ×11 (Lark `cancelAll`), `reason=12` ×4
  (`REASON_GROUP_SUMMARY_CANCELED`: Max swiped Lark's group away on the phone). Both
  mirrored; no stale Relays.
- 📏 **Quality on the 8 Han rows (Claude's grades): 5 ✅ / 3 ⚠️ / 0 ❌.**

| Original | Relay | Grade |
| --- | --- | --- |
| Lizzy Li: 出1.5 k美金，换人民币🙏[FistBump] | Lizzy Li: Sell 1.5k USD for RMB 🙏 [FistBump] | ✅ |
| Mail Assistant: 【Triton】数据权限续期提醒 Data Permissions are going to expire! | Mail Assistant: [Triton] Data access permission renewal reminder Data Permissions are going to expire! | ✅ |
| Shijian Tang: 想请问一下大家，有没有人遇到过类似情况：HR 在系统里修改我的 start date 时操作有误... | Shijian Tang: I would like to ask everyone if anyone has encountered a similar situation: HR made an error when modifying my start date in the system... | ✅ |
| Max Coplan's Feishu CLI: 第四层上线了：飞书翻译优先，ML Kit 兜底 🚀 | …The fourth layer is online: Feishu translation priority, ML Kit fallback 🚀 | ✅ |
| Max Coplan's Feishu CLI: Mac 部署测试：这条通知应该从飞书引擎翻译，不带波浪号 🍎 | …Mac deployment test: This notification should be translated from Feishu engine, without tilde 🍎 | ✅ |
| title 商家经营 / Seller... | Merchant Operating/Seller... | ⚠️ first Han group title through Lark. Readable; 商家经营 = "Merchant Operations"; the spaces around `/` were dropped |
| Yuning Xia: 充电挪个车遇到卧龙凤雏啊：一个用完充电头插回另一个station手搓 deadlock两边都解锁不开... | Yuning Xia: Charging and moving a car encountered a sleeping dragon and a phoenix: one used up the charging head and plugged it back into the other station, rubbing the deadlock on both sides and unable to unlock... | ⚠️ idiom 卧龙凤雏 ("two geniuses", ironic) and 手搓 ("hand-rolled") went literal; gist survives |
| Sender Triton数据安全@you: Data permissions are going to expire! | Triton Shu Ju An Quan@you: Data permissions are going to expire! | ⚠️ not Lark: our `romanize()` turned a bot name into pinyin words. "Triton Data Security" is right (Benchmark 3 row 11) |

### Finding: romanize only person names

`romanize()` is right for bare 2–4-character Han names (陈昱萌 → Chen Yumeng) and wrong
for bot or group Senders that mix Latin and Han (`Triton数据安全`, `Argos平台报警`), where
pinyin carries no meaning. Lark's engine handles those well. Proposal (Max's call): romanize
only bare Han names; send every other Han-containing Sender through the Translator.
