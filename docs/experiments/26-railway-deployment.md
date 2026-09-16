# Railway deployment execution log — 2026-09-15

Implements [the deployment plan](../railway-deployment-plan.md). Times below are
UTC. Credentials and message contents are omitted. This log records actions,
results, failures and changes to the plan as execution proceeds.

## Target

- GitHub `vegerot/larklish`, branch `main`; service root `/backend`.
- Railway project `larklish`, service `backend`, environment `production`.
- One always-running US West (`us-west2`) replica; Trial's fixed maximum of
  2 vCPU / 1 GB (adjusted from the initial paid-plan limits in step 8).
- Managed HTTPS; existing in-memory cache and existing authentication token.
- Verify the public Backend, then the Pixel on cellular without Wi-Fi or VPN.

## Live service

| Item | Verified value |
| --- | --- |
| Public Backend | `https://backend-production-a712b.up.railway.app` |
| Project | `3e575a03-cce3-4c69-bf27-a5362c558c31` |
| Service | `8c497dbc-3a74-449c-b514-bce866ba1d9d` |
| Production environment | `20f0c4ca-4be7-48bc-8d7d-e5fde104ff52` |
| Deployment | `46f79f2e-3b74-4e90-816b-5356a9e8e793` |
| Deployed source commit | `cea339e848d460d48cd82bf7945a4875be09bec3` |
| Release trigger | GitHub `main`, watch path `/backend/**` |
| Build root and builder | `/backend`, Railpack |
| Region and replicas | `us-west2`, one replica |
| Runtime and API host | Go 1.25.14, Linux/amd64; `https://open.larksuite.com` |
| Sleep and restart | Serverless off; `ON_FAILURE`, 10 retries (Trial) |
| Health check | `/v1/ping`, 60-second startup timeout |

[Open the Railway service](https://railway.com/project/3e575a03-cce3-4c69-bf27-a5362c558c31/service/8c497dbc-3a74-449c-b514-bce866ba1d9d?environmentId=20f0c4ca-4be7-48bc-8d7d-e5fde104ff52).

## Steps

### 1. 22:35–22:37 — inspect the starting state

- Read the deployment plan, latest progress, Go module and Railway skill
  references for setup, deployment, configuration and operation.
- `sl smartlog` and `sl status`: started at `43cb3ea4a94f`, with a clean working
  copy. `github/main` was at `c01e480d8a86`; Codebase was at `35f9cc8ed5f1`.
- `backend/go.mod`: Go 1.25, official Lark SDK v3.11.0. There is no
  `backend/.gitignore`; the repository ignore file excludes local credentials,
  build output and the replay corpus.
- `adb devices -l`: Pixel 4a connected as `08041JEC218600`.
- Found the CLI at `~/.railway/bin/railway`; the agent shell's PATH did not
  include it. No installation was needed.

### 2. 22:37–22:38 — move shell work into the user's terminal

- At Max's request, located the only tmux session (`0`), window/pane `0.0`
  (`%0`), already in this repository and idle at a zsh prompt. Subsequent task
  commands run in this pane; tmux control/capture commands transport the input
  and read its output.
- Added `~/.railway/bin` to this pane's PATH and set the Railway skill caller
  and session identifiers for this execution.
- `railway --version`: 5.57.2.
- `railway whoami --json`: already authenticated as Max Coplan. Personal
  workspace: `9acea77b-9a45-43a9-86cb-13437c4a30f1` (Max Coplan's Projects).
- `railway --help`: installed skill revision `950aceb` is current. MCP is not
  configured; the authenticated CLI is available for the deployment work.
- `sl status`: working copy still clean before creating this execution log.

### 3. 22:38–22:40 — inspect existing resources and Backend interfaces

- `railway list --json`: the workspace contains only the unrelated
  `sunny-possibility` project and `function-bun` service. No `larklish` project
  exists, so the plan will create a separate project.
- `railway status --json`: this directory has no linked Railway project.
- Read `railway usage --help` for billing and spending-alert commands.
- Inspected `backend_status()` and the Backend routes. The helper currently
  queries hardcoded SCM/ByteFaaS resources. `/v1/ping` is public; `/lookup` and
  `/chats` require the existing Bearer token. `/chats` returns the cache mapping.
- Created this execution log and linked it from `docs/progress.md`.

### 4. 22:40–22:43 — create the project and prepare source changes

- `railway usage --workspace … --json`: current workspace usage USD 0; no
  workspace spending alert was configured. Inspected usage-limit commands and
  the API's customer schema to find billing/subscription information.
- `railway init --name larklish --workspace … --json`: created project
  `3e575a03-cce3-4c69-bf27-a5362c558c31` in the personal workspace and linked this
  directory. No source was attached and no deployment was started.
- Added `backend/railway.toml`: Railpack, `/backend/**` watch path, public
  `/v1/ping` health check, 60-second startup timeout, `ALWAYS` restart policy,
  and one `us-west2` replica.
- Simplified `backend_status()` to report the configured URL, ping response,
  authenticated `/chats` result and cache count. Removed the hardcoded internal
  SCM/ByteFaaS constants, API wrapper and its unused import. Runtime Go code and
  Android behavior are unchanged so far.

### 5. 22:43–22:46 — empty service, credentials and billing findings

- `railway add --service backend --json` still prompted for optional variables
  in the interactive terminal. Pressed Escape to leave those unset; creation
  succeeded as service `8c497dbc-3a74-449c-b514-bce866ba1d9d`.
- `railway environment list --json` and `railway status --json` verified one
  `production` environment (`20f0c4ca-4be7-48bc-8d7d-e5fde104ff52`) and the empty
  Backend service, with no source, deployment, domain or volume.
- Tried the planned USD 15 soft usage alert. Railway returned
  `Usage limits require an active subscription.` No alert was created.
- Opened the existing Railway workspace in Chrome, inspected Plans, and opened
  the Hobby checkout. The account showed a verified Trial with USD 5 credit;
  checkout required a payment method. Asked Max to complete payment entry, then
  paused billing changes when Max asked whether Trial was sufficient. No
  subscription was purchased by the agent.
- Read the existing Backend token from devbox over SSH into a subprocess pipe;
  copied only `larklish.backendToken` into the Mac's ignored `local.properties`.
  Verified all other properties were preserved. Saved the previous file under
  ignored `output/railway-demo-20260915/local.properties.before` (mode 0600).
  Token values were not printed or rotated; the phone's token file was untouched.
- Inspected the API's `ServiceInstanceUpdateInput` and environment-edit help
  before configuring the service. An initial schema-output filter used `types`
  rather than `matches` and returned an empty list; corrected the filter.

### 6. 22:46–22:48 — check Trial and additional skills

- Listed `~/.agents/skills/` at Max's request; it includes the Railway, Lark,
  Sapling and devbox skills relevant to this work.
- Read Railway's [Trial documentation](https://docs.railway.com/pricing/free-trial):
  Full Trial supports code deployment and full network access. Trial lasts up to
  30 days or USD 5 of usage, then becomes Free with USD 1/month credit. Limited
  Trial has outbound-network restrictions. The account's Plans page said it was
  verified. Actual Lark connectivity will be tested from the deployment.

- Confirmed `~/.agents/skills/use-railway/SKILL.md` is byte-identical to the
  loaded Codex copy. No additional skill installation was needed.
- API readback: `isTrialing: true`, remaining trial credit USD 5. The API's
  `plan: HOBBY` enum also appears for this Trial, so it is not evidence of a paid
  subscription. Proceed on the verified Trial at Max's request; defer the paid
  plan and its usage alert. Updated the deployment plan accordingly.

### 7. 22:48–22:50 — configure credentials and run local checks

- Set `LARK_APP_ID`, `LARK_APP_SECRET`, `LARKLISH_BACKEND_TOKEN`, `LARK_HOST`
  and `PORT` through `railway variable set KEY --stdin --skip-deploys --json`,
  explicitly scoped to the new project, environment and service. Captured
  variable readback in Python and verified all five values without printing them.
- Attempted a `serviceInstanceUpdate` API mutation for build/root/health/region
  settings. The CLI exited 1; the wrapper initially showed only the exit failure.
  Inspecting CLI help and reading configuration before deciding whether to retry.
- `go -C backend test -skip Replay ./...`: passed (cached). The known full-corpus
  replay threshold failure remains outside this change.
- `uvx --system-certs ruff format tools`: one file unchanged.
- `./gradlew ktfmtFormat`: passed; the real existing Backend token is now in
  ignored configuration, so no temporary token placeholder was required.

### 8. 22:50–22:52 — resolve live platform constraints

- Read `railway api --help` and retried the idempotent configuration request
  with its full error visible. Railway rejected `railwayConfigFile`: new
  services cannot opt into deprecated `railway.json` / `railway.toml` support.
  The console says this restriction started 2026-08-28; existing users retain
  support until 2026-12-01. The earlier plan relied on legacy documentation.
- Removed the newly added, uncommitted `backend/railway.toml`. Use ordinary
  Railway service settings through the CLI/API for this single Backend; keep
  their exact values and readbacks in this log instead of adding a new
  infrastructure-as-code toolchain.
- The live Trial settings show fixed maxima of 2 vCPU / 1 GB and `ON_FAILURE`
  with at most 10 restart attempts. Custom resource-limit sliders are disabled.
  Adapt to these Trial limits, keep one replica and Serverless disabled, and
  measure actual usage. Paid-plan-only controls are deferred with billing.
- A readback command used an unsupported `--project` flag on
  `railway environment config`; CLI help confirmed this subcommand uses the
  linked project. Switched to an explicitly scoped API query for the readback.
- Inspected the existing project and Backend Settings pages in Chrome. No
  payment was submitted. The source was still disconnected and the Backend
  had no deployment.

### 9. 22:52 — service settings accepted; phone backup issue isolated

- Repeated the configuration mutation without the deprecated config-file field
  and with Trial's restart policy. `serviceInstanceUpdate` returned true.
- The first readback queried `multiRegionConfig` on `ServiceInstance`, which
  that output type does not expose. Used the supported `railway environment
  config --environment … --json` instead, with variables redacted before
  display. It confirmed `/backend`, Railpack, `/backend/**`, `/v1/ping`,
  60 seconds and one `us-west2` replica.
- `adb pull` of the installed APK failed twice at 0%, without a diagnostic
  beyond exit 1, even when stdout/stderr were captured. The phone and installed
  app are unchanged. Investigating an alternate read method before migration.
- Updated the helper's CLI help text and reran Ruff formatting (one file
  reformatted). Updated the plan and current project records for the verified
  Trial and native-settings approach.

### 10. 22:57 — verify native settings and prepare the first release

- API readback verified `railwayConfigFile: null`, root `/backend`, health path
  `/v1/ping`, timeout 60, `ON_FAILURE`, 10 retries, Serverless false, and Trial
  limits of 2 CPU / 1,000,000,000 memory bytes.
- `adb exec-out cat` returned a 28,213,248-byte APK stream with exit 0, but ZIP
  integrity validation failed. The backup is incomplete despite that exit code;
  phone migration remains gated on obtaining a valid rollback APK.
- A separate phone `run-as` token read exited 255. Token fingerprinting has not
  succeeded and no phone credentials have been modified.
- Opened Railway's GitHub repository selector: no repositories were available.
  Opened Configure GitHub App. The browser advanced through the personal
  `vegerot` account and an Only select repositories form, then returned to
  Railway while it was being inspected; the agent did not submit that form.
  Repository visibility will be verified before attaching the source.
- `python3 -m py_compile tools/larklish_helper.py` passed. Reviewed the helper
  diff and confirmed credentials/backups are ignored by Sapling. An initial
  combined validation stopped at the invalid APK before checking Markdown;
  documentation checks are being rerun independently.

### 11. 23:00–23:05 — publish and deploy the GitHub source

- Committed the helper and execution records as `cea339e848d460d48cd82bf7945a4875be09bec3`.
  Go tests, both required formatters, Python compilation and documentation
  checks passed. Pushed the stack to `github/main`; GitHub returned that same SHA.
- The interactive GitHub CLI opened the terminal's pager; exited it to allow
  the remaining commands to finish. At Max's request, switched subsequent
  commands back to direct terminal execution. The tmux pane is idle.
- Saved a rollback APK on the phone at
  `/data/local/tmp/larklish-before-railway.apk`. Its SHA-256 matches the installed
  APK: `81d103698953b6d68c646500d155e73045790bb3eb6658ed9345577a5429e198`.
  Recorded the previous Backend URL and user-token fingerprint under ignored
  `output/railway-demo-20260915/phone-baseline.json`. The token read succeeded on
  retry. A host `adb pull -Z` still failed, so only the verified on-phone APK is
  considered a valid rollback copy so far.
- Refreshed Railway after the GitHub App flow and verified `vegerot/larklish`
  appeared. Selected it, reviewed exactly two staged changes (repo and `main`
  branch), and clicked Deploy Changes with an explanatory deployment message.
  A `gh api user/installations` inspection returned HTTP 403 because that endpoint
  requires a GitHub-App-authorized token; it was not used for authorization.
- Deployment `46f79f2e-3b74-4e90-816b-5356a9e8e793` built from GitHub commit
  `cea339e848d460d48cd82bf7945a4875be09bec3` and reached `SUCCESS`.
- Generated `https://backend-production-a712b.up.railway.app` with target port
  8787. HTTPS `/v1/ping` returned `pong go1.25.14 linux/amd64`.
- Inspected the Lark messaging skill and shared authentication rules for the
  later synthetic tests. The plan already authorizes messages to the existing
  test group; no message has been sent yet.
- Inspected ADB server status: Android Platform Tools 37.0.1, native USB backend.
  A memory lookup found an older *agent-network* connection issue, not an ADB
  diagnosis; it was not applied to this USB issue.
- The first local-corpus parse used `splitlines()` and hit embedded Unicode
  line separators inside JSON text. The helper documents this; use `split("\n")`
  for JSONL instead. No corpus file was modified.

### 12. 23:05–23:14 — public API, Lookup and restart acceptance

- HTTPS health returned 200. Missing and incorrect Bearer tokens returned 401
  for both `/chats` and `/lookup`; authenticated incomplete Lookup returned 400.
  Authenticated `/chats` returned 200 with an initially empty cache. These
  checks took 0.25–0.43 seconds each.
- Ran the rewritten `backend status` helper with the Railway URL and both the
  correct token and an intentionally wrong token. It reported health/cache
  success and the expected 401 failure respectively, without internal tooling.
- The previously researched September 9 Original was not in the older local
  corpus. Replayed the latest stored synthetic test-group Original instead:
  `found`, 58 Full-text characters, English present, content hash
  `4faf5efc8ad7`. Uncached: 5.568 s; cached: 3.516 and 3.538 s. Three concurrent
  calls also passed in 3.355, 3.379 and 3.858 s.
- Saved sanitized results in ignored `backend-acceptance.json` and
  `lookup-acceptance.json` under the deployment output directory.
- Console readback now explicitly says **Auto deploys when pushed to GitHub**.
  The transient unavailable label cleared after GitHub authorization settled.
- Restarted the Railway Backend. Its startup log records 23:10:47 UTC, the
  domain stayed unchanged, and its cache became empty. The CLI itself kept
  waiting silently after the restart; after confirming the result independently,
  interrupted only that local CLI process (PID 23083), which exited 130.
- Replayed the synthetic Original after restart: the cache rebuilt and Lookup
  plus translation passed in 5.253 s.
- Started the idle check at **23:14:16 UTC**, with one cache entry. No further
  application requests will be sent until **23:29:16 UTC**. The ignored
  `idle-baseline.json` records the exact timestamps and cache for comparison.

### 13. 23:09–23:18 — repair USB access and prepare the phone update

- ADB's server log showed native USB read failures and transport disconnects
  during the failed APK transfers. Checked the local Android ADB source for
  `ADB_LIBUSB`, restarted ADB with `ADB_LIBUSB=1`, and verified the selected
  backend. A large copy still disconnected, so requested a cable/port reconnect.
- Max reconnected the cable or changed ports. The next uncompressed pull
  succeeded: **95,779,980 bytes in 2.242 s**. Verified its SHA-256 against the
  on-phone copy and validated ZIP integrity. No permanent shell setting was
  added; the currently running ADB server uses libusb.
- Changed only `larklish.backendUrl` in ignored local configuration to the
  Railway URL, preserving the existing Backend token and all other properties.
- `./gradlew assembleDebug testDebugUnitTest` passed: **21 tests, zero failures
  or errors**. The new APK's certificate SHA-256 matches the installed APK:
  `cd5b25f8a55c9250d8502b702b02a3618174852e401f9bb6014a61fc13c7eaa4`.
- Phone preflight: app running, notification listener bound, SIM loaded,
  mobile data enabled, Wi-Fi enabled, no configured always-on VPN. The phone
  access token expires at 2026-09-16 00:17:41 UTC; it has not been refreshed by
  this work. Installation is held until the Backend idle check completes.
- Read Recorder/UserToken source: updates record the answering Backend URL;
  an in-place upgrade loads the existing token file before considering the
  build's seed. The phone contains 2,585 Recorder events. Existing DM case
  selection needs a title/key match because Recorder updates do not store a
  chat ID; an initial chat-ID filter returned no cases.
- Railway's first 15-minute metrics showed current memory **10.83 MB**, peak
  **13.59 MB**, and average CPU **0.000251 vCPU**. These are short-window
  observations, not a full billing measurement.

### 14. 23:20–23:28 — prepare notification verification during the idle interval

- Read the Lookup classifier: DM Originals use title `Lark`, with the Sender
  in the Preview. Found 62 exact DM Originals in the existing corpus and saved
  one privately for the post-idle read-only test. No new DM will be sent.
- Verified the phone's active default transport is Wi-Fi, Lark is installed
  and not force-stopped, and the notification listener remains bound.
- Read the Lark chat schema and queried only the existing test group as the bot;
  its name is confirmed as **Larklish 测试群**. Prepared explicitly labeled
  synthetic short, long, cellular and three-message burst cases in ignored
  `phone-tests.json`.
- Prepared an ignored one-off acceptance runner using the repository helper
  for bot sends, ADB and Recorder reads. It records sanitized outcomes, the
  answering Backend URL, Wi-Fi/VPN flags and Relay-to-Update timing, and restores
  forced idle after each long-message test. No synthetic message has been sent yet.
- Workspace usage reported about USD 0.00004 for the current early billing
  window; this includes the unrelated existing project. It is not a measured
  full-month cost for Larklish.
- Kept the new APK uninstalled throughout the idle interval so phone traffic
  continued using the previous Backend. At 23:28:35 UTC, 861 seconds had elapsed;
  the acceptance harness had sent no new application requests during that time.

### 15. 23:29–23:38 — retain cache, install in place, diagnose first phone test

- At 23:29:55 UTC, the idle check passed after **937.831 seconds**. HTTPS health
  passed and the same cache mapping was retained; the two checks took 0.679 s.
- Installed the new APK with `adb install -r`. The phone's user-token file was
  byte-for-byte identical immediately before and after installation. Launched
  Larklish and confirmed the listener was bound and the helper used Railway.
- Sent the labeled short Wi-Fi test through the existing bot. No Relay appeared
  in 45 seconds. Read Recorder, logcat and active notifications: **zero new
  Recorder events and no matching Lark Original**. This was upstream of Larklish.
- Logcat showed Lark's `WschannelForegroundService` had crashed at 23:11:40 UTC
  with `ForegroundServiceDidNotStopInTimeException` for its `dataSync` service.
  Reopened Lark normally. Android documents that bringing the app foreground
  resets this service timer: [foreground-service timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout).
  No Android timeout policy was changed. A transient startup screenshot was
  inspected and removed; unrelated screen contents are not included in this log.
- The first archived DM corpus case returned `no-chat` in 23.905 s. Selected a
  previously successful plain-text DM from Recorder instead, using its recorded
  time as the Lookup timestamp: **found, text, 5.092 s**. Both sanitized results
  are retained; Lookup matching rules and the corpus were not changed.
- Prepared a distinct short-message retry after reopening Lark, preserving the
  original failed test report.

### 16. 23:39–23:43 — real notifications pass on Wi-Fi and cellular

- After reopening Lark, the distinct short retry produced an English Relay.
  Preview was complete; `not-truncated` correctly skipped Backend Lookup.
  Recorder network flags: Wi-Fi true, VPN false.
- Forced deep idle and verified `IDLE`, then sent the labeled long Wi-Fi case.
  Lark posted a truncated Preview; Larklish posted the English Relay and Updated
  it from Railway. Full text matched the sent text exactly, with no Chinese in
  the translated Relay. **Relay → Update: 3.894 seconds**. Restored normal idle.
- Saved the phone's Wi-Fi/mobile-data settings, disabled Wi-Fi, and waited for
  the actual default transport to become **CELLULAR**. Wi-Fi setting was 0 and
  mobile data was 1; no VPN transport was active.
- Repeated the long test in deep idle over cellular. Recorder confirms
  `wifiConnected: false`, `vpn: false`, and the Railway Backend URL. Full text
  matched the sent text exactly and the Relay was English. **Relay → Update:
  4.739 seconds**. Restored normal idle after the test.
- Saved the test summaries as `phone-wifi-short-retry.json`,
  `phone-wifi-long.json` and `phone-cellular-long.json` under ignored output.
  Timings above are Recorder Relay-to-Update intervals; they do not include
  the earlier Lark delivery and Preview translation time.
- Max noted that screenshot thumbnails still showed Lark's launch logo. The
  first capture was the splash screen; a later capture showed the open app.
  Both used one filename, so subsequent captures will use unique filenames.

### 17. 23:45–23:58 — burst, dismissal and final device state

- Sent the three labeled burst messages over cellular. All three produced
  English Relays with complete Previews; all correctly recorded
  `not-truncated`, with Wi-Fi/VPN false.
- Inspected Android's notification command help and active notification keys.
  Located the final synthetic Lark notification in the shade and verified its
  visible text before swiping. Its Original and corresponding Relay were both
  removed. Recorder reason was **12 (`group_summary_canceled`)**, so this
  verifies notification-group dismissal propagation.
- Sent one final labeled long demo notification over cellular after dismissal.
  It produced a full English Update from Railway in **4.531 seconds** after
  the Relay. Full text matched the synthetic message exactly. Left that demo
  notification available for inspection.
- Restored the original Wi-Fi setting and normal device-idle behavior. Later
  readback confirmed Wi-Fi enabled and connected, default transport **WIFI**,
  deep idle **ACTIVE**, both apps running and the listener bound. There is no
  ADB reverse mapping. The phone token file still matches its pre-install bytes.
- Queried project-specific usage: Larklish had consumed about **USD 0.000401**
  so far (CPU 0.000049, memory 0.000336, egress 0.000016). This is an initial
  observation, not a full-month bill. No paid subscription or alert was activated.
- Updated the current plan, progress and research records with the live service,
  verified results, Trial constraints and rollback/operation notes.
- Re-ran ktfmt and Ruff: both passed without source changes. Noticed separate
  uncommitted Go edits renaming `Candidate.CreateTime` to `CreateTimeMs` in six
  Backend files. Those edits are outside this deployment change and are being
  preserved separately; the deployed and tested Backend remains the commit above.

## Current status

Implementation and acceptance are complete. The Backend remains running on
verified Trial, and the updated Pixel uses it. Public authentication, group/DM
Lookup, translation, concurrency, restart, idle retention, Wi-Fi/cellular
notifications, burst handling and group dismissal have been checked. The first
missing Original and the archived DM miss are retained above rather than hidden.

Follow-up: observe normal use and Trial credit consumption before choosing paid
service or changing sleep/cache behavior. The existing internal-production TODO
is independent. Private rollback files and test data remain in the ignored
deployment output directory; no credential values are recorded here.
