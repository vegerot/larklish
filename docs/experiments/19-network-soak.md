# Network soak — September 9–14, 2026

Analyzed September 14 PDT from a read-only ADB snapshot of 2,386 events saved
at `/tmp/larklish-soak-20260914.jsonl`. This temporary raw snapshot contains private
message data and is not checked in. Use `read_events` and `relays_with_outcomes`
from `tools/larklish_helper.py` to reproduce the counts.

## Window and results

Use the first network-recorded event, September 10 00:05:33 UTC (September 9
17:05 PDT), through September 15 01:02:49 UTC (September 14 18:02 PDT).
There are 477 events and 177 Relays, all with recorded truncation; no test-group
Relays. Of these, 134 Previews were complete and intentionally skipped Lookup.
The other 43 were cut: 13 Updated (30.2%), 28 paired with transport errors,
and two had no paired final outcome. The helper calls the latter `canceled`,
but that label alone does not prove why an outcome was absent.

| Network at Relay | Cut Previews | Updated | Paired error | No final outcome |
| --- | ---: | ---: | ---: | ---: |
| Office Wi-Fi, Inspire Creativity | 13 | 13 | 0 | 0 |
| Home Wi-Fi, FBI_van_🚚 | 24 | 0 | 23 | 1 |
| No Wi-Fi | 6 | 0 | 5 | 1 |

All 477 events recorded VPN false. No-Wi-Fi is consistent with cellular, but the
record does not explicitly identify a cellular transport. VPN presence here is
for the app's default network. This sample does not test working VPN access.
All 13 Updates name the CN ByteFaaS endpoint `jmc8tl6s.fn.bytedance.net`:
11 post messages and two text messages. Median Relay-to-Update latency was
3.466 seconds; maximum 5.664 seconds. This measures delivery, not translation
accuracy or proof that every Lookup selected the correct message.

There are 29 raw transport errors versus 28 paired cut-Preview errors: the helper
pairs by notification key, so these are different measurements. Raw errors are
24 connection timeouts (private IPv4 or IPv6 destinations), three read timeouts,
and two TLS connection-closed errors. The network split strongly supports
intranet reachability as the dominant failure, consistent with the existing
private endpoint. It does not demonstrate public connectivity.

## Other findings

- A new withdrawal race: Relay at September 15 00:50:15.045299 UTC, Original
  removed with reason 9 at 00:50:18.533364, then Update at 00:50:19.381256.
  The Update landed 0.848 seconds after removal. This is another occurrence of
  the known race, not just the historical showcase event.
- Twelve translation fallback events: six unchanged-input responses and six
  translation API rate-limit errors (99991400). No manual translation-quality
  grading was performed.
- Phone listener was bound and app running at inspection. Retained exits since
  installation include nine low-memory exits, one signal-9 exit, and two
  instrumentation-related force stops. No retained crash/ANR exit. This does not
  prove uninterrupted notification coverage.
- Installed package last updated September 9 16:46:45 phone local time. No
  reinstall, test message, network change, or deployment was performed.

## Public URL status

This is the initial 20:13 PDT snapshot. Later deployment work, including BOE
removal, successful PPE tests, and the Nario blocker, is recorded in
[progress.md](../progress.md#2026-09-14--boe-removed-singapore-ppe-self-test-succeeds).

Live I18N-TT read on September 14 around 20:13 PDT:
`faas release list --service-id kpb2dvsn --limit 3` returned zero runtime releases.
`faas cluster get --service-id kpb2dvsn --region sg --cluster faas-sg` returned
404 `release record not found` for latest_release. Singapore production is
therefore still unverified/unreleased. The phone's latest successful Update still
names CN.

Last documented Bits blockers remain the BOE production-baseline lookup failure
and five manual GEC checks; their detailed UI state was not refreshed this turn.
The release-window restriction was already resolved by retrying at 19:00 on
September 9. It is not the sole outstanding blocker.

Remaining sequence: resolve normal Bits release workflow and applicable checks;
release and verify Singapore health/Lookup/instance limits; add phone-to-Backend
authentication and protect chat-cache access; configure the documented Consul
trigger and public domain/certificate/route through NetLink/TLB and required
review; verify from cellular with VPN off before phone cutover. No public domain
or ingress submission is recorded in the project history. No fresh complete
NetLink/TLB inventory was queried in this analysis.
