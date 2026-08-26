# 🧪 Experiment 02 — Translation quality (Layer 2)

Date: 2026-08-25. Phone: Pixel 4a. ML Kit `translate:17.0.3`, zh→en model downloaded
on first call. Lark column: `POST /open-apis/translation/v1/text/translate --as bot`.

## Question

Is on-device ML Kit good enough for a notification Preview, judged beside Lark's own
engine? Pass: Max judges the ML Kit column "gist OK".

## Method

- Translator screen (`MainActivity`) with a debug hook: `adb shell am start
  --activity-clear-task -n com.vegerot.larklish/.MainActivity --es text "…"` translates
  at once and logs `Translator: in=[…] out=[…]`.
- 14 Previews in the `Sender: message` shape. Rows 1–5 are **real** Previews taken from
  the shade (`dumpsys notification --noredact`). Rows 6–14 are **made up** by Claude in
  the same register (mentions, identifiers, numbers, jargon, bot and calendar messages).
- The whole `Sender: message` string went to both translators, as decided in round 1
  ("whole string for now, let evidence decide").

## Table

| # | Source | Chinese Preview | ML Kit (on-device) | Lark API |
| --- | --- | --- | --- | --- |
| 1 | real | Yinghao Li: 哈喽@Jiarui Chen @Yufan Chen ， 我发现 oec.seller.assistant_api 有个问题 | Yinghao Li: Hello @Jiaroi Chen @yufan chen, I found OEC.Seller.Assistant_API has a problem | Yinghao Li: Hello @Jiarui Chen @Yufan Chen, I found a problem with oec.seller.assistant_api |
| 2 | real | Max Coplan's Feishu CLI: 测试一：这是 Larklish 第一层实验的中文消息... | Max Coplan's Feishu CLI: Test 1: This is a Chinese message in LarkLish's first layer of experiment ... | _rate-limited (99991400), 4 tries_ |
| 3 | real | ByteTech: 全栈需求实践分享——从一个接口开始; 《洛克王国·世界》中的漫反射GI方案; ModHeader被禁了，何不用AI写一个... | Bytetech: All-Stack Needs Practice Sharing - Starting from an interface; the diffuse reflection of "Lock Kingdom · World"; Modheader is banned, why not write one ... | ByteTech: Full-stack Requirements Practice Sharing - Starting from an Interface; Diffuse Reflection GI Solution in "Rock Kingdom: World"; ModHeader is Banned, Why Not Use AI to Write One... |
| 4 | real | Feng Xiao: @Haochen Yang 我批了那2个ticket | Feng Xiao: @Haochen Yang I brought the 2 Ticket | Feng Xiao: @Haochen Yang I approved those 2 tickets |
| 5 | real | Lanxiang Wang: @HwaCheng Sun 这个 cookie banner，你们有没有考虑过直接在 TLB 配置好 | Lanxiang Wang: @hwacheng sun This cookie banner, have you considered direct TLB configuration? | Lanxiang Wang: @HwaCheng Sun, have you considered configuring this cookie banner directly on TLB? |
| 6 | made up | Lanxiang Wang: 这个需求今天能上线吗？QA 那边还有两个 P1 的 bug 没修完 | Lanxiang Wang: Can this demand come online today? There are two P1 BUGs that have two P1 on the QA. | Lanxiang Wang: Can this requirement be launched today? There are still two P1 bugs that have not been fixed in QA. |
| 7 | made up | Daitao(DT) Song@you: @Max Coplan 麻烦帮忙看下这个 PR，CI 一直挂在 lint 那步 | Daitao (DT) Song @ you: @Max Coplan troubles to see this Pr, CI has been hanging in the step | Daitao (DT) Song@you: @Max Coplan Please take a look at this PR, CI has been hanging in the lint step |
| 8 | made up | Rachel Lam@all: 提醒大家下周三下午三点有全员会议，请提前预留时间 | Rachel Lam @ All: Remind everyone to have a full member in the afternoon, please reserve time in advance | Rachel Lam@all: Remind everyone that there is an all-staff meeting at 3pm next Wednesday, please reserve time in advance |
| 9 | made up | Bits: 你的构建任务 seller_center_web#4521 已失败，请查看日志 | Bits: Your build task SELLER_CENTER_WEB # 4521 has failed, please check the log | Bits: Your build task seller_center_web #4521 failed, please check the log |
| 10 | made up | Jiarui Chen: 线上 oec.seller.assistant_api 的 p99 延迟涨到 800ms 了，谁在看？ | Jiarui Chen: Online OEC.Seller.Assistant_API's P99 delay rose to 800ms, who is watching? | Jiarui Chen: The p99 latency of the online oec.sell.assistant_api has risen to 800ms, who is watching? |
| 11 | made up | Yufan Chen: 收到，我这边先回滚一下昨天的配置，稍后同步结果 | YUFAN Chen: Received, I will roll back this time, I will synchronize the result later. | Received, I will roll back yesterday's configuration first and sync the results later. |
| 12 | made up | Calendar Assistant: 「商家中心周会」将于 15 分钟后开始 | Calendar Assistant: "Business Center Week" will start after 15 minutes | Calendar Assistant: "Merchant Center Weekly Meeting" will start in 15 minutes |
| 13 | made up | Wei Zhang: 数据看板的 GMV 口径和财务对不上，差了大概 3%，明天早上拉个会对齐一下 | Wei Zhang: The GMV caliber of the data sheet and financial is not more than 3%, and it will be aligned tomorrow morning. | Wei Zhang: The GMV caliber of the data dashboard does not match the finance, there is a difference of about 3%. I will hold a meeting tomorrow morning to align it. |
| 14 | made up | Yinghao Li: 辛苦了！这周的 oncall 交接文档我更新到飞书文档里了，链接在群公告 | Yinghao Li: Hard! This week's oncall handover document I update to the flying book document, link in group announcement | Yinghao Li: Thank you for your hard work! I updated the oncall handover document for this week to the Feishu document, and the link is in the group announcement. |

## Observations (Claude; Max judges)

- 📏 **Names and identifiers get damaged by ML Kit in almost every row.** `Jiarui` →
  `Jiaroi`, `Yufan Chen` → `yufan chen` / `YUFAN Chen`, `HwaCheng Sun` → `hwacheng sun`,
  `oec.seller.assistant_api` → `OEC.Seller.Assistant_API`, `seller_center_web#4521` →
  `SELLER_CENTER_WEB # 4521`, `PR` → `Pr`, `ByteTech` → `Bytetech`.
- 📏 **Mentions lose their shape.** `Song@you:` → `Song @ you:`, `Lam@all:` → `Lam @ All:`.
  Lark keeps them.
- 📏 **Meaning errors in ML Kit** (rows 4, 6, 8, 11, 13, 14): "I approved those 2
  tickets" → "I brought the 2 Ticket"; row 6 garbled ("have two P1 on the QA"); row 8
  lost "meeting next Wednesday 3 pm" ("have a full member in the afternoon"); row 11
  lost "yesterday's configuration"; row 13 "differs by ~3%" → "is not more than 3%";
  row 14 `辛苦了` → "Hard!", `飞书` → "flying book".
- 📏 **Gist OK in ML Kit** for rows 1, 2, 5, 7, 9, 10, 12 (7 of 14), with the name and
  identifier damage above.
- 🚩 **Lark's engine is not perfect either.** Row 10: `oec.seller.assistant_api` →
  `oec.sell.assistant_api`. Row 11: it **dropped the Sender** `Yufan Chen: ` entirely.
- 🚩 Lark API rate limit `99991400` hit 5 times in 14 sequential calls, and row 2 never
  got through in 4 tries. The limit is far below the documented 20 QPS; cause unknown.
- 💡 Both engines treat the Sender as part of the sentence. Splitting `Sender: ` off
  before translation, and gluing it back after, would remove one whole class of damage
  (every Sender-name error above) at zero translation cost.

## Gotchas

- 🐚 zsh: `mapfile` does not exist. Read a file in a loop with `while read -r line <&3;
  … done 3< file` so `adb shell` inside the loop cannot eat stdin.
- 🐚 `adb shell` in a `while read` loop consumes the loop's stdin. Add `</dev/null`.
- 🐚 `am start` with the same component and an identical intent is delivered to the
  running instance (`onNewIntent`), which this Activity ignores. Use
  `--activity-clear-task` for a fresh instance each time.
- 🪤 XML comments cannot contain `--`. My manifest comment with `--es` broke the manifest
  merger with a bare "Error parsing AndroidManifest.xml".
- 🪤 Compose BOM `2026.08.00` (runtime 1.12.0) needs `compileSdk 37`. `targetSdk` can stay
  36; they are separate. This is what Studio's earlier bump was about.
- 🪤 `rg` on this machine has no PCRE2, so `\p{Han}` fails. Filter Chinese with Python
  `[一-鿿]`.

## Benchmark 2 — ByteDance / TikTok / Lark jargon (30 rows)

Max does not read Chinese, so Claude wrote the corpus, the reference readings, and the
grades. The rows are **made up** in the register of Max's groups: engineering chat with
ByteDance-speak (灰度/全量, 双月 OKR, 大促, 埋点, 数仓, 达人, 直播带货, 兜底/降级/限流,
复盘/闭环/沉淀, 提测/冒烟/回归, 联调, 排期, 起量, 心智, 风控/合规, 值班/工单/告警) and Lark
features (飞书文档, 多维表格, 妙记, 加急, 审批, 群公告, @所有人). Same `Sender: message`
shape as a real Preview. Same debug hook for ML Kit; Lark API calls spaced 3 s apart
(no rate-limit errors in 30 calls, versus 5 of 14 back-to-back earlier).

### Outputs

| # | Chinese Preview | ML Kit (on-device) | Lark API |
| --- | --- | --- | --- |
| 1 | Yufan Chen: 这个需求的 PRD 我放飞书文档了，大家评审前先看一下，明天下午两点需求评审 | YUFAN Chen: The proportion of this demand, I let go of the book, let's take a look at each other, two demand for two pm tomorrow. | Yufan Chen: I have put the PRD of this requirement in the Feishu document. Please take a look before reviewing. The demand review will be held at 2pm tomorrow afternoon. |
| 2 | Jiarui Chen@you: @Max Coplan 你那个 MR 冲突了，麻烦 rebase 一下再提 | Jiarui chen @ you: @max coplan Your MR conflict, trouble Rebase | Jiarui Chen@you: @Max Coplan your MR conflict, please rebase it again |
| 3 | Lanxiang Wang: 今晚灰度 10%，明早看数据没问题就全量 | Lanxiang Wang: Tonight is 10%, and there is no problem early in tomorrow morning. | Lanxiang Wang: Grey release 10% tonight, full amount tomorrow morning if there is no problem with the data |
| 4 | Bits: 【告警】seller-center-api 集群 5xx 比例超过 1%，持续 5 分钟，请值班同学关注 | Bits: [Alarm] SELLER-CENTER-API cluster 5xx proportion exceeds 1%, lasting for 5 minutes, please pay attention to class students | Bits: [Alert] Seller-center-api cluster 5xx ratio exceeds 1%, lasting for 5 minutes, please pay attention to the on-duty colleagues. |
| 5 | Rachel Lam@all: 双月 OKR 对齐会改到周四上午十点，会议室 SJ-3F-Lark，线上可以进飞书会议 | Rachel Lam @ All: Dunue OKR aligned will change to Thursday morning ten o'clock, meeting room SJ-3F-LARK, online can enter the flying book meeting | Rachel Lam@all: The bimonthly OKR alignment meeting will be changed to Thursday at 10am, in the meeting room SJ-3F-Lark, you can enter the Feishu meeting online. |
| 6 | Daitao(DT) Song: 大促期间冻结上线，有紧急修复走审批流程找 leader 加急 | Daitao (DT) Song: Freezing during the big promotion, there is an emergency repair and approval process to find Leader expedition | Daitao (DT) Song: The launch is frozen during the sales campaign. If there is an emergency fix, go through the approval process and find the leader to expedite it. |
| 7 | Haochen Yang: 埋点数据看板今天不准，数仓那边在补数，预计下午恢复 | Haochen Yang: The docking point data is not allowed today, and the number there is expecting, it is expected to recover afternoon. | Haochen Yang: The event tracking data dashboard is not accurate today, and the data warehouse is making up for it. It is expected to resume in the afternoon. |
| 8 | Wei Zhang: 这个 case 商家反馈退款一直卡在审核，工单号 TS-20260825-0913，谁能帮忙看下 | WEI Zhang: This case business feedback refunds have been truck in the audit, the work list TS-20260825-0913, who can help look down | Wei Zhang: The merchant feedback on this case has been stuck in the review process for a long time. The ticket number is TS-20260825-0913. Can anyone help take a look? |
| 9 | HwaCheng Sun: 直播带货那个链路的 p99 抖动挺大的，怀疑是下游限流了，先降级兜底 | HWACHENG SUN: The P99 of the live broadcast is quite big, suspected that the downstream is limited, first downgrade | HwaCheng Sun: The p99 jitter of the LIVE commerce link is quite large, and it is suspected that it is the limited viewership of the downstream. First downgrade to cover up |
| 10 | Feng Xiao: 复盘文档我已经沉淀到群公告了，重点是这次没做好兜底，下次要有闭环 | Feng Xiao: I have already settled to the group announcement, the focus is this time, I have to have a closed loop next time. | Feng Xiao: I have already deposited the review document into the group announcement. The key point is that I didn't do a good job of fallback this time. Next time, there should be a closed loop |
| 11 | Yinghao Li: 达人侧的样品申请接口 502 了，TikTok Shop 那边的同学在看，先别重试 | YINGHAO Li: The sample application interface of the Daren is 502, and the classmates on Tiktok SHOP are watching, don't try again. | Yinghao Li: The sample application interface on the creator side is 502, and the students on the TikTok Shop side are looking at it. Don't try again first |
| 12 | Calendar Assistant: 「Seller Center 周会」将于 15 分钟后开始，会议室 SJ-3F-Lark | Calendar Assistant: "SELLER CENTER Will" will start after 15 minutes, meeting room SJ-3F-LARK | Calendar Assistant: "Seller Center Weekly Meeting" will start in 15 minutes, Meeting Room SJ-3F-Lark |
| 13 | Lark: 你有一条来自 Yufan Chen 的加急消息，请及时查看 | Lark: You have an expedited news from Yufan Chen, please check in time | Lark: You have an urgent message from Yufan Chen, please check it in time. |
| 14 | Jiarui Chen: 提测了，QA 同学麻烦冒烟一下，回归重点是订单列表和售后入口 | Jiarui Chen: Tested, QA students are in trouble, the return is the order list and after-sales entrance | Jiarui Chen: submitted for QA, QA colleagues please smoke, the focus is on the order list and after-sales entrance |
| 15 | Lanxiang Wang: 这个 AB 实验放量到 50% 了，转化率涨了 0.3 个点，但客单价掉了 | Lanxiang Wang: This AB experiment is put into 50%, and the conversion rate has risen by 0.3 points, but the passenger bill is lost. | Lanxiang Wang: This AB experiment has scaled to 50%, and the conversion rate has increased by 0.3 points, but the average transaction value has dropped |
| 16 | Wei Zhang: 我这边排期排不开，能不能先做 MVP，颗粒度粗一点，下个迭代再优化 | Wei Zhang: I can't open this side, can you do MVP first, granular granularity, next iterative re-optimization | Wei Zhang: I can't schedule it here. Can we do MVP first, granularity is thicker, and optimize it in the next iteration? |
| 17 | Bits@you: @Max Coplan 你提交的审批「测试机申请」已通过 | Bits @ you: @Max Coplan you submit approval "Tester Application" has passed | Bits@you: @Max Coplan Your application for testing device has been approved |
| 18 | Daitao(DT) Song: 联调的时候发现 mock 数据和线上不一致，接口文档谁维护的？ | Daitao (DT) SONG: When you discover the mock data and line inconsistent, the interface document is maintained? | Daitao (DT) Song: During joint debugging, it was found that the mock data was inconsistent with the online data. Who maintains the interface documentation? |
| 19 | Rachel Lam: 周报记得今天下班前提交到多维表格，leader 明早要看 | Rachel LAM: Weekly remember to submit to the multi-dimensional table before work, Leader will look early tomorrow morning. | Rachel Lam: Remember to submit the weekly report to Base before getting off work today, and the leader will review it tomorrow morning. |
| 20 | Haochen Yang: 商家中心新版首页起量了，DAU 破 50 万，感谢大家 | Haochen Yang: The new version of the business center is from the homepage, DAU breaks 500,000, thank you | Haochen Yang: The new homepage of the merchant center has started, DAU has broken 500,000, thank you everyone |
| 21 | Feng Xiao@all: @所有人 今晚机房切换，22:00-23:00 服务可能有抖动，提前告知商家 | Feng Xiao @ all: @ 所有 Tonight to switch, 22: 00-23: 00 service may have jitter, inform the merchant in advance | Feng Xiao@all: @Everyone, tonight the data center switch, 22:00-23:00 service may have jitter, inform the merchant in advance |
| 22 | HwaCheng Sun: 这个功能的心智还没建立起来，商家不知道入口在哪，建议加个引导 | HWACHENG SUN: This function has not been built, the merchant does not know where the entrance is, it is recommended to add a boot | HwaCheng Sun: The mentality of this function has not been established yet, and merchants do not know where the entrance is. It is recommended to add a guide |
| 23 | Yufan Chen: 收到，我拉个群同步一下，把风控和合规的同学也拉上 | YUFAN Chen: Received, I pulled a group to synchronize, and pulled the wind control and compliance. | Received, I will create a group to synchronize and add colleagues in risk control and compliance |
| 24 | Yinghao Li: 值班交接：今天有两个 P1 还没闭环，一个是物流轨迹不更新，一个是优惠券重复发放 | YINGHAO Li: Today, two P1 have not closed the ring today, one is the logistics track is not updated, one is the coupon repetitive issue | Yinghao Li: On-duty handover: There are two P1s that have not been closed today, one is that the logistics trajectory is not updated, and the other is that coupons are repeatedly issued |
| 25 | Lanxiang Wang: 抖音那边的打法可以借鉴，但 TikTok 这边合规要求不一样，得先过法务 | Lanxiang Wang: The play on the side of the shake can be drawn on, but Tiktok is not the same as the compliance requirements. | Lanxiang Wang: Douyin's play can be learned from, but TikTok compliance requirements here are different, you have to go through Legal first |
| 26 | Jiarui Chen: 服务挂了，正在回滚，大家先别发布 | Jiarui Chen: Service hangs, rolling back, everyone is released first | Jiarui Chen: The service is down and rolling back. Please don't release it yet. |
| 27 | Wei Zhang: 妙记我发群里了，没参会的同学可以看一下要点 | Wei Zhang: Wonderful I have a group, I have no participation, I have to look at it. | Wei Zhang: I have sent Feishu Minutes in the group. Students who did not attend the meeting can take a look at the key points. |
| 28 | Daitao(DT) Song: 这块逻辑有点绕，我串讲一下，大家周五有空吗 | Daitao (DT) Song: This logic is a bit around, I talk about it, do you have time on Friday? | Daitao (DT) Song: This logic is a bit confusing. Let me give a presentation. Are you free on Friday? |
| 29 | Bits: 你的构建 seller_center_web #4588 成功，耗时 6 分 12 秒 | Bits: Your build seller_center_web # 4588 success, time consuming 6 minutes 12 seconds | Bits: Your build seller_center_web #4588 successful, took 6 minutes and 12 seconds |
| 30 | Rachel Lam: 麻烦大家填一下这个多维表格，统计一下大促期间的值班安排 | Rachel Lam: Trouble everyone fills this multi-dimensional table, statistics on duty arrangements during the big promotion | Rachel Lam: Please fill out this Base and count the duty arrangement during the sales campaign |

### Grades (Claude)

✅ gist right · ⚠️ partly right, awkward but recoverable · ❌ wrong or misleading

| # | ML Kit | Lark | Claude's reading of the Chinese | Note |
| --- | --- | --- | --- | --- |
| 1 | ❌ | ✅ | PRD is in a Feishu doc; read it before review; requirements review tomorrow 2 pm | ML Kit: 'let go of the book', 'two demand for two pm' |
| 2 | ⚠️ | ✅ | your MR has a conflict; please rebase and resubmit | ML Kit: gist OK, names lower-cased, '@ you' |
| 3 | ❌ | ✅ | grey release 10% tonight; full rollout tomorrow morning if data looks fine | ML Kit lost 灰度/全量 entirely: 'Tonight is 10%' |
| 4 | ⚠️ | ✅ | [Alert] seller-center-api cluster 5xx > 1% for 5 min; on-call please look | ML Kit: 'class students' for on-call colleagues |
| 5 | ❌ | ✅ | bimonthly OKR alignment moved to Thu 10 am, room SJ-3F-Lark, or join the Feishu meeting online | ML Kit: 'Dunue OKR', 'flying book meeting' |
| 6 | ⚠️ | ✅ | deploy freeze during the sale; urgent fixes go through approval, ask the leader to expedite | ML Kit: 'Leader expedition' |
| 7 | ❌ | ✅ | event-tracking dashboard is inaccurate today; data warehouse is backfilling; back this afternoon | ML Kit: 'docking point data is not allowed' |
| 8 | ⚠️ | ✅ | merchant says a refund is stuck in review, ticket TS-20260825-0913, can someone look | ML Kit: 'truck in the audit'; ticket id intact in both |
| 9 | ⚠️ | ⚠️ | live-commerce path p99 jitters a lot; suspect downstream rate limiting; degrade to the fallback first | Lark: 'limited viewership' for 限流, 'cover up' for 兜底 |
| 10 | ❌ | ✅ | retro doc is in the group announcement; the point is we had no fallback; next time close the loop | ML Kit dropped 复盘 and 兜底 |
| 11 | ⚠️ | ✅ | creator-side sample-request API returns 502; TikTok Shop folks are on it; don't retry yet | ML Kit: 'Daren' untranslated |
| 12 | ❌ | ✅ | 'Seller Center weekly meeting' starts in 15 min, room SJ-3F-Lark | ML Kit: 'SELLER CENTER Will' |
| 13 | ✅ | ✅ | you have an urgent (buzz) message from Yufan Chen |  |
| 14 | ❌ | ✅ | submitted for testing; QA please smoke-test; regression focus: order list and after-sales entry | ML Kit: 'QA students are in trouble' |
| 15 | ⚠️ | ✅ | AB test ramped to 50%; conversion +0.3 pt but average order value dropped | ML Kit: 'passenger bill is lost' |
| 16 | ❌ | ✅ | no room in my schedule; can we ship an MVP at coarse granularity and refine next iteration | ML Kit: 'I can't open this side' |
| 17 | ✅ | ✅ | your approval request 'test device application' was approved |  |
| 18 | ❌ | ✅ | during joint debugging the mock data differs from prod; who owns the API doc? | ML Kit: 'When you discover the mock data' |
| 19 | ⚠️ | ✅ | submit the weekly report to the Base (multi-dimensional table) before EOD; leader reads it tomorrow | Lark knows 多维表格 = Base |
| 20 | ❌ | ⚠️ | new Seller Center homepage is gaining traction; DAU passed 500k; thanks all | ML Kit: 'is from the homepage'; Lark: 'has started' is weak |
| 21 | ❌ | ✅ | @all: data-center switch tonight 22:00–23:00, service may jitter; tell merchants in advance | ML Kit left @所有 untranslated and lost 机房 |
| 22 | ⚠️ | ⚠️ | users have no mental model of this feature; merchants can't find the entry; add an onboarding guide | Lark: 'mentality' for 心智 — recoverable |
| 23 | ⚠️ | ⚠️ | got it; I'll open a group to sync and pull in risk-control and compliance | Lark DROPPED the Sender again (message starts with 收到); ML Kit: 'wind control' |
| 24 | ⚠️ | ✅ | on-call handover: two P1s still open — logistics tracking not updating; coupons issued twice | ML Kit: 'closed the ring' for 闭环, gist survives |
| 25 | ❌ | ✅ | Douyin's playbook is worth borrowing, but TikTok compliance differs; legal must sign off first | ML Kit: 'the shake' for 抖音, lost 法务 |
| 26 | ❌ | ✅ | service is down; rolling back; nobody deploy for now | ML Kit INVERTED it: 'everyone is released first' |
| 27 | ❌ | ✅ | Minutes (妙记) posted in the group; those who missed it can read the key points | ML Kit: 'Wonderful I have a group' |
| 28 | ⚠️ | ✅ | this logic is convoluted; I'll do a walkthrough; free on Friday? | ML Kit: 'a bit around, I talk about it' |
| 29 | ✅ | ✅ | build seller_center_web #4588 succeeded in 6 min 12 s |  |
| 30 | ⚠️ | ✅ | please fill in this Base to collect on-call shifts for the sale period | ML Kit: gist OK, 'big promotion' |

| Engine | ✅ | ⚠️ | ❌ |
| --- | --- | --- | --- |
| ML Kit | 3 | 13 | 14 |
| Lark API | 26 | 4 | 0 |

## Assessment (Claude, 2026-08-25)

- **Lark's engine is clearly better.** 26 of 30 right, 0 wrong. It knows the
  jargon: 灰度 → "grey release", 埋点 → "event tracking", 数仓 → "data warehouse", 达人 →
  "creator", 多维表格 → "Base", 妙记 → "Feishu Minutes", 加急 → "urgent". Its four ⚠️ rows are
  word choices a reader can recover (限流 → "limited viewership", 心智 → "mentality") and one
  dropped Sender.
- **ML Kit is misleading on jargon-heavy chat.** 14 of 30 wrong, 13 partly,
  3 right. It has no ByteDance vocabulary (飞书 → "flying book", 抖音 → "the shake",
  妙记 → "Wonderful", 双月 → "Dunue", 风控 → "wind control") and it mangles casing of every
  name and identifier. Worst case, row 26: `大家先别发布` ("nobody deploy yet") became
  "everyone is released first" — the opposite instruction, in fluent English. The failure
  mode is silent: the output reads like English, so the reader cannot tell it is wrong.
- **Plain chat is better for ML Kit** (first table: gist in 7 of 14). The bot and calendar
  rows (13, 17, 29) are fine in both engines.
- **Verdict against Max's rule** ("stick to ML Kit for faster iteration; abandon only if
  unusably bad"): ML Kit is **usable as a hint** — who wrote, in which group, roughly what
  about — and **not usable as a reading of what was said** in engineering groups. That is
  borderline, not unusable. So: keep ML Kit through Layer 3 (the Relay plumbing does not
  depend on the engine; `Translator` is one method), and record **`LarkApiTranslator` as
  the first follow-up feature after Layer 3**, expected to replace ML Kit as the default.
- **Both engines mistreat the Sender when it sits inside the sentence** ("damage the
  Sender"): ML Kit re-cases or misspells the name (`yufan chen`, `YUFAN Chen`, `Jiaroi`,
  `HWACHENG SUN`) and breaks `@you`/`@all` into `@ you`/`@ All`; Lark dropped `Yufan Chen: `
  twice (rows 11 and 23, both messages start with 收到). Fix for both: split the Sender off
  before translation and put it back after (`Preview.parse`, TDD later per Max).
- **Caveat:** the corpus is Claude's, not Max's real messages. Reading real group messages
  needs the `lark-cli` user identity (`search:message` and message-read scopes), which is
  missing on this machine and may need tenant approval. Real Previews from the shade (5 so
  far) are in the first table. A week of real Relays is the true test.

## Benchmark 3 — 30 real messages from Max's Lark (2026-08-25)

Source: `lark-cli im +messages-search --as user --is-at-me` (last 14 days) and
`--query 的 --chat-type group` (last 3 days), after Max granted `search:message` to the CLI
app (device flow, approved in Chrome). Each message is flattened by Claude to the Preview
shape Lark would post: `Sender: message`, `Sender@you:` / `Sender@all:` for mentions, the
card title for cards, `<at user_id="all">` → `@all`, cut near the first clause. This is an
approximation of the Preview, not the Preview itself.

### Outputs

| # | Preview (real message) | ML Kit (on-device) | Lark API |
| --- | --- | --- | --- |
| 1 | 温天信: @翁斌斌 @徐银苓 @Max Coplan 出差过来的同学没有车。 | Wen Tianxin: @ 翁斌斌 翁斌斌 翁斌斌 @ 徐银苓 徐银苓 徐银苓 @max Coplan has no car. | Wen Tianxin: @Weng Binbin, @Xu Yinling, @Max Coplan. The classmate who came on a business trip doesn't have a car. |
| 2 | 温天信@all: 明天中午我们 12 点去 Redmond 的 Idylwood Park 吃烧烤哈。 | Wen Tianxin @all: Tomorrow At noon, we went to Redmond's IDYLWOOD Park to eat BBQ. | Wen Tianxin @all: Tomorrow at noon, we will go to Idylwood Park in Redmond for barbecue at 12 o'clock. |
| 3 | 温天信@all: 看看哪位同学住在 Bellevue 附近或者经过 Bellevue 公司的，可以带上没有车/出差的同学吗。 | Wen Tianxin @all: See which classmate lives near Bellevue or by Bellevue, can you bring a classmate without a car / business trip? | Wen Tianxin @all: See if any classmates who live near Bellevue or pass through Bellevue company can bring classmates who don't have a car or are on a business trip. |
| 4 | 王蓝翔@all: 准备全量了 | Wang Saixiang @all: ready to be full | Wang Lanxiang @all: Ready for full quantity |
| 5 | 王蓝翔@all: 0824 主应用班车现在开始灰度 10% | Wang Saixiang @all: 0824 Main application shuttle now starts grayscale 10% | Wang Lanxiang @all: 0824 Main application shuttle now starts grey release 10% |
| 6 | 王蓝翔@all: 继续 30% | Wang Saixiang @all: Continue 30% | Wang Lanxiang @all: Continue 30% |
| 7 | 王蓝翔@all: 本次发布已完成，预期发布 730 个页面，实际发布成功 685 个页面，有一些撞车和配置冲突的暂时没有继续发布 | Wang Saixiang @all: This release has been completed, expected 730 pages, actually released 685 pages, some crash and configuration conflicts have not continued release | Wang Lanxiang @all: This release has been completed. It is expected to release 730 pages, but 685 pages have been successfully released. Some collisions and configuration conflicts have not been continued for the time being |
| 8 | 周报与分享@all: 下周的主应用发布班车收集已开启，需要上车的乘客请坐稳扶好～ | Weekly and Share @all: Next week's primary application release shuttle has been opened, the passengers who need to get on the car should be held up ~ | Weekly report and sharing @all: The collection of shuttle buses for next week's main application release has started. Passengers who need to get on the bus, please sit tight and hold on~ |
| 9 | 弋鹏玮@all: 受 TT <-> non-TT 机房拉闸影响，通过 tce 服务在 TT 机房调用 llmbox 的，如果使用的是 llmbox-global.byteintl.net | 弋鹏玮 弋鹏玮 @all: is affected by the TT <-> Non-TT machine room, using the TCE service to call LLMBOX in the TT computer room, if used is LLMBOX-GLOBAL.BYTEINTL.NET | Yi Pengwei @all: Affected by TT < - > non-TT data center shutdown, call llmbox in TT data center through tce service, if using llmbox-global.byteintl.net |
| 10 | 弋鹏玮@all: 按照 modelhub 的要求，我们已经上线了对应模型接口协议的改造。 | 弋鹏玮 弋鹏玮 @all: According to the requirements of Modelhub, we have launched the transformation of the corresponding model interface protocol. | Yi Pengwei @all: According to the requirements of ModelHub, we have already launched the transformation of the corresponding model interface protocol. |
| 11 | Triton数据安全@you: 【提醒】数据权限到期提醒！Hi @Max Coplan，你有18条权限将于9天内到期。若判断为工作所需权限，请及时续期或重新申请权限。 | Triton data security @YOU: [Reminder] Data privilege expiration reminder! Hi @Max Coplan, 18 permissions will be expired within 9 days. If it is determined as the required permissions, please renew or re-apply for permissions. | Triton Data Security @you: [Reminder] Reminder for data access permission expiration! Hi @Max Coplan, you have 18 permissions that will expire within 9 days. If it is determined that the permissions are required for work, please renew or reapply for permission in time. |
| 12 | 审批@you: TEA小助手在 “Tea-DES元信息登记审批” 中提及了你：Max Coplan 同学你好，该DES审批单已进入DES审批环节 | Approval @YOU: TEA Small Assistant refers to you in the "TEA-DES meta information registration approval": Max Coplan classmates Hello, the DES approval list has entered the DES approval | Approval @you: TEA Assistant mentioned you in "Tea-DES Meta Information Registration Approval": Hello Max Coplan, this DES approval form has entered the DES approval stage. |
| 13 | Bits: 发布单分支同步存在冲突：【Seller Center】Shop Performance Module and adding Shop Health Violations | Bits: Release Single Branch Synchronous Existence Conflict: [Seller Center] Shop Performance Module and Added Shop Health ViOLATIONS | Bits: Release single branch synchronization conflict: [Seller Center] Shop Performance Module and adding Shop Health Violations |
| 14 | GEC容量治理: GEC 容量治理 - 集群正在升级（需人工处理） PSM: oec.seller.learning_center_api 报警时间: 2026-08-21 04:30:36 | GEC capacity governance: GEC capacity governance - cluster is upgrading (need to be manually handled) PSM: OEC.Seller.learning_center_api Alarm time: 2026-08-21 04:30:36 | GEC capacity governance: GEC capacity governance - cluster is being upgraded (requires manual processing) PSM: oec.seller.learning_center_api Alarm time: 2026-08-21 04:30:36 |
| 15 | TikTok Web Bot@you: [Tiktok Arch] TikTok资产Owner关系确认 Hello @Max Coplan，为了给合规/成本/数据中心建设提供准确的资产归属信息 | Tiktok Web Bot @ you: [Tiktok Arch] Tiktok Assets Owner Relationship Confirm Hello @Max Coplan, in order to give compliance / cost / data center Construction provides accurate asset-owned information | TikTok Web Bot@you: [TikTok Arch] TikTok Asset Owner Relationship Confirmation Hello @Max Coplan to provide accurate asset ownership information for compliance/cost/data center construction |
| 16 | 夏银璐@you: Hi Max @Max Coplan ，@黄细娜 Xina is our RD for TikTok Shop US merchants and also researches the SEO. | Summer Silver @You: hi max @max coplan, @ 黄细娜 xina is out for tiktok shop US Merchants and also Researches the seo. | 夏银璐@you: Hi Max @Max Coplan ，@黄细娜 Xina is our RD for TikTok Shop US merchants and also researches the SEO. |
| 17 | 合规治理助手@you: 👋 ROW Service Public Network Upgrade - goofy_ssr.sg.3259424 @Max Coplan 请尽快确认 | Compliance Governance Assistant @YOU: 👋 Row Service Public Network Upgrade - GoofY_SSR.sg.3259424 @max Coplan please Confirm as soon as possible | Compliance Governance Assistant @you: 👋 ROW Service Public Network Upgrade - goofy_ssr.sg @Max Coplan Please confirm ASAP |
| 18 | 孟显峰@all: iOS 40.1.0 福利抽奖开始啦！！！对应版本：抖音iOS 40.1.0 版本字节内测 | Meng Xuanfeng @all: iOS 40.1.0 welfare draw begins!   Correspondence version: Tudo IOS 40.1.0 version byte internal test | Meng Xianfeng @all: iOS 40.1.0 welfare lottery starts!!! Corresponding version: Douyin iOS 40.1.0 version ByteAlpha |
| 19 | 纪振强: 是的，辛苦了 | Ji Zhenqiang: Yes, hard work | Ji Zhenqiang: Yes, you've worked hard. |
| 20 | 俞月伦: lark-cli是飞书做的项目，开源了，google下 | Yu Yuelun: Lark-CLI is the project made by flying books, open source, google | Yu Yuelun: Lark-cli is a project made by Feishu, open-sourced, under google |
| 21 | Zhao Yifan: bytedcli MR Review 修复 `bytedcli oncall chat list --help` 对 `--id` 的误导说明：运行时要求 flow ID，但帮助文本此前写成 chat ID。 | Zhao Yifan: BYTEDCLI MR REVIEW Misldown Description: The Flow ID is required at runtime, but the help text is previously written as a CHAT ID. | Zhao Yifan: bytedcli MR Review fixes the misleading statement about '--id' in'bytedcli oncall chat list --help ': Flow ID is required at runtime, but the help text was previously written as chat ID. |
| 22 | 郭松荣: @俞月伦 这个开发机的traex没有lark 相关能力吗，怎么读不了飞书文档了 | Guo Song Rong: @ 俞月伦 俞月伦 This development has no LARK related ability, how to read the flying book documentation | Guo Songrong: @Yu Yuelun, does the traex of this developer computer not have Lark-related capabilities? Why can't it read the Feishu documentation? |
| 23 | Argos平台报警: [critical] Error rate of downstream calls is too high - Target: bytedance.abase2.gec_seller_profile:default | Argos platform alarm: [critical] error Rate of DownStream Calls is Too High - Target: Bytedance.abase2.gec_seller_profile: Default | Argos平台报警: [critical] Error rate of downstream calls is too high - Target: bytedance.abase2.gec_seller_profile:default |
| 24 | 周梦楠: @杨铮 这个界面出问题现在应该是遇到啥 BUG 恢复不了，你可以重新打开窗口后，回复个“继续”，AI 应该能续上之前的活 | Zhou Mengnan: @ 杨铮 This interface has problems should now be encountered by bug recovery. After you reopen the window, reply to "Continue", AI should be able to continue | Zhou Mengnan: @Yang Zheng, there is a problem with this interface. It should be due to encountering some bug that cannot be restored. You can reopen the window and reply with "continue". The AI should be able to continue the previous activity |
| 25 | 杨铮: @周梦楠 关闭应用重新打开是可以的。但是打断了正在运行的任务 | Yang Wei: @ Zhou Mengnan closed the application to re-open it. But interrupted the running task | Yang Zheng: @Zhou Mengnan, it is possible to close the application and reopen it. But it interrupts the running task. |
| 26 | 魏强: 那就算了  我以为能用 trae 的资源[看] | Wei Qiang: That's, I thought I could use TRAE resources [see] | Wei Qiang: Forget it, I thought I could use Trae's resources [see]. |
| 27 | Yifei Feng: 也可以配置toml文件，里面加上你的ak 这边澄清一下，是modelhub你们组的ak | Yifei feng: You can also configure TOML files, plus your AK here, it is modelhub you group AK. | Yifei Feng: You can also configure the toml file and add your ak to it. To clarify, it is the ak of your group in ModelHub. |
| 28 | 张显良: @王鑫运 哥你哥是哪里来的ttadk init dsh[看] 我分支还没合入呢 | Zhang Xiangliang: @ 王鑫运 王鑫运 哥 Your brother is from the TTadk INIT DSH [see] My branch has not been incomited. | Zhang Xianliang: @Wang Xinyun, where did your brother come from ttadk init dsh [look] I haven't merged the branch yet |
| 29 | Matt Yang: @Oncall 助手 现在x.openAuthPage相关的问题 增加3P platform要怎么加 | Matt Yang: @ONCALL Assistant Now X.OpenAuthpage related questions increase 3P Platform how to add | Matt Yang: @Oncall Assistant, now x.openAuthPage related issues, how to add 3P platform |
| 30 | 幸湘熔: @Oncall 助手 懒加载 正式包是好的 但是测试包有问题 | Fortunately: @ONCALL Assistant lazy loading official package is good but the test package has a problem | Xing Xiangrong: @Oncall assistant, lazy loading, the official package is good, but there is a problem with the testing suite |

### Grades (Claude)

| # | ML Kit | Lark | Note |
| --- | --- | --- | --- |
| 1 | ❌ | ✅ | ML Kit tripled every mention name and mis-attributed 'has no car' |
| 2 | ⚠️ | ✅ | ML Kit tense: 'we went' |
| 3 | ✅ | ✅ |  |
| 4 | ❌ | ⚠️ | 准备全量了 = 'about to go full rollout'. ML Kit 'ready to be full'; Lark 'full quantity' |
| 5 | ⚠️ | ✅ | 灰度: ML Kit 'grayscale', Lark 'grey release' |
| 6 | ✅ | ✅ |  |
| 7 | ⚠️ | ✅ | 撞车 (collision): ML Kit 'crash' |
| 8 | ⚠️ | ✅ | release-train metaphor; both keep it |
| 9 | ⚠️ | ✅ | ML Kit left Sender 弋鹏玮 untranslated and DOUBLED it; 机房 'machine room' |
| 10 | ⚠️ | ✅ | Sender doubled again |
| 11 | ✅ | ✅ |  |
| 12 | ⚠️ | ✅ | ML Kit 'TEA Small Assistant', 'classmates Hello' |
| 13 | ⚠️ | ✅ | ML Kit 'Release Single Branch Synchronous Existence Conflict' |
| 14 | ✅ | ✅ | ML Kit re-cased the PSM identifier |
| 15 | ⚠️ | ✅ |  |
| 16 | ❌ | ✅ | Sender 夏银璐 → 'Summer Silver' (literal); English message mangled ('is out for'). Lark left the Chinese name as-is |
| 17 | ⚠️ | ⚠️ | Lark cut the identifier to goofy_ssr.sg (lost .3259424); ML Kit re-cased it |
| 18 | ⚠️ | ✅ | 抖音: ML Kit 'Tudo'; 字节内测: Lark 'ByteAlpha' (the real internal name) |
| 19 | ⚠️ | ✅ | 辛苦了: ML Kit 'hard work' |
| 20 | ⚠️ | ✅ | 飞书: ML Kit 'flying books' |
| 21 | ❌ | ✅ | ML Kit 'Misldown Description', garbled |
| 22 | ⚠️ | ✅ | Sender doubled; 'flying book documentation' |
| 23 | ✅ | ✅ | English alert; both fine. Lark kept Sender Argos平台报警 in Chinese |
| 24 | ⚠️ | ✅ |  |
| 25 | ✅ | ✅ |  |
| 26 | ⚠️ | ✅ | 那就算了 'forget it': ML Kit 'That's,' |
| 27 | ⚠️ | ✅ |  |
| 28 | ❌ | ✅ | ML Kit 'Your brother is from the TTadk' |
| 29 | ⚠️ | ✅ |  |
| 30 | ❌ | ⚠️ | Sender 幸湘熔 → 'Fortunately' (literal); Lark '测试包' → 'testing suite' (test build) |

| Engine | ✅ | ⚠️ | ❌ |
| --- | --- | --- | --- |
| ML Kit | 6 | 18 | 6 |
| Lark API | 27 | 3 | 0 |

## Assessment, revised with real data (Claude, 2026-08-25)

- **Real chat is plainer than Benchmark 2.** ML Kit's gist rate rises: 6 ✅ and
  18 ⚠️ of 30, 6 ❌. Most ⚠️ rows are readable with effort ("grayscale
  10%", "flying book", "hard work"). The ❌ rows are garbled or misattributed.
- **The Sender is where ML Kit fails hardest, and real Senders are often Chinese names.**
  It translated names literally (夏银璐 → "Summer Silver", 幸湘熔 → "Fortunately"), left
  others untranslated and doubled them (弋鹏玮 弋鹏玮), tripled names inside mentions
  (row 1), and re-cased every identifier. Lark romanizes names (温天信 → "Wen Tianxin") or
  keeps them as-is (夏银璐, Argos平台报警), which is the right behaviour for a Preview.
- **Lark: 27 ✅, 3 ⚠️, 0 ❌.** It knows the internal names
  (字节内测 → "ByteAlpha", 灰度 → "grey release"). Its ⚠️ rows: 全量 → "full quantity", one
  identifier cut short, 测试包 → "testing suite".
- **Verdict stands** (Max's rule): ML Kit is a usable hint on plain chat and misleading
  on jargon; not unusable. Keep it through Layer 3. `LarkApiTranslator` is the first
  follow-up and should become the default. Both are hidden behind `Translator`.
- **`Preview.parse` is now mandatory, not optional.** With the Sender split off and
  never translated, ML Kit's worst failure class disappears. Open question for Max: a
  Chinese Sender name in the Relay — keep the characters (what Lark shows in-app), or
  romanize (what the Lark API does)? Keeping them is zero code.
  **Decision (Max, 2026-08-25): romanize** — Chinese characters are useless to Max.
  Done in `Romanize.kt` with Android's ICU `Han-Latin/Names` transform, test-first on the
  phone (`RomanizeTest`): 温天信 → "Wen Tianxin", Argos平台报警 → "Argos Ping Tai Bao Jing".
- **Scope finding (overturns part of the handoff):** the CLI app's user token already
  carries `im:message.group_msg:get_as_user`, `im:message.p2p_msg:get_as_user`,
  `im:message:readonly`, `im:chat.user_setting:read`, `search:message` and more. The
  handoff's claim that message-read scopes are "effectively blocked" by security review
  is false for this app. The other reasons to reject polling (two booleans vs three
  Tiers, no push for users) still hold. The 2.0 "full message" idea becomes feasible.

## Emoji (added 2026-08-25 late)

- 🚩 ML Kit translates a lone 😀 as **"Bamboo"** (Max found it on the translator screen).
- 🚩 In mixed text ML Kit **deletes** emoji: `服务挂了😀，正在回滚👍🏼` → "The service
  hangs, rolling back".
- ✅ The Han gate (`String.hasHan` → `Translator.englishOf`) keeps non-Han text away from
  the engine, so an emoji-only Preview passes through unchanged. The gate now guards both
  the Relay and the translator screen. Emoji loss inside Chinese text remains an engine
  defect; revisit with `LarkApiTranslator`.
