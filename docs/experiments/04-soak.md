# 🧪 Experiment 04 — Overnight soak on real traffic

Date: 2026-08-25 23:57 → 2026-08-26 10:04 PDT. Build: debug (`cancelNotification` off, so
the Original stays next to the Relay). Source: `filesDir/events.jsonl` via
`adb exec-out run-as com.vegerot.larklish cat files/events.jsonl`. Times below are PDT.

## Questions

1. Does the listener survive a night of real traffic? (crashes, missed Originals)
2. Which removal reason does a Lark-initiated withdrawal carry?
3. How good is ML Kit on real Previews when nobody is watching?

## Findings

### Reliability ✅

- ✅ One process the whole night: pid 10106, started 23:57:54, elapsed 10:06:45 at
  read time. `logcat -b crash` has zero Larklish entries.
- ✅ 9 of 9 Originals posted while the listener was bound got a Relay. The one Original
  that arrived before the rebind (key `…873230118`, Lanxiang Wang) has no Relay and its
  later removal was harmless (`manager.cancel` on a tag that never existed).
- ✅ Zero stale Relays at read time: the shade holds one Lark Original and its one Relay
  (Chenglong Song, 08:51). Every earlier Relay was withdrawn by the mirror.
- ✅ In-place updates work: key `…-829139454` (group 【高优】容灾建设专项-2026) was posted
  four times (Chenglong Song 08:33, Yangyang Hu 08:37, Lori Zhang 08:41, Chenglong Song
  08:51). Each `notify(key, RELAY_ID)` replaced the previous Relay, as Lark replaces its
  Original. Same for the Approval bot key `…1795986313` (two different approvals).

### Withdrawal reason: always 9 📏

Every removal was `reason=9` (`REASON_APP_CANCEL_ALL`). No `reason=1` (Max tapped
nothing on the phone) and no `reason=8` (the Experiment 03 guess). Lark withdraws by
calling `cancelAll()`, which also removes Android's autogroup summary (key
`g:Aggregate_AlertingSection`, 2 of the 9 removal rows).

| Posted | Withdrawn | Delay | Note |
| --- | --- | --- | --- |
| 23:58 Yifei Cheng | 00:11 | 13 min | with the pre-bind Original and the summary |
| 00:13 Kani@you | 00:14 | 7 s | |
| 00:36 Approval (Kani role) | 00:36 | 19 s | |
| 01:36 Approval (IAM) | 01:36 | 11 s | |
| 01:37 Zhifu Liu reminder | 08:34 | 7 h | Max asleep; withdrawn with the next batch |
| 08:33 Chenglong Song | 08:34 | 17 s | |
| 08:37 → 08:51 three URL posts | — | live | still in the shade |

The 7–19 s delays while Max was at a computer, and the 7 h gap while Max slept, say:
`cancelAll` fires when Max reads the chat somewhere (desktop or phone). The recorder
cannot tell desktop from phone; it does not need to. The mirror handles 9 the same as 1.

### ML Kit quality on real traffic ⚠️

9 Relays: 4 pure English Previews passed through untouched ✅ (the Han gate works: Kani,
two Approval texts, three URL posts). 4 Han messages: 0 ✅ / 4 ⚠️ (gist right, details
damaged). 1 Han group title: ❌, shown four times.

| Original | Relay | Verdict |
| --- | --- | --- |
| 【直播开启🔛】 生态构建、进化与观测：DeepSeek Harness 技术实践[「TechHub·Live… | [Live open 🔛] Ecological construction, evolution and observation: Deepseek Harness technology practice ["Techhub · Live … | ⚠️ gist OK; 生态 = ecosystem, not "Ecological"; `DeepSeek`/`TechHub` case lost; emoji survived |
| Approval: "申请字节云IAM权限" from Zhifu Liu is awaiting your approval | Approval: "Application byte Cloud IAM Permissions" from zhifu liu is awaiting your approval | ⚠️ 字节云 is ByteCloud (a product), "apply for" not "Application"; **the English name inside the message was lowercased** |
| Zhifu Liu: "申请字节云IAM权限" is still awaiting your action | Zhifu Liu: "Application Byte Cloud IAM Permissions" Is Still Awaiting your Action | ⚠️ same; the English tail came back in Title Case |
| Chenglong Song: oec.supply_chain.warehouse_fulfillment  需要扩容。… | Chenglong Song: OEC.supply_Chain.warehouse_fulfillment needs to be expanded. … | ⚠️ meaning right (需要扩容 = needs more capacity); identifier case mangled |
| title 【高优】容灾建设专项-2026 | [High excellent] Disaster-Disaster Construction Special -2026 | ❌ 高优 = high priority; 容灾 = disaster recovery; 专项 = project. Real: "[High priority] Disaster recovery project - 2026" |

Same pattern as Experiment 02: mixed Han + Latin text is where ML Kit hurts most — it
rewrites the Latin parts (case, spacing) it should leave alone. Not unusably bad: every
message kept its gist. The group title is the worst case and it repeats on every post.

### Recorder defects found by reading the data 🐛

- The `text` column is rebuilt from `Preview` (`"$sender: $message"`), so it **drops the
  mention**. Row 5 reads `Kani Permission Notification: …` while the Relay correctly says
  `Kani Permission Notification@you: …`. The Original had `@you`; the record lost it.
  Fix: record the raw `EXTRA_TEXT`.
- Removal rows include Lark's autogroup summary key (2 of 9). Noise. The posted path
  already skips `FLAG_GROUP_SUMMARY`; the removed path does not.

## Answers

1. Yes. One process, no crash, 9/9 relayed, 0 stale Relays.
2. `REASON_APP_CANCEL_ALL` (9), from Lark's `cancelAll()`, whenever Max reads the chat
   anywhere. The mirror already covers it.
3. Gist survives; names, identifiers and group titles do not. The `LarkApiTranslator`
   follow-up stands. A per-title cache is not needed (four identical calls cost nothing
   visible).
