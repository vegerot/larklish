# 🗺️ Larklish — Current plan

Larklish helps Max understand and act on Lark notifications quickly. Support the Pixel 4a
running Android 16 (minimum SDK 36). [CONTEXT.md](../CONTEXT.md) defines the project terms.
Read [progress.md](progress.md) from the bottom for dated state and the latest handoff.

## Working rules

- Grow in small experimental layers. Validate important uncertainty on the phone.
- Keep changes scoped and remove obsolete implementations instead of adding compatibility.
- Record decisions/evidence in experiments and append a concise progress entry with `Next:`.
- Check `sl status` and the diff before work and after context compaction.
- Stop at the end of each requested layer. Commit completed layers; app changes must build
  and install successfully. Test-only and documentation changes need their relevant checks.
- Keep credentials and real notification records out of version control.

## Current behavior

1. Lark posts an **Original**. Its **Preview** has a group title and usually `Sender: message`.
2. The phone translates Han text with **ML Kit** and posts the first English **Relay**.
   Complete Latin fields pass through. Bare Chinese personal names are romanized.
3. A complete Preview whose message has no Han needs no Backend call, even if its title
   or Sender has Han. Its early Relay remains final.
4. A complete Preview with a Han message goes to the Backend for Lark translation directly.
5. A Preview ending in `...` goes to the Backend for **Lookup** and Full-text translation,
   even when its visible text is entirely Latin. Only this branch needs a user access token.
6. A successful Backend result **Updates** the existing Relay. On Lookup or message
   translation failure, the first Relay remains. A title/Sender translation failure retains
   that field's first wording. There is no phone-side Full-text translation fallback.

The phone preserves the Original's PendingIntent for taps. It uses the Original's key
as the Relay tag; newer Originals on that key cancel older pending Updates. Debug builds
retain the Original for comparison; release builds cancel it. The removal handler cancels
Relays when Lark withdraws Originals and ignores Larklish's own cancellation.

ML Kit and Lookup both survived bounded deletion experiments:
[31 — Lookup](experiments/31-lookup-deletion.md), [33 — early Relay](experiments/33-early-relay-deletion.md).
These are small observational cohorts, not guarantees of translation quality or reliability.

## Required deployment paths

**Both internal ByteCloud and external Railway deployments are required by Max
(2026-09-28).** Maintain one Go Backend implementation and request contract for both.
Neither deployment is an obsolete alternative or a candidate for removal merely because
both exist. This requirement does not imply automatic failover between them.

The phone has a device-local HTTPS Backend URL; changing it requires no rebuild.
The compiled default is the internal public alias. The last verified selected phone URL
was Railway; consult progress and inspect settings before any deployment or cutover.

| Path | Operation | Last recorded acceptance |
| --- | --- | --- |
| Railway (external) | GitHub main → service rooted at backend/; provider-managed HTTPS | Work-profile Original → Relay → Full-text Update, September 24 |
| ByteCloud (internal) | ByteDance source → SCM artifact → ByteFaaS via Bits; separate PPE and production stages | Replacement PPE deployment and phone flow verified September 24; production promotion still gated |

PPE means the pre-production environment. Production promotion uses the normal Bits
checks; PPE success does not establish production acceptance. Recheck task/function state
before acting: earlier PPE functions and release tasks in old plans have been superseded.
Keep GitHub and ByteDance source histories aligned for subsequent Backend changes.
A push can trigger deployment; choose the destination deliberately.

Detailed deployment records: [Railway](experiments/26-railway-deployment.md),
[Work profile and recovery](experiments/30-work-profile.md), and the dated progress entries.
The earlier [ByteCloud promotion plan](bytedance-ppe-to-production-plan.md) is historical
rollout context; its task IDs and checkpoints are not current execution instructions.

## Mechanisms and boundaries

- Lark package: `com.larksuite.suite`; Larklish: `com.vegerot.larklish`.
- Lark is in Work user 10; Larklish is in personal user 0. Cross-profile notification
  access requires the AirWatch listener allowlist and Work-profile Lark notifications.
  Root inspection is available with `adb shell 'su -c "..."'` when ADB root is authorized.
- Backend routes use Bearer authentication for `/lookup` and `/chats`; `/v1/ping` is public.
  The internal alias exposes the prefixed Lookup route. Android requires HTTPS.
- The Backend uses standard Go HTTP plus the official Lark SDK. Its chat cache is in memory.
  The phone supplies its user access token for each Full-text Lookup; the Backend does not
  own a persistent user session.
- The phone owns the rotating user-token file. Refresh tokens are single-use. Preserve
  that file across installs; copying/reseeding a token can disrupt another refresh chain.
- Lookup supports text and post messages. Cards, images and other types are not Full-text
  implementations. Matching handles observed Preview differences; it remains heuristic.
- Recorder events and timing spans share a flow ID. Use exact flow IDs for new analyses.
  Timing starts at Original receipt; it cannot measure Lark's earlier delivery delay.

## Checks and everyday commands

```sh
sl status
./gradlew testDebugUnitTest
./gradlew ktfmtFormat
uvx --system-certs ruff format tools
go -C backend test ./...
python3 -m unittest discover -s tools
./tools/larklish-helper phone status --details
./tools/larklish-helper events stats
./tools/larklish-helper phone install
```

Prefer Android Studio MCP when convenient. The helper is plain Python; only `token` and
`showcase` need its extra dependencies. Import helper functions for one-off investigations.
The install helper verifies token-file preservation. Do not clear app data to update it.

[Experiment 32](experiments/32-replay-gate.md) defines the replay gate: portable fictional
fixtures assert message identity/text or expected skips. Private corpus resolution counts
are diagnostic, not a percentage-based correctness gate. Real messages stay ignored.

## Shortcuts

This is a single-user app. App credentials, the user-token seed and the shared Backend
Bearer token are embedded in the APK from ignored local.properties. There is no sign-in
screen or per-user Backend authorization. Wider distribution requires revisiting those
choices. ByteCloud production acceptance remains separate from PPE acceptance.

## Next work

- Preserve both deployment paths and resolve internal production promotion through Bits.
- Test narrow translation-fidelity improvements against actual ML Kit results before
  adding parsing or masking machinery. Preserve useful early English.
- Investigate timing inconsistencies before claiming precise latency savings.
- Add other message types only after measuring the missing value in real traffic.

Earlier layer plans, alternatives and deferred feature proposals are retained in
[the historical build plan](plan-history-2026-09-28.md). They are evidence and ideas,
not automatic requirements. Add work here only when it serves a current user need.
