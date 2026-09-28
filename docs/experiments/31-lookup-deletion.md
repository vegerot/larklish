# Experiment 31 — Delete Full-text Lookup from a recorded cohort

Date: 2026-09-28. Decision: retain Lookup. This was an offline removal of Lookup's
Updates from recorded flows; no app, Backend, phone setting, or deployment was changed.

## 1. Question the requirement

The user outcome is understanding enough of an Original to decide or act without opening
Lark. Recovering every cut Preview is a means to that outcome. Test whether keeping only
the first Preview Relay would preserve the useful information in recent traffic.

Bound: the latest 50 non-test `relayed` events with `truncated=true` and a nonempty
`flowId`, in the snapshot taken on September 28. They span
2026-09-17T01:37:28.110022Z through 2026-09-28T21:51:13.360788Z. The snapshot contains
3,584 events and ends at 2026-09-28T21:51:15.199149Z. Pair outcomes by exact flow ID,
not by notification key. Exclude `Larklish 测试群`. Repeated Originals remain separate
observations; this is a notification cohort, not 50 independent conversations.

Private evidence is in the ignored directory `replay-corpus/deletion-20260928/`:
`events.jsonl` is the phone snapshot; `assessment.json` contains the 50 flows, their
recorded outcomes/timings, the retained Preview Relay (`withoutLookup`), and assessment.
Read the snapshot with `tools.larklish_helper.read_events`; no credentials were collected.
The evidence spans older and current Recorder schemas: early Updates have `fullText`,
later ones have `message` and `source`. All 29 Updates have `relayText` for comparison.

## 2. Delete in the offline comparison

For each flow, remove its Lookup Update and retain its first Preview Relay. Compare the
information remaining against the recorded Update. This tests removal of delivered
Full text. It does not simulate a separate Lark translation of the cut Preview, so
translation quality improvements alone do not count as evidence for retaining Lookup.

Rubric (agent judgment, not a user study):

- Added information: the Update supplies a concrete action, reason, location, complete
  link, operational detail, or other substantive information absent from the Preview.
- Preview sufficient: the Preview already expresses the apparent point; the Update adds
  sentence completion, image markers, or an emoji with little apparent decision value.
- Lost Preview text: the Update removes visible information without a demonstrated gain.
- No Update: removal does not change the observed first Relay. The unseen Full text's
  potential value remains unknown.

| Observed result | Count of 50 | Effect of deleting Lookup |
| --- | ---: | --- |
| Added information | 23 | Lose substantive information |
| Preview sufficient | 5 | Little apparent information loss |
| Lost Preview text | 1 | Preserve text that the Update removed |
| No Update | 21 | Same observed Preview Relay |

The 29 delivered Updates took a median 3.574 seconds after the first Relay; nearest-rank
p90 was 6.476 seconds, maximum 16.037 seconds. This is conditional on Updates arriving.
It excludes time before the first Relay and does not measure whether Max read the Update.

Assessment row IDs, in ascending Recorder order:

- Added information: 1, 2, 3, 5, 7, 8, 9, 10, 11, 19, 21, 22, 24, 31, 32, 34, 35,
  36, 38, 46, 48, 49, 50.
- Preview sufficient: 20, 25, 26, 28, 30.
- Lost Preview text: 47.
- Remaining rows: no Update.

Examples without copying private messages:

- Rows 2–3 recover a requested action and complete approval links.
- Rows 9–10 recover the question after a long SDK identifier. Row 9's translation
  still loses nuance about URL lifetime; recovered text is not proof of perfect translation.
- Rows 34–36 recover scaling details and operational links, including rollback links.
- Row 32 adds where a lost phone can be collected.
- Rows 20 and 30 already convey the offer/request; their additions are minor.
- Row 47 replaces a URL plus visible prose with only the URL. Its record says `post`
  and `source=full-text`; the cause (flattening, matching, or an edit) is not established.
  Do not label it a wrong-message match without raw API evidence.

The 21 non-Updates comprise 8 `no-message`, 3 `no-chat`, 2 `type:interactive`,
5 `translation-failed:not-translated`, 1 translation rate limit, 1 API search timeout,
and 1 canceled flow. The canceled flow is followed 38 ms later by another Original on
its notification key. Translation failures are not evidence of failed message matching.

## 3. Simplify what survives

Reject complete Lookup deletion: it would discard substantive information in 23 of
these 50 observations. Also reject skipping Lookup for Latin-only Previews: 15 of those
23 useful additions have no Han in their Original's text. Language alone is a poor
proxy for the value of recovering Full text.

Keep the current simple cut-Preview eligibility rule for now. The five low-value
Updates do not establish a cheap reliable rule that distinguishes them before Lookup.
Do not add a classifier or more matching rules from this experiment.

The next concrete discrepancy to investigate is row 47's loss of visible Preview prose.
Use the corresponding raw API message to establish the cause before choosing a fix.
No individual matching rule was disabled here, so this experiment cannot attribute
benefit to the cache, delayed window, mention matching, or DM search separately.

## 4. Accelerate after the deletion decision

Existing Recorder flow IDs made this comparison possible without deploying another
build or sending test messages. Continue using this evidence path for a targeted row-47
investigation. The historical 67/165 replay result is not this cohort's success rate;
this cohort has 29/50 delivered Updates and a separate content-value judgment.

## 5. Automate only if repeated

No new permanent helper command, dashboard, or deployment automation. This is one
bounded experiment. Keep the private assessment and this decision record; automate the
comparison only if it becomes a repeated task with an agreed grading rubric.

## Limits and verification

The cohort mixes deployments (36 outcome records name Railway, 13 name PPE, and the
canceled flow has no outcome record). It spans a phone build/schema change and a gap
in recorded traffic. It is not a randomized trial or a current-version reliability test.
No claims about attention saved, actual reading, correctness of every match, or host
superiority follow from it. A Preview may suffice for triage even when Full text adds
information; Max has not graded these rows.

Verified: 50 selected flows, 29 delivered Updates, assessment categories sum to 50,
and all 29 Updates are assigned one of the three comparison categories. Private files
are ignored by Sapling. No live messages, token refreshes, or Backend requests were needed.

## Row 47 follow-up — edited API message, no parser loss demonstrated

Read the matching chat's raw messages through the existing helper for the five-minute
window before this Original and five seconds after it. The API returned one message.
Private evidence: `replay-corpus/deletion-20260928/row47-messages.json`.

- Message created: 2026-09-28T18:19:11.790Z.
- API marks `updated=true`; `update_time`: 2026-09-28T18:19:24.706Z.
- First Relay recorded: 2026-09-28T18:19:27.912457Z.
- Backend Update recorded: 2026-09-28T18:19:31.941326Z.
- Both `content` and `content_v2` contain one link element whose text is the URL.
  Neither contains the prose visible after the URL in the Original.

A temporary Go probe passed the raw SDK message through production `candidateOf` and
`pickMessage`. It reproduced exactly the recorded URL-only text and selected that
message using the recorded Preview stem and timing. The probe passed and was removed;
no production code changed. This establishes that the current raw message is not losing
prose in Larklish's parser. The recorded Update also agrees with that raw text.

The strongest explanation is an Original retaining prose from before an edit while
Lookup retrieves the edited message. The API read exposes the current message, not its
edit history, so the exact pre-edit text is not proven. There is no evidence here of
selecting a different message or of a flattening defect.

Correction to the initial interpretation: row 47 loses *Preview text*, but calling that
a product defect was premature. Keeping deleted prose could itself misrepresent the
sender's current message. Retain its observational category, but do not count it as a
confirmed benefit of deleting Lookup. Do not add a length guard or reject edited messages
on this evidence. No runtime fix is justified by this investigation.
