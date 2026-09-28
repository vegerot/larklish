# Experiment 33 — Does the early ML Kit Relay earn its place?

Date: 2026-09-28. Decision: retain the early Relay and ML Kit. Offline comparison;
no phone settings, runtime code, messages, or deployment changed.

## 1. Question the requirement

Max values something useful appearing quickly, while recognizing useless output adds
noise. Judge the first Relay on useful early understanding, not polished English.
Compare deleting ML Kit and leaving the Original visible until a Backend Update arrives.
An Original already appears promptly; the feature under test adds early English.

Selected the latest 20 non-test relayed events with a nonempty flowId and Han in title
or text, without filtering by outcome. Window: September 26 01:35:24.549805 UTC through
September 28 21:34:00.566998 UTC. Pair by exact flowId. Exclude the Larklish test group.
Ordinary automated reports remain included. Two cases have Chinese only in the title.
The remaining 18 have Chinese in the message. There are repeated reminders and reports;
these are 20 notifications, not 20 independent kinds of user need.

Private snapshot, selected flows and grades are in the ignored directory
`replay-corpus/early-relay-20260928/` (`events.jsonl`, `sample.json`, `assessment.json`).
Read via the existing helper. No token or Backend call was needed.

Rubric: useful gist preserves the apparent topic/request even with awkward wording;
material error corrupts a concrete detail or the subject enough that the result needs
correction. Title-only means the message was already English. These are agent judgments
against the Chinese Preview, not Max's reading behavior or a blinded translation study.
A useful-gist grade does not certify every detail or technical identifier.

## 2. Delete in the offline comparison

Remove the first translated Relay and retain the Original until any recorded Update.

| Early result | Count | Row IDs |
| --- | ---: | --- |
| Useful message gist | 14 | 1, 2, 4, 5, 7, 9, 10, 12, 13, 14, 15, 18, 19, 20 |
| Material error | 4 | 3, 8, 16, 17 |
| English message, useful translated title | 2 | 6, 11 |

Four material errors: percentile labels become “before 19%” and “1 21%”; one URL is
corrupted alongside a bad phrase translation; “development machine” becomes
“Development Aircraft.” Some topic information survives even in these rows, so do not
call all four completely useless.

Examples of useful early information: a GPU application failed; a package keeps reporting
an error; a team is sharing work for discussion; a version upgrade involves modal/drawer
behavior; download is slow while upload is normal. A mooncake exchange request is
recognizable, but the flavors are unreliable. A daily usage report is recognizable
although its title is imprecise.

Observed outcomes: 13 Updates, 5 no-message misses, 2 complete-no-han skips by design.
No translation failure or cancellation occurs in these latest 20; none was excluded
by selection. Do not extend that absence to other traffic.

Of the five misses, four early Relays convey useful gist (10, 13, 14, 15), and one has
the Development Aircraft error (16). Deletion would leave all five without an English
message in the recorded flow. The title-only rows would lose translated group context
because the current Backend eligibility rule ignores title-only Han.

Separately grading the 13 Updates: 11 convey useful gist (some with wording caveats),
while 2 retain material problems: a usage report becomes a dosage report (9), and a
request to exchange mooncakes becomes a description of traditional activities (12).
Backend output is not a perfect reference translation. Row 8 also adds Full text, so
its improvement cannot be attributed solely to a better translation model.

## Timing and counterfactual limits

Recorded monotonic Original-to-Relay: median 0.768 seconds, range 0.205–2.549 seconds
across all 20. Wall-clock Relay-to-Update: median 2.069 seconds, range 0.976–4.872 seconds
among the 13 Updates. Thus the early English was available during a measurable wait,
and remained the only English message on the five Lookup misses.

These delays are observations of the current sequential implementation. Removing ML Kit
would let the Backend start earlier, so the observed gap is not an exact prediction of
extra wait after deletion. No alternate implementation or perceived latency was measured.
The Original already exists, and debug builds also retain it alongside the Relay.

One timing anomaly: row 5 has 2.341 seconds wall-clock Relay-to-Update but 0.716 seconds
in its monotonic relay_to_outcome span; Backend spans also exceed the recorded phone
request duration. Cause unverified. Report wall time for the visible Update gap; do not
use nested timing spans to claim precise savings or sum them as independent operations.

## 3. Simplify what survives

Reject deleting ML Kit: it supplies useful early English in 14/18 Chinese-message rows,
including four that never get a Backend Update. This supports Max's preference for
quick information. Retain the early Relay and Backend Update pattern.

The evidence identifies fidelity problems worth separate investigation; it does not
establish a reliable runtime rule for suppressing bad translations. Do not add a
quality classifier, token-protection framework, or new fallback from this sample.
A future bounded experiment could test preserving URLs/identifiers before changing
translation behavior, but no such change is part of this experiment.

## 4. Accelerate after the decision

No new latency optimization follows automatically. The existing flow IDs and Recorder
were sufficient to compare usefulness and timing. Investigate the timing discrepancy
before using fine-grained duration arithmetic to justify concurrency or latency work.

## 5. Automate only if repeated

No new helper command, dashboard, or permanent test suite. Preserve the private grades
and this decision. There is no runtime change to test or install. Verified all 20 rows
are graded, outcomes pair by flowId, and the private evidence stays ignored.
