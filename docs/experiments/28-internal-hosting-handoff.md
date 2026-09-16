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

### Second route ticket submitted — September 16 release window

The scheduled continuation fired at 16:00:00 UTC. At 09:00 PDT the Classic
route's Modify control was available. The existing exact route was edited; no
second public path was created. The route editor's HEADER MODIFY component could
not load: its `feature_detail/17` request timed out while managed Chrome reported
Local Network Access denied for `cloud.tiktok-row.net`. A read-only authenticated
API check returned that feature successfully, so this was a browser permission/
network-path problem, not an observed NetLink policy denial. No browser was
manually launched and no policy exception was requested.

The supported route-level Nginx directive editor was used for the same header
overwrite: `more_set_input_headers "x-tt-env: ppe_deploy_i18n_1";`. The rewrite
editor was set to PATH `SET /lookup` with next action `break`. Before submitting,
the route's draft view showed both settings on only
`=/_/test/demo/larklish/lookup`. The console did not offer a generated full diff
before its `Create Ticket` action; that limitation was observed rather than
claiming a pre-submission diff review.

Created normal reviewed NetLink business ticket
[379619](https://cloud.tiktok-row.net/netlink/v2/main/business/ticket/379619?ti_business_id=10071827),
underlying NetLink ticket `326868`, TLB ticket `1261561`, TLB flow `340414`.
The generated TLB diff was read before any deployment action. It contains only:

1. A `rewrite_rules` entry for the exact route: `PATH_SET` to `/lookup`, `break`.
2. That route's `plaintext_directives` change from empty to the one
   `more_set_input_headers` directive above; no other location or service changes.
3. The expected servername version bump from `1260366` to `1261561`.

There are no DNS, PSM/cluster, Authorization or sibling-route changes in the
generated diff. The workflow is again review → canary →
canary DQ → full → full DQ → close. Business review is pending with
`wangchen.iven`; Max's Approve action is disabled. No reviewer was changed or
messaged. The ordinary public path still returned the expected plain 404 before
review. A temporary canary-configured Android build passed ktfmt, unit tests and
`assembleDebug`; the `larklish.backendTlbCanary` property and request-header
code must be removed again after this ticket reaches 100%. ADB showed no
connected device at the start of this continuation, so the phone remains on the
working Railway build.

Read-only direct PPE preflight (before the second ticket's review completed):
`POST /lookup` with `x-tt-env: ppe_deploy_i18n_1` returned 401 for missing and
wrong Bearer tokens; the configured token passed authentication and returned
400 for an intentionally empty Lookup payload. This confirms authenticated PPE
is still running, but does not yet prove the public TLB route, header overwrite,
or a real Lookup. `tools/larklish-helper backend lookup --help` shows saved
Original input through `--file`, but it also needs a current phone user token;
the Pixel was disconnected at this point.

### Authenticated production requested — September 16

Max explicitly asked to deploy the authenticated Backend to Singapore production
now, superseding this experiment's earlier deferral of the production milestone.
The normal Bits workflow was inspected before acting. Production function
`kpb2dvsn` / `faas-sg` still runs revision `yl6hk8n4io` (`1.0.2`) from older SCM
`1.0.0.11`; authenticated PPE function `mmyp0srw` runs revision `qpu3xqeitk`
(`1.0.5`) from SCM `1.0.0.13`. Production retains MY instance limit 1/1 and
other IDCs 0/0. No production code change has yet been made.

Bits development task `2844150` and associated release ticket `1229200073986`
are the existing authenticated-code workflow. Its self-test pipeline had passed,
but the Develop-stage GEC quality gate awaited five manual declarations. Max
confirmed that DECC marking and ROW↔TTP data sharing do not apply to this demo.
The Backend code has no TCC or RDS integration and this deployment targets only
Singapore. The five items were each recorded as **Not involved** with a separate
reason; `Pass All`, skip and force controls were not used. GEC then reported
`passFlag: true` with two non-blocking automated warnings for no matching test
plan. Bits enabled its normal **Complete development** action, which advanced the
task to Test and started the ordinary testing pipeline. That pipeline succeeded.

Test-stage GEC then blocked Merge on **Nario Scenario Coverage Rate**. QCSS report
`2331366` says it cannot resolve the worker ID to a PSM and commit ID. A normal
retry of only Nario reproduced the error. A normal full GEC recheck after the
pipeline and the two Test-stage manual declarations were complete generated a
new worker ID (`01M2NMGXDKP3KVT1XF54KYPHKQ@1@7`) and failed with the same
error. Both Test-stage manual items (DECC and TLB configuration) were recorded
as Not involved with the demo/data-scope evidence; pending manual count is zero.
The remaining blocked item is Nario's platform metadata lookup, not a failing
Lookup test. This is the same class of issue previously handled by a separately
approved GEC disposition for older development task `2820325`; that old approval
has not carried over to the new task. No check was skipped or force-completed.
Release ticket `1229200073986` still has zero change items and production still
runs the old code. The prior release ticket `1225461564162` separately retains
its final `SCM info not found in pipeline` failure. No general Single Release
workflow is listed in this Bits workspace, so a template shortcut was not used.

If authenticated production eventually deploys and its missing/wrong/correct
Bearer behavior is verified, the public route no longer needs to force PPE for
authentication. It will still need the `/lookup` path rewrite. Pending NetLink
ticket `379619` currently includes the PPE overwrite and remains in business
review; production deployment alone will **not** change that ticket. After
production is proven, inspect/cancel or supersede its PPE directive through a
new reviewed change before treating the public route as production-backed.

### Phone rollback checkpoint

Max prefers rebuilding the Railway Android app from source over saving an APK
before the internal-host trial. The recorded, verified Railway deployment source
commit is `cea339e848d460d48cd82bf7945a4875be09bec3` (Experiment 26). The
tracked `app/` tree is identical at committed checkpoint
`7840e57e810316d8af164a60ab35eb94cda41234`; subsequent temporary canary
edits are uncommitted and are not part of either checkpoint.

If the phone needs to return to Railway, build the checkpoint in a separate
worktree, set the ignored `larklish.backendUrl` to
`https://backend-production-a712b.up.railway.app`, keep the existing Backend
token and other private build inputs, and omit the temporary canary/PPE headers.
Install the rebuilt APK in place and verify a Railway Lookup. Do not rewrite
the active branch's history just to roll back the phone. This source checkpoint
does not contain ignored `local.properties` or the local signing key, which must
still be available for the rebuild.

### Backend-alias plan started — September 16

The [PPE-to-production plan](../bytedance-ppe-to-production-plan.md) supersedes
the rewrite/PPE-header ticket approach documented above. NetLink business ticket
`379619` was still in business review with the TLB stage waiting. Its Cancel
action succeeded; after a browser reload the ticket read `canceled`, with all
stages canceled. Read-only NetLink `get-path-config` then confirmed that the
already-deployed exact route is still version `1260366`, targets
`coplan.lark.larklish` in `faas-sg`, and has no plaintext directive. A public
`POST` without PPE selection still reached that PSM and returned the expected
plain 404 before the new Backend code was deployed.

The Go Backend now registers only the prefixed `POST` Lookup alias in addition
to its existing path; it uses the same Bearer-protected handler. Tests cover
missing, wrong and correct Backend Bearer tokens, non-POST access, and excluded
prefixed `/chats` and `/v1/ping`. The Android source now sends `x-tt-env` from
optional `larklish.backendTtEnv` and no longer contains the temporary TLB
canary switch. The ignored local build setting selects `ppe_deploy_i18n_1`;
the public host and shared token are unchanged. The phone still has the working
Railway installation; no APK was installed during this step.

Go tests, `go vet`, Backend build, Android unit tests, `ktfmtCheck`, APK build,
and 26 Python helper tests passed. `TestReplayCorpus` skipped because this
checkout has no `replay-corpus` directory, not because of a new failure. The
ByteDance `origin/main` branch was fetched and is an ancestor of local `main`;
its 13-commit gap consists of the previously planned 12 commits plus the
new documentation-plan commit. The Backend changes in that gap were audited:
routine logging moved to stdout and the candidate timestamp field was renamed
without changing Lookup behavior. No source push or SCM/PPE release has yet
occurred.

### Public PPE acceptance and Pixel trial

Committed the tested alias/app change as `93d5b3af3306dd7f414c5ba08ed7ab4a8df8641d`
and fast-forwarded **ByteDance** `origin/main` only. GitHub `main` remained at
`7840e57e810316d8af164a60ab35eb94cda41234`, so Railway did not
auto-redeploy. SCM Git trigger built that exact commit successfully as
`oec/seller/larklish:1.0.0.19` (version ID `165680146`).

Bits development task `2850411`, titled “Larklish public Lookup alias PPE,”
was created from task `2844150` with the new SCM version pinned, PPE enabled,
and BOE disabled. Bits automatically associated release ticket `1229776511490`;
no production release action was taken. Its ordinary self-test pipeline
succeeded, deploying PPE function `mmyp0srw` revision `c45ojbhrt6` (`1.0.7`)
from SCM `1.0.0.19` to `faas-sg`.

For the direct gateway **and** public exact route, `POST` with
`x-tt-env: ppe_deploy_i18n_1` returned 401 with missing or wrong Backend Bearer
tokens and 400 with the correct token and intentionally incomplete payload.
The public path without the PPE header, or with a mistyped lane, returned 404.
Prefixed `/chats` and `/v1/ping` also returned 404. A recorded Original from
`2026-09-16T16:50:05.866750Z` with the phone's valid user token returned
HTTP 200, `found` (`post`), English available, 167 Full-text characters,
SHA-256 prefix `5237fe9efec3`, in **2.364 seconds**. Neither token nor text
was printed.

The phone's user access token was near expiry, so its existing `refresh`
debug hook refreshed the normal saved chain before acceptance. Before install,
the saved-token file's SHA-256 was
`cfc4f4b143a3820c21af44aa1011547be0212fb44147bb4e7e8fac01a6523d11`;
the Recorder had 2,733 events and the listener was bound. The new APK's signing
certificate SHA-256 matched the documented Railway debug build:
`cd5b25f8a55c9250d8502b702b02a3618174852e401f9bb6014a61fc13c7eaa4`.
`larklish-helper phone install` succeeded in place and verified that the saved
token was preserved byte-for-byte, all 2,733 events remained, and the listener
rebound. The installed app's own Lookup hook returned `found` through the public
ByteDance host on both Wi-Fi and cellular, with VPN off. Wi-Fi was restored.

A normal test-group probe did **not** produce a matching Relay: Lark had been
stopped, and after launch it opened `NoPermissionActivity`. The listener was
bound and the app-side Lookup hook worked, so this is an observed
Original-delivery blocker, not evidence of a Backend failure. Full
Relay-to-Update timing on the new host remains unverified. No Railway rollback
was needed.

The new task's Test pipeline succeeded. Test-stage GEC blocked on **Nario
Scenario Coverage Rate**: QCSS report `2331436` says worker
`01M2NTFGB059K1PYWF06XFCCMZ@1@7` cannot be mapped to a PSM and commit ID,
the same platform-metadata error as the older task. Its two Test-stage manual
items were each marked **Not involved** with separate remarks: this demo needs
no DECC marking, and this Backend code release changes no TLB configuration.
The manual pending count reached zero. One normal retry of only the Nario item
finished with the same worker-to-PSM/commit mapping error. Bits GEC remains
`CheckStatusFailed` with `passFlag: false`; Merge is unavailable. Skip and
Force Test Out were not used. The alias-bearing production release remains
gated. The task is
[2850411](https://bits.bytedance.net/devops/470900839426/develop/detail/2850411/flow?devops_space_type=server_fe),
and its automatically associated release ticket is `1229776511490`.

The new Bits task's Develop-stage GEC report had five manual items. Each was
recorded individually as **Not involved** with a reason: this is one Singapore
demo with no multi-region deployment requirement, DECC marking, TCC switch,
ROW↔TTP sharing, or RDS schema change. GEC then showed 9 passed, 2 non-blocking
test-plan warnings, 0 pending, 0 skipped. Its normal **Complete development**
action advanced task `2850411` to Test, where the normal pipeline and GEC are
running. The older production task `2844150` still has the non-skippable Nario
worker-to-commit failure, and production function `kpb2dvsn` still runs SCM
`1.0.0.11` revision `1.0.2`. No production cutover occurred.

### Later Original-delivery retry

After reopening Lark, a second synthetic test-group message **did** produce an
Original and English Relay on Wi-Fi with VPN off. Recorder marked it
`skipped not-truncated`: Lark supplied the complete Preview, so no Backend
Update was necessary. A third, deliberately long test-group message also
produced an English Relay over cellular with VPN off and the same correct
`not-truncated` result. Its length did not make Lark truncate this bot
notification. Wi-Fi was restored after the cellular probe. Thus the earlier
no-Relay probe was transient; notification interception works on both networks,
but these bot messages still do not measure full Relay-to-Update timing.
