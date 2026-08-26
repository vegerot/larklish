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
