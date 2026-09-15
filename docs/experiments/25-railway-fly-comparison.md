# Railway, Fly and the public demo — 2026-09-15

## Decision

**Use Railway with one always-running service and the existing in-memory cache.**
Max accepted this recommendation and requested the
[Railway deployment plan](../railway-deployment-plan.md), originally saved verbatim
and now updated to use GitHub deployments.
This supersedes the [BytePlus VM plan](../byteplus-deployment-plan.md), which
remains unchanged as history. No Railway deployment has been performed.

The requirements remain: demo ASAP, keep it available afterwards, stable public
HTTPS reachable from the Pixel on cellular without VPN, and a target below
USD 15/month. Up to USD 35/month was allowed if necessary for the demo.

## What changed the recommendation

Read the [shared Gemini conversation](https://share.gemini.google/K7SVM8qXHipE),
then checked the relevant claims against official documentation and the existing
Go Backend. The earlier VM choice had been made within Max's BytePlus choice.
Reopening the provider comparison made Railway the simplest overall option.

| Option | Setup and ongoing work for Larklish |
| --- | --- |
| Railway | Connect GitHub with `/backend` as the service root, configure variables and one replica, and generate a domain. Go compilation, HTTPS and certificate renewal are managed. |
| Fly | Launch from Go source, review the generated Dockerfile and `fly.toml`, choose Machine size/count and stop behavior. Provides a stable `fly.dev` HTTPS address. |
| BytePlus VM | Compile and transfer the executable, configure the VM/IP/security group, systemd, Nginx and Certbot, and maintain OS updates and certificate renewal. |

The initial proposal used Railway's [CLI source upload](https://docs.railway.com/cli/up)
to avoid a source-repository integration. The existing GitHub remote makes
[native GitHub deployments](https://docs.railway.com/deployments/github-autodeploys)
the simpler ongoing workflow: each release comes from a pushed commit and needs
no separate local upload.
[Railpack detects Go](https://railpack.com/languages/golang/) from `go.mod` and
builds the executable. [Public networking](https://docs.railway.com/networking/public-networking)
provides a generated domain and automatic certificates. Domain generation is a
separate setup step.

Fly does not require a handwritten Dockerfile: its
[Go workflow](https://fly.io/docs/languages-and-frameworks/golang/) generates
configuration. Its defaults still need attention for our single-instance goal;
[Machine counts](https://fly.io/docs/launch/scale-count/) and
[autostop settings](https://fly.io/docs/launch/autostop-autostart/) control that.
The Dockerfile distinction in the shared conversation overstated Fly's burden.

Railway Hobby has a [USD 5/month minimum](https://docs.railway.com/pricing/plans)
including USD 5 of usage; excess usage costs extra. This is not a measured
Larklish bill. Fly charges for its selected Machine resources and traffic.
Experiment 24 retains the observed BytePlus VM quote of about USD 13.90/month
before traffic and taxes. No comparative cloud latency or billing experiment
has been run.

## GitHub deployment decision

- Verified `sl paths`: `github` points to `vegerot/larklish`; the default remote
  points to Codebase. GitHub reports the repository's default branch as `main`.
- Set service root `/backend`, config-file path `/backend/railway.toml`, and
  watch path `/backend/**`. Railway resolves the config-file path independently
  of the service root. [Monorepo settings](https://docs.railway.com/deployments/monorepo).
- Use Railway's GitHub App and automatic deployments from `main`. Run local
  checks before committing and pushing. There is no GitHub Actions workflow in
  this working copy; no deployment workflow is needed for the native integration.
  Railway's optional **Wait for CI** requires a push-triggered GitHub Actions
  workflow. [GitHub deployment controls](https://docs.railway.com/deployments/github-autodeploys).
- CLI uploads operate on local files, which can include uncommitted changes.
  GitHub deployments make the source commit explicit and remove the repeated
  upload step. CLI/MCP remain useful for configuration and operations.
- The first deployment must verify the pushed commit and Go build root, then
  pass the existing public-Backend and cellular acceptance checks. No Railway
  GitHub connection or deployment has been tested yet.

## Cache preservation and scale-to-zero

Inspected `backend/fetcher.go`: the cache contains chat-name/Sender-to-chat-ID
mappings and the recent-chat order used for thread replies. Both are currently
in memory and are rebuilt after process restart.

- [Railway volumes](https://docs.railway.com/volumes) provide a mounted persistent
  directory. Saving/reloading a small JSON cache file would allow the service
  to sleep without losing its learned mappings. Its
  [Serverless feature](https://docs.railway.com/deployments/serverless) wakes on
  traffic, but documents wake-up latency and a possible first-request 502.
- [Fly volumes](https://fly.io/docs/volumes/overview/) preserve files across
  stop/start and deployments while the volume is retained. Ordinary root
  filesystems are ephemeral and should not be treated as persistent storage.
- [Fly suspend/resume](https://fly.io/docs/reference/suspend-resume/) saves RAM
  and resumes the existing process, potentially preserving our current cache
  without application changes or an attached volume. CPU/RAM charges stop
  while suspended; storage remains billable. Snapshots can be discarded during
  deployments or maintenance, and external API connections must work after
  a long suspension. This has not been tested with Larklish.

Continuous running is therefore **not technically necessary just to preserve
the cache**. It remains the selected initial behavior because it needs no new
persistence or wake-up handling, and sleeping cannot reduce a Hobby bill already
at its USD 5 minimum. Revisit after measuring usage and Update latency.

The phone posts the Relay before calling the Backend for an Update. A Backend
wake-up would delay that Update; the current phone code does not retry a failed
Backend HTTP request. These are concrete behaviors to test if sleeping is
selected later, rather than reasons to implement speculative retries now.

## Deployment preparation findings

- The existing Backend supports `PORT`, binds on all interfaces, reads its
  credentials from environment variables, and exposes `/v1/ping`. No application
  rewrite is expected for Railway.
- Singapore is available as `asia-southeast1-eqsg3a`. Railway's
  [configuration reference](https://docs.railway.com/config-as-code/reference)
  documents `multiRegionConfig`, health checks and `ALWAYS` restart policy.
  Sleeping and resource limits will be set in service settings.
- The [variable CLI](https://docs.railway.com/cli/variable) supports single-key
  values from stdin and `--skip-deploys`. The
  [domain CLI](https://docs.railway.com/cli/domain) can generate a service domain
  and target port 8787.
- The CLI was absent on this Mac at planning time. The Pixel was attached and
  ready for later verification. No Railway sign-in, account creation, service
  creation, variable upload, CLI installation or deployment was performed.
- The existing Backend token is on devbox, in gitignored `local.properties`;
  it was not removed. The Mac lacks that entry. Copy only the Backend-token
  property, preserving the other credentials and the phone's token chain.
- `tools/larklish_helper.py:write_local` removes all `lark.*` entries before
  writing its supplied values. It is not suitable for importing only the
  Backend token. The implementation should make a targeted property update.
- The helper's `backend status` still queries internal SCM/ByteFaaS. Removing
  those hardcoded queries is part of implementation, not this documentation task.

Next: follow the updated Railway plan, beginning with the existing token, CLI and
Hobby setup. Push the configuration to GitHub, connect the repository and deploy,
verify authenticated Lookup, update the phone in place,
and verify cellular operation. Keep the existing internal deployments and the
separate authenticated internal-production TODO.
