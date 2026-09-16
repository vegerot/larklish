# Add end-to-end timing diagnostics

## Summary

Measure one Original → Relay → Update flow across the Android app and Go Backend. Use the existing private event record and helper to show the breakdown; no telemetry service, OpenTelemetry dependency, or network export.

## Implementation

- Give each Original a unique flow ID. Carry it through its Relay, Backend request, Update or skip, and timing records. This also prevents a late result from being attributed to a newer Original that reuses the same notification key.
- On the phone, measure with a monotonic clock: each Preview translation operation, each Lark HTTP attempt (including token refresh and retries), time to Relay, the Backend round trip, any Full-text fallback, and time to Update or skip. Record duration, operation name, and success/failure—never tokens, request bodies, or response text in timing data.
- In the Backend, measure Lookup stages and each outgoing Lark SDK HTTP attempt, including SDK token requests. Return the per-call durations with a successful Lookup response so the phone can save the complete flow under one ID. Keep the Backend’s existing outcome logs; log timing data for Backend failures that cannot be returned to the phone.
- Add `tools/larklish-helper events timing` to show the stages and total duration for a selected flow, with a machine-readable option. Explain that timing starts when Larklish receives the Original; it cannot measure time before Android delivers that notification.

## Tests and acceptance

- Test flow-ID pairing when the same notification key is reposted, successful and failed HTTP attempts, retries, token refresh, skipped uncut Previews, Backend misses, and phone fallback.
- Verify that stage durations are nonnegative, totals are coherent, older event files still work, and timing output contains no credentials or message text.
- Run Go and Android tests, formatting checks, and helper tests. If the Pixel is available, use a cut-Preview probe to confirm one readable end-to-end timing breakdown; report that live check separately if the device is unavailable.

## Assumptions

- “Each part” means the flow observable from the app, not Lark’s own delivery time.
- Existing private `events.jsonl` plus the helper is the simplest useful place to see timings. No deployment or production cutover is included.

## Additions requested during implementation

- Measure on-device translation time explicitly, including ML Kit work as well as Lark calls.
- Measure the gap from receipt of the Original to the first Relay, including work outside translation.
- Log the same Backend timing breakdown on the Backend for remote soaking, including successful Lookups.
