# 🧪 Experiment 17 — Backend soak, September 3–9

Snapshot taken 2026-09-09 around 23:24 UTC. The previous soak boundary was
`2026-09-03T06:44`; the selected record spans September 3 06:45 through September 9
23:04 UTC. All times below are UTC.

**The record contains 34 Updates and 48 transport-error events. Corporate-network
reachability is the leading explanation for failed Updates, but individual failures
cannot be classified without network state.** The earlier **34 of 87 (39%)** result
is a historical estimate using inferred truncation across several installs. It is
not the current grader's result or a success rate for the latest build.

## Historical counts using inferred truncation

| Outcome | Count |
| --- | ---: |
| Relays | 300 |
| Complete Preview (`not-truncated`) | 213 |
| Inferred cut Preview | 87 |
| Updated | 34: 18 `text`, 16 `post` |
| Inferred cut Preview paired with `error` | 47 |
| `type:interactive` | 3 |
| `no-message` | 1 |
| `no-chat` | 1 |
| Superseded before an Update | 1 |

The dedicated `Larklish 测试群` accounts for 17 Relays, including 10 Updates and one
failed inferred cut Preview. Excluding that group gives **24 of 76 inferred cut
Previews Updated (32%)**, across 283 Relays. This still includes conversation in a
separate group named `test`; it is not a claim that all 283 are production traffic.

The helper at `986f4ee` infers a cut Preview from trailing `...`. Only the last two Relays
also record `truncated`; both agree with the helper. A literal ellipsis can trigger
the same path, as the last probe demonstrates. The record does not include every
Original received, so these counts cannot establish a delivery rate for Originals.

| UTC date | Relays | Inferred cut Previews | Updated | Inferred cut Preview paired with `error` |
| --- | ---: | ---: | ---: | ---: |
| September 3, from 06:44 | 40 | 20 | 12 | 4 |
| September 4 | 88 | 24 | 12 | 11 |
| September 5 | 7 | 1 | 1 | 0 |
| September 6 | 6 | 2 | 0 | 2 |
| September 7 | 21 | 9 | 0 | 9 |
| September 8 | 69 | 20 | 4 | 15 |
| September 9, through 23:04 | 69 | 11 | 5 | 6 |

### Regrading after the ByteDance pull

Commit `f31e72a` records the phone's actual `Preview.truncated` decision and excludes
missing values. Running that helper against this exact saved snapshot gives:

```text
Relays 300   Preview cut 1   complete 1 (skipped by design)
Excluded 298 Relays without recorded truncation from the grade.

cut Previews → Updated 1 of 1
     1  updated (text)
```

That one cut Preview is the deliberate literal-ellipsis probe in Experiment 16;
its Full text was unchanged. Neither 39% nor this 1-of-1 result establishes the
latest build's success rate on naturally cut traffic. The 34 Updates and 48 raw
transport errors do not depend on the old truncation inference.

## Transport failures are the main problem

There are **48 raw `skipped error` events**:

| Error | Count |
| --- | ---: |
| Connection failure or timeout naming `jmc8tl6s.fn.bytedance.net:443` | 30 |
| Connection timeout to the old Mac home address, `192.168.251.184:8787` | 2 |
| Read timeout | 8 |
| TLS handshake closed | 6 |
| Unexpected end of stream | 2 |

The ByteFaaS connection failures name private IPv4 or IPv6 destinations. Their
source addresses vary across home and other networks. This is evidence of a
network-dependent reachability problem; the record alone cannot distinguish the
route, VPN state, and remote availability for each failure. The generic read,
handshake, and stream errors do not identify their endpoint.

ByteFaaS also demonstrably worked: **19 Updates explicitly record**
`backend=https://jmc8tl6s.fn.bytedance.net`; the earlier 15 Updates do not record an
address. At 23:26, a read-only request from the phone to that host's `/v1/ping`
returned **HTTP 200 in 1.55 seconds**, using `10.8.6.194`. That verifies reachability
at the snapshot time. It does not establish reliability on every network or verify
the Lookup itself.

The 34 successful Updates took **3.43 seconds median, 6.14 seconds p90, 21.93
seconds maximum** from the recorded Relay to the recorded Update. p90 uses nearest
rank. This measures the Update delay, not the initial Preview translation delay.

### Why 48 error events become 47 failed cut Previews

On September 4 at 15:38:44.696, a cut Preview for `L'Chaim UK` relayed. An uncut
Original took the same key 95 ms later and recorded `not-truncated`. At 15:38:56.199,
an error from the earlier work arrived on that key. `relays_with_outcomes` assigns
it to the newer, uncut Relay and overwrites its `not-truncated` outcome. The older
cut Relay remains `canceled` in the grade.

The supersession is real, but the late error exposes a limitation in pairing solely
by key and record order. Do not count it as a new failure of an uncut Preview.

## Translation and withdrawal

- **11 of 300 initial Relays contain the `~` fallback mark.** Three additional
  Updates contain it. The 14 fallback events comprise eight `99991400` rate-limit
  failures and six `NotTranslated` results. These are fragment-level events, not
  necessarily 14 distinct Originals.
- The existing mixed-language problem remains visible: the fallback changes
  `/api/v2/seller/common/get` into `/ API / V2 / SELLER / COMMON / GET`, and an
  otherwise English Full text with a Han mention gets arbitrary capitalization.
- **One Update follows withdrawal without an intervening recorded Relay:** the
  already documented September 3 showcase probe. It relayed at 21:14:30.450,
  withdrew at 21:14:30.935, and Updated at 21:14:33.425 — 2.49 seconds after removal.
  No additional instance appears in this snapshot. This check cannot detect an
  Original withdrawn before its first Relay was recorded.

## Current phone and build boundary

- The process is running; notification permission and listener access are granted.
- Android's retained 16 process exits contain 13 `LOW_MEMORY`, two `SIGNALED`, and
  one `PACKAGE UPDATED`. None is a crash or an app-not-responding exit. The retained
  history starts September 4; it does not prove zero crashes throughout the soak.
- The latest install was **September 9 at 23:02:35 UTC** (16:02:35 on the phone).
  Only two Relays follow it, both in the dedicated test group: one uncut Preview and
  one Update whose Full text is identical to the Preview ending in literal `...`.
  Real cut traffic has not yet validated that install in this snapshot.
- The shade has six message Originals and one matching Relay, with **zero stale
  Relays and five Originals without a Relay**. Those five Originals were last
  updated before the install. Their absence is consistent with the install boundary;
  this snapshot does not prove what removed their Relays.
- At the snapshot, the phone's record included Backend addresses and `truncated`
  that this checkout did not yet write. This source mismatch is now resolved:
  ByteDance was the other machine's `origin`, and fetching it brought this checkout
  to `f31e72a`. Experiment 18 subsequently installed network recording on that base.

## Reproduce and next step

These counts used the helper at `986f4ee`, before this checkout fetched the ByteDance
remote. The helper at `f31e72a` intentionally excludes old Relays without `truncated`;
use the earlier helper to reproduce the historical heuristic counts above.

Raw notifications and message text remain outside the repository in the private
scratch directory `/tmp/larklish-soak-20260909-WLEN6B/`. It contains `events.jsonl`,
`grade.txt`, `updates.json`, and the phone health snapshots; it is temporary evidence.

```sh
git show 986f4ee:tools/larklish_helper.py > /tmp/larklish-soak-20260909-WLEN6B/helper-at-soak.py
python3 /tmp/larklish-soak-20260909-WLEN6B/helper-at-soak.py events \
  --file /tmp/larklish-soak-20260909-WLEN6B/events.jsonl \
  --since 2026-09-03T06:44 --utc stats
python3 /tmp/larklish-soak-20260909-WLEN6B/helper-at-soak.py events \
  --file /tmp/larklish-soak-20260909-WLEN6B/events.jsonl \
  --since 2026-09-03T06:44 --utc grade
```

## What the pulled history changes

- **Corporate access was already a requirement.** `ce0c8e3` and `dbbfb84` establish
  an internal-only ByteFaaS trigger and Max's choice of always-on SealSuite off the
  office Wi-Fi. The old next step was already to enable/verify that VPN and test an
  Update on cellular. A public domain remains deferred. The old soak did not record
  VPN state, so it cannot prove that all failures happened without corporate access.
- **One connection bug is already fixed.** `9af34ed` makes every `IOException`
  advance to the next Backend URL, including the unexpected end-of-stream caused
  by USB forwarding to a stopped Mac Backend. It also records the Backend that
  answered. Errors still lack that field and retain only the last attempted URL's
  exception; do not assume every generic error names ByteFaaS or one URL attempt.
- **The replay refactor does not change production Lookup rules.** Experiment 16
  compares identical data before and after `f31e72a`: all 165 outcomes agree.
  Both resolve 67/165 and fail the existing 50% threshold. That gate needs a separate
  follow-up; its failure does not demonstrate a new matching regression.
- **Build and deployment are distinct.** `718703d` enables builds on push;
  `6bbb09f` adds `backend status` to compare built and deployed revisions, and
  `2ca2aa2` documents the actual release. `/v1/ping` reports the Go version after
  `2166df8`, not the deployed source commit. Fetching source does not deploy it.
- The pulled commits do not fix the withdrawal race, late-error pairing, or the
  mixed-language fallback output. Those remain separate issues.

Next: finish/verify the existing always-on SealSuite step and validate the
ByteFaaS path on cellular without a local Backend satisfying the request. For a
probe, inspect the actual `truncated` and `backend` fields: Experiment 16 shows
that `probe --idle` alone does not guarantee a naturally cut Preview. Then grade
fresh real traffic containing `truncated` and the debug `network` fields from
Experiment 18. Separate office Wi-Fi, VPN, other networks, and unknown network
state; VPN presence alone does not prove corporate reachability. Prioritize errors
that persist with verified corporate access. No additional Backend deployment is
needed solely for the phone's new network recording.

The original snapshot and this history review sent no probe messages and made no
Backend changes. Experiment 18 documents the intervening app install separately.
