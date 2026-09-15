# Deploy Larklish on Railway

Updated 2026-09-15 to use GitHub deployments. The original CLI-upload plan remains in version history.

## 1. Target and defaults

Run the existing Go Backend as **one always-running Railway service**, with its current in-memory cache and a Railway-provided HTTPS domain.

| Setting | Choice |
|---|---|
| Account | Max’s personal Railway workspace, Hobby plan |
| Project / service | `larklish` / `backend` |
| Environment | `production` |
| Deployment source | GitHub `vegerot/larklish`, branch `main` |
| Service root / watch path | `/backend` / `/backend/**` |
| Railway config file | `/backend/railway.toml` |
| Region | Singapore: `asia-southeast1-eqsg3a` |
| Replicas | One |
| Serverless / sleeping | Disabled |
| Initial resource limits | 1 vCPU, 512 MiB memory |
| Internal port | `8787` |
| Health check | `GET /v1/ping` |
| Restart policy | `ALWAYS` |
| Storage | Existing in-memory cache; no volume or database |

Hobby costs **at least $5/month**, including $5 of resource usage. Aim below the established $15/month target and configure a $15 usage alert. Actual cost will be measured after deployment. [Pricing](https://docs.railway.com/pricing/plans), [regions](https://docs.railway.com/deployments/regions).

**Success:** the Pixel receives a Relay and its Full-text Update over cellular, with Wi-Fi and VPN disabled. The service remains available afterwards.

## 2. Prepare credentials and deployment configuration

### Credentials

1. Retrieve the existing `larklish.backendToken` from devbox’s `/data00/home/max.coplan/code/github.com/vegerot/larklish/local.properties`.
2. Copy **only that property** into the Mac’s gitignored configuration, preserving its other entries. Reuse the token without rotating it.
3. Keep the phone’s access/refresh-token chain untouched. Do not use the helper’s token-writing function for this copy: it replaces all `lark.*` entries.
4. Configure Railway with:

   | Variable | Value |
   |---|---|
   | `LARK_APP_ID` | Existing app ID |
   | `LARK_APP_SECRET` | Existing app secret |
   | `LARKLISH_BACKEND_TOKEN` | Existing token retrieved from devbox |
   | `LARK_HOST` | `https://open.larksuite.com` |
   | `PORT` | `8787` |

Use `railway variable set KEY --stdin --skip-deploys` for credentials, passing values through subprocess input without printing them or putting them in command arguments. [Variable CLI](https://docs.railway.com/cli/variable).

### Railway setup

1. Install the official Railway CLI for configuration, logs and operations; it is currently absent on the Mac.
2. Sign in and complete Hobby billing setup.
3. Create the project and empty service using `railway init --name larklish` and `railway add --service backend`. Reuse an existing matching project if one was already created.
4. Add `/Users/bytedance/code/github.com/vegerot/larklish/backend/railway.toml`, containing:
   - Railpack builder.
   - One Singapore replica through `multiRegionConfig`.
   - Health-check path `/v1/ping`, with a 60-second startup timeout.
   - Restart policy `ALWAYS`.
5. Set the service root to `/backend`, the Railway Config File to `/backend/railway.toml`, and the watch path to `/backend/**`. The config-file path is relative to the repository root, independently of the service root.
6. Set Serverless **off**, the resource limits and the credentials above before the first deployment.
7. Connect Max's Railway account to GitHub and grant the Railway GitHub App access to `vegerot/larklish`. Attach the repository to the service during the deployment step below, after the configuration commit is on GitHub.

Railpack detects `go.mod` in the `/backend` service root and builds the Go executable. Keep its automatic build/start commands. Railway's native GitHub integration handles deployment; no GitHub Actions deployment workflow or custom packaging pipeline is needed. [Go support](https://railpack.com/languages/golang/), [configuration reference](https://docs.railway.com/config-as-code/reference), [monorepo settings](https://docs.railway.com/deployments/monorepo), [GitHub connection](https://docs.railway.com/services#deploying-from-a-github-repo).

## 3. Deploy and verify the public Backend

### Deployment

1. Run the applicable local checks and commit the deployment configuration with Sapling.
2. Push the tested commit to the `github` remote's `main` branch. The default remote points to Codebase; a push there does not release the Railway Backend.
3. Connect the Railway service source to `vegerot/larklish`, branch `main`, and enable automatic deployments. Confirm the root directory, config-file path, watch path and variables before applying the staged configuration and deploying the latest commit.
4. Wait for Railway's build and deployment to succeed. Verify that the deployment's source commit matches the commit pushed to GitHub and that Railpack built the Go Backend from `/backend`.
5. Generate the public domain:

```sh
railway domain --service backend --port 8787
```

Generate the domain once, then retain it across releases. Railway provisions and renews its HTTPS certificate. [GitHub deployments](https://docs.railway.com/deployments/github-autodeploys), [domain CLI](https://docs.railway.com/cli/domain).

Record the project, service, deployment ID, source revision, region and generated URL.

### Acceptance checks

Before changing the phone:

- Confirm deployment success, one replica, sleeping disabled, and the expected configuration.
- Verify HTTPS normally and require `/v1/ping` to identify a Linux Backend.
- Verify missing/wrong Bearer tokens return **401** for `/lookup` and `/chats`.
- Verify an authenticated incomplete Lookup returns **400**, and authenticated `/chats` returns its cache.
- Replay the existing test Original using the phone’s current access token. Require `found`, matching Full text and English.
- Exercise an uncached Lookup, cached Lookups and three concurrent requests. Require completion within the phone’s existing 30-second timeout.
- Leave the service idle for 15 minutes, then verify availability and cache retention.
- Restart the deployment once. Confirm the domain stays unchanged and the cache rebuilds successfully.

The public JSON protocol and Bearer authentication remain unchanged. No Go application changes are expected.

Reuse the established test baseline: ordinary Go tests passed; the full corpus replay has the existing **67/165** threshold failure. Record it without changing Lookup rules or weakening the test.

## 4. Move the phone and leave a repeatable workflow

### Phone migration

1. Retain the previous APK and Backend URL for rollback.
2. Confirm the new APK’s signing identity matches the installed app.
3. Set the generated Railway URL in the Mac’s gitignored configuration. Build and install in place, preserving app data and `user-token.json`.
4. Send clearly labeled synthetic messages to the existing **Larklish 测试群**: a short message, a long Chinese message, and a small burst.
5. Verify Original → prompt Relay → Full-text Update for the truncated message.
6. Repeat on cellular with Wi-Fi and VPN off. Confirm the Recorder identifies the Railway URL.
7. Check an existing direct-message Lookup and dismissal behavior.

### Operations and records

- Simplify `backend status` in [the helper](/Users/bytedance/code/github.com/vegerot/larklish/tools/larklish_helper.py) to report the configured Backend’s health, authentication and cache count. Remove its hardcoded SCM/ByteFaaS queries.
- Subsequent releases follow **local checks → Sapling commit → push to GitHub `main` → Railway automatic deployment**. Watch paths restrict source-triggered releases to Backend changes; Android and documentation changes do not trigger a Backend deployment. Verify the deployed source commit, health and authenticated Lookup after each release; use Railway’s previous deployment for immediate rollback.
- Use the Railway CLI or MCP tools for configuration, logs, metrics and restarts. GitHub remains the normal source for releases. There is currently no GitHub Actions workflow; enable Railway's **Wait for CI** only if a suitable push-triggered workflow is added later.
- Inspect Railway’s logs, memory, CPU and usage estimate after the initial tests. Record the first complete billing measurement before considering sleep or persistent caching.
- Update the project plan, progress and research records to make Railway the current choice. Preserve the verbatim BytePlus plan as historical material.
- Run the required formatters and documentation checks before committing the scoped configuration, helper and record changes.

**Assumptions:** this remains a single-user demo that continues running afterwards; Singapore is the initial region; existing internal deployments remain available independently. Scale-to-zero and cache persistence are deferred until measurements justify them.
