# Deploy Larklish on ByteDance PPE, then switch the Pixel to production

## Summary

Use the public route that is already live. It forwards `/_/test/demo/larklish/lookup` to the correct PSM but currently gets a Backend 404. Add that exact path as an authenticated Backend handler, deploy it to PPE, and have the Android app send `x-tt-env: ppe_deploy_i18n_1`. No new TLB configuration is needed.

Railway stays running as a rollback option; we will record its source commit and rebuild that commit if we need to rollback. Once the **alias-bearing Backend code** is released to production and verified, disable the PPE header in the app, rebuild and install it, and verify a production Lookup on the Pixel.

## Release workflow going forward (2026-09-16)

The PPE steps below record the original alias rollout, which used Bits. For later
Backend changes, use ByteFaaS directly to release the exact successful,
Singapore-synchronized SCM artifact to PPE function `mmyp0srw` in
`ppe_deploy_i18n_1`, then verify the deployed revision and behavior. Both a
direct console release and a `bytedcli` release pinned to an existing revision
succeeded for SCM `1.0.0.20`; creating a new revision through `bytedcli` was not
tested. Production function `kpb2dvsn` remains on the normal Bits workflow and
its GEC/QA gates. Do not use the direct PPE path as a production release.

## Changes

- Cancel pending rewrite/PPE-header ticket `379619` while it is still in review; verify the completed first route remains at version `1260366`. If the ticket has advanced, inspect its effects before proceeding.
- Add only `POST /_/test/demo/larklish/lookup` to the Go router, using the same Bearer-protected Lookup handler as `POST /lookup`. Do not expose prefixed `/chats` or `/v1/ping`.
- Add an optional build-time `larklish.backendTtEnv` setting to the Android app. When set, send it as `x-tt-env`. Set the ignored local value to `ppe_deploy_i18n_1` for the PPE trial. Retain the configurable Backend URL and token. Remove the temporary `larklish.backendTlbCanary` property and code—there is no new TLB rollout.
- Update Experiment 28 and `progress.md` to supersede the rewrite plan as implementation proceeds.

## PPE release and acceptance

1. Add route/authentication tests; run Go tests, Android unit tests, ktfmt, the local Backend build, and diff checks. Record the known corpus replay-test result separately from new regressions.
2. Audit the 12 commits by which local/GitHub `main` leads ByteDance `origin/main`, then fast-forward **ByteDance `origin/main` only** with the tested change. Do not push GitHub `main`, which would auto-redeploy Railway. Wait for SCM to build a successful artifact from the exact new commit.
3. Dry-run, then create a new Bits development task from task `2844150`, pinned to that SCM version, with PPE enabled and BOE disabled on `ppe_deploy_i18n_1`. Run its normal self-test deployment and verify the PPE function reports the new revision and source.
4. Test the direct gateway and existing public exact path. With `x-tt-env` selecting PPE, missing/wrong Backend Bearer tokens must return 401; the correct token with an incomplete payload must reach Lookup validation (400). A recorded Original with a valid phone user token must return the expected Full-text fingerprint and English output. Without—or with a mistyped—environment header, the still-old production build should return 404 for the prefixed path. Confirm prefixed `/chats` remains inaccessible. If public header forwarding fails, do not install the APK; investigate a narrowly reviewed header-only TLB change, with no path rewrite.
5. Record the verified Railway source rollback commit (`cea339e848d460d48cd82bf7945a4875be09bec3`), signing certificate, Recorder count, and user-token fingerprint. Build and install the PPE-configured app in place; verify the token is unchanged and the listener rebounds. Run a synthetic notification test on Wi‑Fi and cellular with VPN off, record Relay-to-Update timing and Backend identity, then restore the phone’s network settings. If acceptance fails, rebuild the Railway-configured app from the checkpoint and reinstall it.

## Production follow-up and phone cutover

Resolve the Nario worker-to-commit blocker through the normal GEC/QA process. Complete the required Bits Test/Merge and production release checks without skipping or forcing them. The **existing** production release is pinned to SCM `1.0.0.13`, which lacks the prefixed handler; completing that release alone must **not** switch the phone.

Promote the **new alias-bearing Backend build** to production through the normal workflow. Verify that the public prefixed path works **without** `x-tt-env`, rejects missing and wrong Bearer tokens, and completes a recorded Lookup. Then remove `larklish.backendTtEnv` from the phone’s ignored build configuration, rebuild and install the app in place, and verify a production Lookup on the Pixel. However we should still keep the code for using `backendTtEnv` because it might be useful in the future.  This cutover is part of the plan—not a later discretionary decision—and requires no additional NetLink route change.

## Current release disposition

The older task `2844150` and release `1229200073986` are superseded by the alias-bearing task `2850411` and its release `1229776511490`. Do not promote SCM `1.0.0.13` to production as an intermediate step: it lacks the prefixed handler and the new task contains the same authentication change. Both tasks currently show the same non-skippable Nario worker-to-commit metadata failure. Preserve the older task as comparison evidence for QA/GEC until its report has been shared; then retire the old task and associated release through normal controls, after checking that closure will not affect their shared PPE lane. Neither old item has been cancelled yet. Production remains gated on normal resolution for the alias-bearing task.
