# Helper reports and acceptance checks — September 16, 2026

The September 9 and 14 soak reviews repeatedly rebuilt grouped counts, time
windows, error counts and Relay-to-Update latency in Python. The Railway session
also built a separate acceptance runner. This change extends the acceptance
commands introduced in `abc4e5a`, after rebasing onto GitHub `main`.

## Soak report

```sh
tools/larklish-helper events \
  --file output/helper-verification/events.jsonl \
  --since 2026-09-10T00:05:33Z \
  --until 2026-09-15T01:03:00Z \
  --exclude-test-group --utc \
  grade --group-by network --json
```

- The interval includes `since` and excludes `until`. An offset is honored;
  without an offset, filter times mean UTC. Outcomes are observed only before
  `until`. The grade selects Relays by their recorded time after pairing.
- `--han` retains the selected Relay's Update and skip. Previously it discarded
  them before pairing, changing a one-Update sample from 1/1 to 0/1.
- A missing outcome is `no recorded outcome`, not proof of cancellation.
  Missing `truncated` remains unknown and outside the cut-Preview denominator.
- `--exclude-test-group` excludes only the dedicated `Larklish 测试群`.
- `--group-by network|backend|day` splits the same Relay cohort. Network means
  the state at Relay; Backend means the URL on its paired outcome, or unknown.
  Daily buckets use local time unless `--utc` is supplied. No Wi-Fi does not
  establish cellular access. Missing Wi-Fi/VPN/Backend fields remain unknown.
- Latency covers Updates to cut Previews, measured from Relay to Update, with
  count, median, nearest-rank p90 and maximum. Empty samples have null metrics.
- Raw event/error counts are separate from paired Relay outcomes. Han/test-group
  filters omit unkeyed fallback events because they cannot be attributed.
- JSON and human-readable output share one report. Misses contain Original
  Preview text, so save reports with the private snapshots under ignored output.
  `events pull` still saves the entire raw record, ignoring view filters.

Recorder still has no per-Original identifier. A late result on a reused key can
attach to a newer Relay; the report states that limitation instead of guessing.

## Probe and Backend acceptance

```sh
tools/larklish-helper probe --idle --expect updated --timeout 60 \
  '明确标记的长测试消息……' > output/probe.json
tools/larklish-helper backend lookup \
  --file output/helper-verification/lookup-case.jsonl \
  --index -1 --repeat 2 > output/lookup.json
tools/larklish-helper phone status --details --json
```

`probe` prefixes its synthetic message with a unique `[probe:…]` marker, avoiding
matches to unrelated traffic with a common text prefix. Thread probes observe the
reply using its own marker. `--expect relayed` accepts a Relay; `updated` requires
an Update whose Full text matches the sent message. The default `auto` requires
an Update for a recorded cut Preview or `not-truncated` for a complete Preview.
Unknown truncation does not pass `auto`. Han presence is reported, not treated
as a translation-quality verdict.

The timeout is a deadline. A terminal skip ends the wait immediately and fails
an Update expectation. Failure reports include phone diagnostics; no matching
Relay does not establish that Lark posted no Original. Forced idle is restored
on failure too. Probes no longer clear logcat; use `phone log --clear` explicitly.
Debug hooks still print logs and cannot combine with a probe expectation or
thread mode. Non-debug probes print JSON to stdout, including failures and no
message contents; use normal shell redirection to save it.

`backend lookup` keeps the upstream events-JSONL input and Relay index. It uses
the recorded time, reads the current access token from `LARKLISH_USER_TOKEN` or
the phone, and never refreshes or writes that token. Each repeated request reports
HTTP status, Lookup outcome/reason, translation availability, elapsed time and
Full-text length/hash. Repetition does not prove a cold cache. Failed HTTP requests,
invalid responses and skipped Lookups exit nonzero. `backend status` also exits
nonzero when health or authentication fails. Failure bodies are not echoed.

`phone status` adds installed version/update time; `--details` adds retained exit
reasons for both apps. Default transport and active VPN networks come from the
current-network section of Android's dump, not request history. Active VPN
networks are a system observation, not proof of the app's VPN routing. The
upstream `phone install` command remains available.

## Verification

- Offline regression tests cover filtering/pairing, exclusive end boundaries,
  timezone offsets, reused keys, unknown fields, latency, Unicode JSONL, probe
  deadlines/expectations/cleanup/thread correlation, current-network parsing,
  and local HTTP requests through the real CLI. Requests preserve the historical
  timestamp and keep tokens out of output; expired phone tokens stop without
  refreshing.
- A fresh read-only snapshot contains 2,614 Recorder events. Selecting the
  Experiment 19 window reproduces 177 Relays, 43 cut Previews, 13 Updates,
  28 paired errors and two without a recorded outcome. Median 3.466 seconds,
  p90 5.333 seconds, maximum 5.664 seconds. Office/home/no-Wi-Fi cut counts and
  outcomes match the earlier report.
- Live phone status identifies default Wi-Fi, no active VPN networks, both apps
  running, listener bound, and normal idle. Nested exit reasons such as
  `APP CRASH(EXCEPTION)` are retained correctly.
- Public Railway health and authenticated cache checks pass. Replaying an
  existing successful synthetic Original twice returns HTTP 200, `found`,
  English present and identical Full-text hashes in 3.656 and 4.577 seconds.
  The Railway URL was selected only through an environment override.
- No fresh message, reinstall, token refresh or network change was performed.
  New probe behavior is covered by offline tests; live checks used existing data.

Private snapshots and results are under ignored `output/helper-verification/`.

## Follow-up — one stdout contract

Repository history uses `--json` as a boolean switch from human output to JSON.
`--file` selects saved input, while replay's `--out` names the multi-file corpus
directory it creates. Probe and Backend Lookup already emit only JSON, so their
short-lived `--json FILE` options were removed: normal shell redirection saves
their reports without a second output mechanism.

The real Python unit suite now includes a pure thread-correlation test: given
root and reply Relays with distinct markers, `probe_observation` selects and
validates the reply. The command-level thread test separately checks that the
reply receives its own marker and is the message observed by the probe.
