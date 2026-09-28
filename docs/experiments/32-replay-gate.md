# Experiment 32 — Replace the aggregate replay gate

Date: 2026-09-28. Scope: test behavior; Lookup behavior is unchanged.

## 1. Question the requirement

Reproduced 67/165 resolved: 71 no-chat, 9 interactive cards, 9 no-match, 7 no-message,
1 system message, 1 image. The inherited >=50% gate failed without identifying a
regression and could not detect a wrong message if the resolution count stayed constant.

All 165 corpus Originals lack recorded truncation; 86 texts do not end in an ellipsis.
The replay calls Lookup for all Originals, independently of current phone eligibility.
Its denominator is not current Update reliability. Missing truncation remains unknown.

Selected the first 10 resolved and first 10 unresolved rows in corpus order before
classification. This is a bounded diagnostic sample, not a representative estimate.
Private evidence and input hashes: `replay-corpus/gate-audit-20260928.json` (ignored).

| Zero-based rows | Inspected raw evidence | Assessment |
| --- | --- | --- |
| 1, 2, 5 | Matching image/prose, Han post, Latin post, 5–11 seconds earlier | Known identities |
| 9 | Image, localized mention, same question, 7 seconds earlier | Known identity |
| 10 | Exact DM text, 4 seconds earlier | Known identity |
| 11 | Localized opening mention, same remaining request, 26 seconds earlier | Known identity |
| 21 | All-mention, same question and image, 8 seconds earlier | Known identity |
| 27, 28 | Long and short experiment messages, 8 and 6 seconds earlier | Known identities |
| 33 | Localized mention, same complete question, 8 seconds earlier | Known identity |
| 7 | Interactive card with same title, 11 seconds earlier | Expected unsupported-type skip |
| 0, 3, 4, 6, 8, 12, 13, 14, 15 | No captured DM mapping or message within 60 seconds anywhere in captured messages | Unknown; ungraded |

Known identities are supported by raw text, chat mapping and timing. Originals do not
expose message IDs. Missing evidence does not prove a live Lookup failure. No live API
queries were made. Nine unknowns receive no invented expected identity.

## 2. Delete

Removed the 50% assertion. Retained private corpus replay, outcome counts and TSV output
as diagnostics. The unchanged 67/165 count is not a correctness score.

## 3. Simplify

`TestReplayKnownCases` uses `backend/testdata/replay-cases.json`: 10 positive cases and
one unsupported card, with fictional text and IDs preserving inspected API shapes.
No colleague messages or internal identifiers are checked in.

SDK JSON decoding → production candidateOf → ParsePreview → Fetcher.FullTextOf checks
expected chat, message ID and text, or expected skip. Each positive case includes a
newer unrelated message. Expectations are authored from intended behavior, not regenerated
from test output. Existing Fetcher, picker and server tests retain their coverage.
These independent fixtures do not validate every corpus row or every cache history.

## 4. Accelerate

Known cases run without a phone, credentials, private corpus or network. The optional
corpus remains available for distributions and discovering additional cases.

## 5. Automate and verify

The existing Go test command and SCM unittest.sh discover the new gate; no workflow or
helper command was added. The full Backend suite passed. Two temporary production
mutations were rejected by the new gate: replacing matched IDs while keeping resolution
successful, and replacing matches with no-match. Both were restored and the suite passed
again. This does not certify all 67 resolved corpus messages as correct.
