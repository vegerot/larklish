# Internal hosting exploration — devbox handoff

## Goal and scope

Determine what remains to expose the authenticated ByteDance-hosted Backend over
public HTTPS, complete the normal workflow where possible, and measure a real
Lookup. Discover actual remaining approval gates rather than assuming either
that access is sufficient or that more exemptions will be necessary.

Max likes Railway and has not selected a migration. Railway stays the active
demo Backend. Do not change the phone's Backend URL or redeploy Railway for this
experiment. Start with the existing authenticated Singapore PPE deployment;
track authenticated production deployment as a separate milestone. Stop after
reporting results and remaining work so Max can decide whether to continue.

## Progress and evidence

- Live browser inspection confirmed [NetLink ticket 378704](https://cloud.tiktok-row.net/netlink/v2/main/business/ticket/378704?ti_business_id=10071827&x-bc-region-id=bytedance)
  is `finished`: review, create API service and close all succeeded. It registers
  `coplan.lark.larklish`. The displayed last update was September 16, 2026,
  08:53:58 UTC+8 (September 15, 17:53:58 PDT).
- This registers the API service; it does not publish a public route. Earlier
  registration-review blockers in Experiment 21 are now resolved.
- Max states he has TLB access and can approve NetLink tickets changing TLB
  configuration. Record and follow each generated ticket's actual workflow;
  do not treat this statement as evidence that all other gates are absent.
- No additional external approval blocker is currently known. A same-domain
  route ticket previously inspected followed review → canary → full → close.
  The new Larklish route ticket has not been created or inspected.
- Railway passed the recorded public authentication, Lookup, restart, idle-cache
  and cellular acceptance checks. Cellular Relay-to-Update measurements were
  4.739 s and 4.531 s. See [Experiment 26](26-railway-deployment.md). These are
  historical results, not a fresh health check or a controlled provider comparison.
- No internal infrastructure change was made during this handoff preparation.

## Starting infrastructure state

Except for the ticket completion above, these are the last recorded values from
[Experiment 21](21-public-ppe-route.md), to recheck on devbox before mutation.

| Item | Last recorded state |
| --- | --- |
| PSM | `coplan.lark.larklish` |
| Authenticated source | `952abda5d0965ae29ad84ed6b4ced8a8ea168222` on ByteDance origin |
| SCM artifact | `oec/seller/larklish:1.0.0.13`, version ID `165403130` |
| PPE | Function `mmyp0srw`, cluster `faas-sg`, lane `ppe_deploy_i18n_1` |
| Successful PPE run | `1230600871170` |
| Direct gateway | `https://kpb2dvsn.sg-fn.tiktok-row.net` |
| Consul trigger | `e8a78dl9`, `larklish-tlb-consul`, enabled/ready/synced |
| Domain and temporary prefix | `https://shop.tiktokglobalshop.com/_/test/demo/larklish` |
| NetLink business / namespace | `TTS_Global_Shop`, `10071827` / `125` |
| Servername / TLB cluster | `2955` / `tiktok_ecom_lb_alisg_v3` |
| TLB Backend | Prior local draft only: Consul, `faas-sg`, SG1 weight 100, other locations 0 |
| Public route | Not published in the last inspection |
| Production | Older unauthenticated code; configured token alone does not enforce authentication |

Use FaaS site `i18n-tt`, region `Singapore-Central`. Earlier direct gateway tests
needed `x-tt-env: ppe_deploy_i18n_1`; they did not need `x-use-ppe`. Verify the
TLB path independently. The old browser draft may no longer exist.

## Steps on devbox

1. Read `AGENTS.md`, `CONTEXT.md`, `docs/plan.md`, the latest progress entries and
   this handoff. Inspect Sapling Smartlog/status and preserve pending work.
   Recheck authenticated CLI/browser access and the actual resource state above.
   Rediscover browser tabs rather than reusing old page IDs. Use `bytedcli` for
   ByteDance tooling and inspect its current help before choosing commands.
2. Confirm the completed API service appears in API Management. Inspect the
   required interface definitions/annotations and TLB Backend registration.
   Prepare the smallest public surface: `POST /lookup` only. The phone does not
   need public `/chats`; direct gateway health checks can use `/v1/ping`.
3. Register the Backend with Consul discovery and prepare the temporary route.
   Strip `/_/test/demo/larklish`, preserve `Authorization`, and pin forwarding
   to authenticated PPE. Omitted or caller-supplied environment headers must not
   route this public path to the older unauthenticated production build.
4. Inspect the complete diff and generated workflow. Preserve unrelated routes
   and DNS. Record ticket IDs, actual approval stages and any newly discovered
   blockers. Normal review-ticket submission is authorized. Max can handle TLB
   approvals; surface the concrete ticket if his action is required. Do not
   bypass checks, request policy exemptions or message reviewers on his behalf.
5. Complete the normal canary/full release stages that the ticket requires.
   `x-tlb-canary: 1` selects already deployed canary configuration; it neither
   deploys a draft nor authenticates requests.
6. Verify public HTTPS and missing/wrong/correct Bearer behavior. Confirm prefix
   rewriting, PPE pinning despite absent/changed client headers, and that excluded
   paths do not expose Backend endpoints. Replay an existing recorded synthetic
   Original with its valid user token; record outcome, Full-text fingerprint,
   translation availability and duration without printing credentials or text.
   Inspect `tools/larklish-helper backend lookup --help` for URL selection and
   existing report support; import helper functions for one-off gaps. Do not
   change the phone's saved configuration or send fresh messages for this test.
7. If the public PPE experiment succeeds, investigate the separate production
   milestone below through normal Bits. Record any new approval requirement as
   observed, not presumed. If blocked, preserve the working PPE result and report
   the exact blocker rather than expanding into unrelated platform changes.
8. Append each action, command, result and decision to this experiment and add a
   concise progress entry. Report reachability, authentication, Lookup timings,
   operational effort, approval gates and remaining work. A migration decision
   and phone cutover are outside this experiment.

## Separate production milestone

Authenticated release: Bits task `2844150`, release `1229200073986`. The older
release `1225461564162` has pipeline/run `1229186479874` / `1229972423170`, final
job `3261393118`, failing with `SCM info not found in pipeline`. The prior retry
reproduced it despite SCM output containing the expected artifact and commit.
This is a technical failure distinct from the already approved Nario exemption.
Recheck current status and the new release's actual gates before acting.

Use a permitted deployment window instead of requesting a time-window exemption.
Previously verified working-day windows in `America/Los_Angeles` were 09–12,
13–18 and 19–22; recheck IAM for the operation. Verify production authentication
before directing any public route to production. Do not skip or force-complete
checks. No new exemption or external approval is known to be necessary, but the
remaining workflow has not been fully inspected.

## Credentials, workspace and handoff boundaries

- Reuse the existing Backend token without printing or rotating it. Secrets stay
  in ignored configuration; preserve the phone's user-token chain. If devbox lacks
  required credentials or recorded input, establish access without copying secrets
  into these documents. Expired user tokens are a test prerequisite to resolve
  through the existing app flow, not an infrastructure approval.
- ByteDance `origin` and GitHub are distinct remotes. Inspect their current values
  before any source publication. Max subsequently authorized committing and
  pushing this handoff and the pending helper changes to GitHub; this does not
  authorize publishing a new internal Backend release from devbox.
- Pending helper work exists in `tools/larklish_helper.py`,
  `tools/test_larklish_helper.py`, Experiment 27 and `docs/progress.md`: stdout-only
  probe/Lookup JSON and a pure thread-correlation test, with 26 tests previously
  passing. These changes are included with the handoff in the authorized commit;
  ensure devbox has that commit before continuing.
- Run commands directly; Max withdrew the requirement to run everything in tmux.
- This handoff records the plan only. Infrastructure execution begins on devbox.
