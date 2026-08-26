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
