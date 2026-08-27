# 🧪 Experiment 08 — Soak 2026-08-26 → 27: Layer 4 for a day, Layer 5's first hours

Source: `events.jsonl` (182 events, pulled 2026-08-27 15:56 PDT), read with
`tools/events --file … --since 2026-08-26T17:29` (the Layer 4 build went live 10:29 PDT).
101 Relays since then; **73 are real traffic**, 28 are our probes (`Feishu CLI`, `测试群`).
The first 36 Relays were graded in `05-lark-api.md` "Soak"; this file covers the whole day
and what the Relay record says about the traffic itself.

## Shape of a day of Lark notifications (73 real Relays)

| Measure | Value | So what |
| --- | --- | --- |
| Previews that end in `...` | **39 / 73 = 53 %** | Layer 5 (Full text) matters for every second notification |
| Empty Previews (`Sender:...`) | **0** | the "no boundary in 45 chars" case is real (E5) but rare in practice |
| Title `Lark` (bot DMs, Approval, Bits, reactions) | 17 / 73 = 23 % | the DM path (commit 6) carries a quarter of the traffic |
| One group (`San Jose 1199...`) | 33 / 73 = 45 % | office chatter dominates; its messages are English + `post`s |
| Han in text / Han in title | 15 / 4 | most traffic is English; the Han gate passes 58 rows untouched |
| `@you` mentions | 7 | the mention suffix parse gets real use |
| Same-key re-posts < 10 s apart | 3 (4 %) | the Q15 race is real; per-key cancellation stays |
| Withdrawal latency (43 pairs) | min 8 s, **median 11.5 min**, p90 90 min, max 221 min | when Max is away the Relay lives for hours; it *is* what Max reads |
| Silent gap | 21:13 → 09:30 PDT, then 3 Originals at 09:30 sharp | overnight quiet (phone DND / Lark schedule?), not a crash — the same process relayed at 09:30 |
| Message types in the busiest group (12 newest via the API) | 6 `text`, **4 `post`**, 1 `image`, 1 `sticker` | `post` is common, and it looks like text — see below |

Previews **can span two lines**: `Yubao Guo: TTP 故障应急最新信息同步：\n为缓解TTP2 …` and the
`Zhifu Liu@you` row both carry a `\n` inside the 45-character budget. E5's "newline ends
the Preview" held for our probes only. The Layer 5 stem must therefore ignore whitespace
when it compares.

## `post` is the everyday message type

Both real Originals after Layer 5 went live were skipped as `type:post` — including
`Stella Peng: 想求一两颗泰诺🥹🥹`, a plain sentence typed in the mobile editor. The API's
`body.content` for a `post`:

```json
{"title": "", "content": [[{"tag": "text", "text": "想求一两颗泰诺🥹🥹", "style": []}]], "content_v2": …}
{"title": "", "content": [[{"tag": "img", "image_key": "img_v3_…", "width": 1080, "height": 1440}],
                          [{"tag": "text", "text": "Cool performance by Sofia Alise …", "style": []}]]}
```

Paragraphs of tagged elements; the text is right there. Lark's own Preview flattens it the
same way (`[image]Cool performance by …`). Per Q6's rule ("if Post looks similar, add it in
another commit right away") → commit 7 flattens `post`s: `text`/`a` → text, `at` →
`@user_name`, `img` → `[image]`, paragraphs joined by newlines, `title` first.

## Quality: the 10 real Han rows after the first write-up

| Original | Relay | Grade |
| --- | --- | --- |
| title 【高优】容灾建设专项-2026 · 国际电商风险运营平台: 【风险平台 - US 容灾风险（高优）】推动报告2026-08-27 | [High priority] disaster recovery construction special project -2026 · **Guo Ji Dian Shang Feng Xian Yun Ying Ping Tai**: [Risk Platform - US Disaster Recovery Risk (High Priority)] Promote Report 2026-08-27 | title ✅ text ✅; Sender ❌ `romanize()` again (plan.md Later) |
| Yubao Guo: TTP 故障应急最新信息同步：↵为缓解TTP2 容量风险，目前决策执行回切C端10%读流量到 TTP 操作（10... | TTP fault emergency latest information synchronization:↵To mitigate the TTP2 capacity risk, the current decision is to cut back 10% of the C-end read traffic to the TTP operation (10... | ✅ |
| Shasha Chen: 由于 TTP 发布异常，今天的发布窗口推迟到明天[image] | Today's release window postponed to tomorrow due to TTP release exception [image] | ✅ |
| Zhifu Liu@you: https://…/drawer?flowType=1&isNew=1↵@Max Coplan 麻烦审批一下 cc@Yonghao Cao 没有大学SSR us-ttp 日志的权限 | (URL kept)↵@Max Coplan Please approve cc@Yonghao Cao does not have permission for university SSR us-ttp logs | ✅ |
| Zhiqiang Wang: @Oncall Assistant FS 租户迁移到MPA之后，顶导中的助手SDK缺失customFetch | **~**@ONCALL Assistant FS Tenants Migrate to MPA, the assistant SDK in the top guide is deleted for CustomEtch | ❌ ML Kit fallback: `customFetch` → `CustomEtch`, 缺失 → "deleted", shouting |
| Zhijin Wang: @Oncall Assistant  拉美配置了cerberus的New Icon后，部分菜单不生效 | @Oncall Assistant Latin America Some menus do not work after cerberus New Icon is configured | ✅ (Lark took this Latin-heavy one) |
| Qianqian Zhang: 朋友有一辆车想长期出租，帮忙代发一下：🚗 **2023 Toyota Camry LE*** 地点：Bay... | A friend has a car that wants to be rented out for a long time, please help me send it: 🚗 ** 2023 Toyota Camry LE ** * Location: Bay... | ✅ markdown kept |
| Approval: "ByteIO权限申请" from Toby Jin is awaiting your approval | **~**"BYTEIO Permissions" from Toby JIN IS AWAITI Your Approval | ❌ ML Kit fallback mangled the *English* |
| title 沟通 SEA assistant... · Yan Fan: @Lydia Chengcen Liu 按照之前提供的列表是都处理完了。@Dong Liu 看下我们下assistant... | Communicate with the SEA assistant... · @Lydia Chengcen Liu has finished processing according to the previously provided list. @Dong Liu, take a look at our assistant... | ⚠️ subject muddled ("everything on the list is handled"); title is a noun phrase, not a verb |
| Stella Peng: 想求一两颗泰诺🥹🥹 | I want to ask for one or two Tylenol🥹🥹 | ✅ |

Totals for the day (Han rows, both tables): **Lark 12 ✅ / 4 ⚠️ / 0 ❌**; ML Kit fallback
**0 ✅ / 0 ⚠️ / 2 ❌**; `romanize()` on bot Senders **2 ❌**.

### Finding: the fallback hurts Latin-heavy text

`tools/events stats` reported `fallback 0` all day — a bug: the `~` sits after `Sender: `
and `is_fallback` looked at the start of the text (fixed in this commit). The two real
fallback rows are both **Latin-heavy sentences with a few Han words**: Lark returned them
unchanged (its language detection says "English"), so `FallbackTranslator` sent the whole
sentence through ML Kit, which damaged the English parts more than it helped the Han parts.
The right move is not "better fallback" but **translate only the Han runs** (`权限申请` →
"permission request") and keep the Latin text as it was. → plan.md Later.

## Layer 5's first hours

- 4 Updates, all probes (group and DM, commits 5–6). Real traffic: 2 × `skipped type:post`
  (above), 0 `no-chat`, 0 `error`. The `no-match ×5` are our own stale-cache tests.
- **Refresh tokens are single-use.** At 15:57 PDT the Mac's lark-cli tried to refresh with
  the token we had copied as the phone's seed; the phone had rotated it at 15:22, so the
  refresh failed and lark-cli **cleared its user token** (`token_missing`). Debian's
  separate login was untouched. So `tools/lark-token --write` *hands the chain over* to
  the phone; that machine's lark-cli needs `lark-cli auth login` afterwards (Commands).

## New vs. validated

Validated: 0 crashes; the Relay follows every withdrawal (`cancelAll` ×41, group-summary
×4, no stale Relays); Lark's engine is good on real Han text; the romanize problem; the Q15
race exists.

New: 53 % of Previews are truncated (Layer 5's value, measured); empty Previews are rare;
`post` is the everyday type and must be flattened (commit 7); Previews span lines; the
fallback damages Latin-heavy text (and the tool hid it); withdrawals take minutes to hours
when Max is away; a quarter of the traffic is DM-shaped; the seed token is consumed.
