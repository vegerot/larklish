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
Live IAM policy details on September 16 confirmed working-day windows of 09–12,
13–18 and 19–22 in the user's time zone. Verify production authentication before
directing any public route to production. Do not skip or force-complete checks.
No new exemption or external approval is known to be necessary, but the remaining
workflow has not been fully inspected.

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

## Devbox execution — 2026-09-16

### Rechecked state

- GitHub `main` is at handoff commit `ddfdd93`; the working copy started clean.
  Sapling is not installed on this devbox, so Git was used for the working-copy
  check. `bytedcli auth status` is authenticated through ByteCloud Auth.
- Live NetLink API Service Management now lists `coplan.lark.larklish` under
  `TTS_Global_Shop`, owned by `max.coplan`. The old registration is therefore
  effective. `bytedcli netlink business-ticket get --ticket-id 378704` is not a
  valid check for this record: ticket numbers collide across business contexts,
  and the command drops the URL's `ti_business_id`, returning an unrelated CN
  ticket. The business-scoped console is the evidence used here.
- Authenticated PPE function `mmyp0srw` remains deployed in `faas-sg`, lane
  `ppe_deploy_i18n_1`, at SCM `1.0.0.13`. Production function `kpb2dvsn`
  remains on older SCM `1.0.0.11`.
- Consul trigger `e8a78dl9` belongs to production function `kpb2dvsn`, not the
  separate PPE function. Fresh readback confirms `larklish-tlb-consul` is
  enabled, ready and metadata-synced for PSM `coplan.lark.larklish`. The normal
  Consul path reaches PPE later through a forced environment header.
- No public route existed at the start. NetLink returned
  `NETLINK_PATH_NOT_FOUND`, and a public POST to the intended URL returned 404.

### API definition and route shape

- Created one API definition only: API service ID `127961`, API ID `1912054`,
  name `Lookup`, method `POST`. It is annotated as an App API, owned by
  `max.coplan`, with the existing application-scenario description. `/chats`
  and `/v1/ping` were not added.
- The final published API path is `/_/test/demo/larklish/lookup`. The console's
  API-release mode publishes the API path directly and does not expose rewrite
  or request-header controls. Classic mode exposes those controls, but blocks a
  PSM until its API route has first been published. The normal safe workflow is
  therefore two reviewed tickets:

  1. publish the exact prefixed API path and register the TLB Backend;
  2. modify that route to rewrite PATH to `/lookup` and overwrite `x-tt-env`
     with `ppe_deploy_i18n_1`.
- This split does not temporarily expose the older production Backend. Both the
  production and PPE direct gateways return 404 for the prefixed path before the
  rewrite exists. The first ticket has no rewrite or environment directive, so
  its route remains non-functional until the second reviewed change.

### First route ticket submitted

Submitted the normal reviewed configuration; no auto-deploy, exemption, reviewer
message or skipped check was used.

| Item | Value |
| --- | --- |
| NetLink business ticket | `378806` |
| TLB ticket | `1260366` |
| Underlying NetLink ticket / flow | `326501` / `340141` |
| Route | exact match `=/_/test/demo/larklish/lookup` |
| Backend | `coplan.lark.larklish` / `faas-sg`, HTTP |
| TLB service discovery | Consul; SG1 weight 100, MY/MY2/MY3 weight 0 |
| TLB service load balancing | existing default hash configuration |
| Submitted reviewer | `wangchen.iven` |
| Current state | finished; all TLB stages succeeded |

The complete ticket diff adds only the new TLB service and the one exact-match
location, and advances servername version `1051155` to ticket version `1260366`.
It does not change DNS or any existing location. The generated TLB workflow is:

`review` → `canary` → `canary.dq` → `full` → `full.dq` → `close`

The business-scoped NetLink wrapper further resolved the review stage to
`business review` followed by `resources ownership review`. Business review was
assigned to `wangchen.iven`. The Approve button was disabled for Max's session at
that stage, even though Max has TLB access. The normal reviewers subsequently
completed the review; no reviewer was changed and no gate was bypassed. The
ticket URL is:

<https://cloud.tiktok-row.net/netlink/v2/main/business/ticket/378806?ti_business_id=10071827&x-bc-region-id=bytedance>

At submission time the route was still absent from live configuration and the
public URL still returned the TLB HTML 404. Railway and the phone were unchanged.

### First route deployed

The normal workflow finished without a bypass or skipped quality check:

| Stage | Result / UTC time |
| --- | --- |
| Review | succeeded at 04:23:42 |
| Canary | four canary pods succeeded at 04:26:20 |
| Canary quality inspection | succeeded at 04:32:09 |
| Full release | 0% for 15 minutes, 10% for 10 minutes, 30% for 5 minutes, then 100%; all 530 pods succeeded at 05:10:01 |
| Full quality inspection | succeeded at 05:16:04 |
| Close | succeeded at 05:17:00 |

`x-tlb-canary: 1` became usable after the canary stage succeeded. Before the
full release it selected the four canary pods: the response changed from the
existing TLB HTML 404 to the Backend's plain Go 404 and included
`x-gw-dst-psm: coplan.lark.larklish`. After the full release, both ordinary and
canary requests reach that PSM and return the same expected plain 404. This
proves the exact route and service discovery are live while also confirming that
the prefixed path remains inert without the planned rewrite.

### Second ticket deferred outside the release window

The route appears in Access Management as an exact-match API route. At 22:38 PDT
on Tuesday, its Modify action and `Create Policy` → `Classic mode` were disabled
with the generic message `unsupported platform` and an exception link for
`ti_platform.config.operate` (`node_id=2138126`). That message initially looked
like a missing standing permission, but the linked IAM policy details show that
it is NetLink's production release-time-window policy:

- `ti_platform.config.operate` is the generic config create/read/update/delete
  operation (`allow to crud config`), not a special TLB entitlement.
- The same policy also controls `ti_platform.config.create_path`,
  `tlb.service.update` and `ti_netlink.namespace.operate` when
  `request.env == "prod"`.
- On working days, normal publishing is allowed 09:00–11:59:59,
  13:00–17:59:59 and 19:00–21:59:59 in the user's time zone. The policy blocks
  all other working-day times and all non-working days.

No exception was requested. We can create or modify a Classic-mode route in the
next normal window. API-release mode remained available, but it does not expose
PATH rewrite or request-header overwrite controls. The first route is safe to
leave deployed because it still returns 404 and neither exposes the older
production handler nor changes another route.

### App preparation while the route deployed

- `adb devices -l` found the connected Pixel 4a in `device` state.
- The Backend host was already easy to configure at build time through the
  ignored `larklish.backendUrl` entry in `local.properties`. The devbox value was
  changed to `https://shop.tiktokglobalshop.com/_/test/demo/larklish`; no source
  change was needed for host selection.
- A temporary `larklish.backendTlbCanary` BuildConfig switch and
  `x-tlb-canary: 1` request header were built successfully while the TLB release
  was in progress. Once the first release reached 100%, both the temporary
  property and its source code were removed as requested. The non-canary build
  then passed `ktfmtFormat`, Android unit tests and `assembleDebug`.
- The internal-host APK was not installed. The endpoint intentionally returns
  404 until the rewrite/PPE problem is resolved, so installing it would replace
  the working Railway configuration with a known-broken Backend.

### Scheduled continuation

The release-window gate is time-based, so no exception is necessary. A persistent
user-systemd timer, `codex-larklish-20260916.timer`, is active and scheduled for
September 16 at 16:00 UTC / 09:00 PDT with one-second accuracy. Its one-shot
service queues a continuation into this same Codex task through the running App
Server. The wakeup script successfully verified App Server version `0.154.0` and
writes a receipt before allowing any duplicate delivery.

This reuses the mechanism that successfully resumed the September 15 task at its
exact release-window boundary. An initial raw shell sleep was stopped after the
timer was verified. A persistent Codex goal retains the complete objective, but
it was marked temporarily blocked on the external time gate so automatic goal
continuations do not consume work while nothing can legally change. The queued
message will resume the task when the timer fires. Timer and wakeup files are
local operational state outside the repository.

### Resume here

1. During the next normal working-day release window, open Classic mode again.
   Prefer modifying the existing exact route so the public URL stays unchanged.
   Configure PATH `SET /lookup`, next action `break`, and request header
   `x-tt-env` overwritten to `ppe_deploy_i18n_1`; preserve `Authorization`.
2. A new Classic route is also possible in that window, but it must use a
   different matcher because the existing exact path already occupies its
   matcher. A new API-release route would not solve the problem because it still
   lacks the rewrite and forced-header controls.
3. Inspect the resulting diff for only the intended location directives. Complete
   its full normal workflow, use `x-tlb-canary` only during its rollout, and
   remove the temporary header/property again at 100%.
4. Only after the public PPE endpoint works, install the internal-host build and
   run missing/wrong/correct Bearer plus recorded-Original acceptance checks.
5. Keep the production Bits milestone deferred until public PPE succeeds.
