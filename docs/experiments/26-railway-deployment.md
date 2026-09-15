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

## Current status

Execution in progress. The Railway project exists; source deployment and phone
migration have not been performed yet.
