# Temporary public PPE route — 2026-09-15

> Continuation update: ticket `378704` is now verified finished. Railway is the
> active demo Backend, and Max authorized internal hosting as a separate
> experiment. The historical state and phone-cutover plan below are superseded
> by [Experiment 28](28-internal-hosting-handoff.md).

## Decision

Use Singapore PPE for the immediate demo. Max explicitly retained authenticated
Singapore production deployment as an important TODO. It is not complete.

Public base URL: `https://shop.tiktokglobalshop.com/_/test/demo/larklish`.
The public route is not deployed; the phone still has its previous configuration.

## Resume here

Last verified state from the September 15 investigation:

| Work | State / next action |
| --- | --- |
| Authenticated Backend | Published to ByteDance origin and tested in Singapore PPE |
| Consul trigger | Ready; do not recreate it |
| API Management registration | Ticket `378704` awaits `xi.zhang`, then `wangchen.iven` |
| TLB service registration | Local form draft only; not deployed |
| Public route | Not published; finish API annotations and the route review after registration |
| Phone update and cellular soak | Pending public routing; preserve app data and the user-token chain |
| Authenticated production deployment | **Important TODO**; Bits SCM-metadata failure remains unresolved |
| Restriction to Max's Lark identity | Proposed additional check; not implemented or selected for immediate work |

The immediate blocker is external API-registration review. Completing that review
permits the next registration steps; it does not itself expose the HTTP endpoint.

## Verified

- Authentication commit `952abda5d0965ae29ad84ed6b4ced8a8ea168222` is on ByteDance
  `origin/main`. SCM `oec/seller/larklish:1.0.0.13`, version ID `165403130`, built
  successfully from that commit.
- PPE function `mmyp0srw`, cluster `faas-sg`, lane `ppe_deploy_i18n_1` runs the
  authenticated build. Bits project run `1230600871170` succeeded after the
  existing shared token was configured on PPE.
- On PPE: `/v1/ping` returns 200; `/chats` returns 401 without the shared token and
  200 with it.
- On `https://kpb2dvsn.sg-fn.tiktok-row.net`, `/chats` returns 200 without an
  environment header (old production build), and 401 with
  `x-tt-env: ppe_deploy_i18n_1` (authenticated PPE). A malformed `/lookup` request
  with that header returns 401 without the token and 400 with the correct token.
  `x-use-ppe` was unnecessary for this direct FaaS gateway test. TLB behavior is
  still to be verified.
- Production Consul trigger `e8a78dl9` (`larklish-tlb-consul`) is enabled, ready,
  and metadata-synced. ByteSD resolves the PSM to FaaS gateway endpoints in SG1,
  including IPv6 endpoints for `faas-sg` / `prod`.
- [ByteFaaS Consul documentation](https://cloud.bytedance.net/docs/faas/docs/63d786117df7d2021dfc68e3/63e13a783d23a3021df0bf3c)
  confirms that PPE needs only the production Consul trigger and an environment
  header. Consul resolves the gateway, which then routes to the function.

## NetLink registration and review

| Item | Value |
| --- | --- |
| Domain | `shop.tiktokglobalshop.com` |
| Business / namespace | `TTS_Global_Shop`, `10071827` / `125` |
| Servername | `2955` |
| TLB cluster | `tiktok_ecom_lb_alisg_v3` |
| Existing servername version at inspection | `1051155` |
| API registration ticket | [378704](https://cloud.tiktok-row.net/netlink/v2/main/business/ticket/378704?ti_business_id=10071827) |
| Underlying NetLink ticket / flow | `326460` / `328875` |
| Current review | PSM owner `xi.zhang`, then business reviewer `wangchen.iven` |

The ticket registers the existing PSM as an API service at level P2. It does not
create a new PSM, Space, public route, or policy exemption. NetLink selected
`xi.zhang` for PSM-owner review; `wangchen.iven` is an existing domain owner and
was selected from the permitted business reviewers. No direct message or reminder
was sent to either reviewer. The console submitted an empty change-reason field
despite a prepared explanation; the ticket's configuration diff identifies the
exact PSM and namespace being registered.

The normal route form requires API registration and interface annotations before
publishing. There are no existing Larklish routes among the domain's 37 routes.

The two warnings in Max's screenshot refer to different records:

| Console warning | Required record |
| --- | --- |
| PSM must be published via API | The existing PSM in NetLink API Management, owned by the business line; this is ticket `378704` |
| PSM is not registered in the TLB cluster | A TLB Backend entry describing service discovery and the FaaS cluster |

The later route change maps the temporary public path to that Backend entry.
No new Bits Space is needed. An incorrect Bits URL redirected to Space creation
during login recovery; nothing was created there.

The open route form contains a **local draft** of the TLB service configuration:
Consul discovery, backend cluster `faas-sg`, SG1 gateway weight 100, other gateway
locations 0, default load-balancing algorithm, no protocol conversion. A live TLB
service query still returns no registered service. This draft is not deployed and
may be lost if the browser form is closed.

## Remaining route work

After registration approval, define and annotate the APIs that will be public,
then publish their routes through NetLink's required review. The Backend API
inventory is:

| External path suffix | Method | Backend path | Authentication |
| --- | --- | --- | --- |
| `/lookup` | POST | `/lookup` | Existing Bearer token |
| `/chats` | GET | `/chats` | Existing Bearer token |
| `/v1/ping` | GET | `/v1/ping` | Public health probe |

The initial route proposal included all three paths. During the security
discussion, the recommendation was narrowed to exposing only `/lookup`: the
phone does not need `/chats`, and the FaaS health probe can keep using the direct
function endpoint. This narrower public scope remains a proposal to confirm
before publication; no route was changed during that discussion.

- Restrict routing to the chosen temporary base path. Strip
  `/_/test/demo/larklish` before forwarding to the Backend.
- Target PSM `coplan.lark.larklish` / `faas-sg` through service discovery. Set the
  gateway's service identity and pin the route to PPE `ppe_deploy_i18n_1` in TLB.
  Preserve `Authorization`. Clients omitting or changing the environment header
  must still reach authenticated PPE; the older unauthenticated production code
  must not become public through this route.
- Review the resulting configuration diff, ensuring the existing 37 routes and
  DNS records are unchanged. Complete the required external review.
- Verify missing/wrong/correct token behavior and a Lookup using an existing
  test message. Verify that paths excluded from the public route cannot reach
  the Backend. Do not send a new message without Max's authorization.
- Only after public connectivity works, set the phone's Backend URL, build and
  install the APK as an update, preserve app data and its existing user-token
  chain, verify on cellular with Wi-Fi and VPN off, then soak.

## Authentication discussion

The implemented Bearer token is a shared secret. It permits whoever possesses
it; it does not establish that the caller is Max. The token is embedded in the
APK, so possession of that APK can expose the credential. Keep the generated
value in gitignored `local.properties`; never print it or commit it.

A proposed stronger account restriction is to validate the phone's existing
Lark user token through Lark's identity API, then compare the returned stable
user ID with Max's configured ID. This would reuse the existing login. It has
not been implemented, and compatibility with this app's token/scopes still needs
verification. Do not describe the current Backend as enforcing a Max-only
identity allowlist.

[TLB SSO documentation](https://cloud.bytedance.net/docs/netlink/docs/653a37a304438602f814d09a/6707f21e20353102ea1665d6)
states that successful SSO authentication supplies `X-Bytedance-User`, but failed
authentication does not itself block the request. The Backend would still need
to enforce authentication and the allowed identity. No SSO configuration changed.

## TLB canary investigation

Max asked whether the canary header could replace release approval. The headers
involved have different purposes:

| Header | Purpose |
| --- | --- |
| `Authorization: Bearer <token>` | Authenticate to the Backend |
| `x-tt-env: ppe_deploy_i18n_1` | Select the PPE Backend environment |
| `x-tlb-canary: 1` | Select a TLB instance with the canary routing configuration |
| `get-svc: 2` | Inspect the second TLB hop when debugging canary forwarding |

[Official canary instructions](https://cloud.bytedance.net/docs/netlink/docs/653a37a304438602f814d09a/67f8e929118a1305014b4600)
confirm that `x-tlb-canary: 1` forwards to instances where the small-traffic
release is already in progress. It does not deploy an unsaved route draft.
Canary instances also carry ordinary traffic, so this header does not make a
route private or provide authentication.

Read-only inspection of existing [ticket `1105927`](https://cloud.tiktok-row.net/tlb3/ticket/1105927)
for this same domain and TLB cluster found this completed sequence:
`review` → `canary` → `full` → `close`. Review completed July 10 at 02:33:40 UTC;
canary started at 02:34:12 UTC. This is evidence from an existing batch dynamic
configuration ticket, not the future Larklish route ticket. Inspect the new
ticket's actual stages when it exists; do not claim an unverified exception.
The separate API-registration review remains required now.

The canary instructions also document incompatibility with forwarding through
the PPE/BOE proxy selected by `x-use-ppe`, and with `x-schedule-vdc`. The direct
FaaS gateway test needed only `x-tt-env`; verify the final TLB-to-FaaS path rather
than assuming that every combination of environment and canary headers works.
No canary configuration was deployed during this investigation.

## Continuation constraints

- Use `--site i18n-tt --vregion Singapore-Central` for the FaaS resources.
- For production changes, recheck IAM's permitted window. The verified user
  timezone is `America/Los_Angeles`; working-day windows are 09–12, 13–18, and
  19–22. Max chose the ordinary window, without a policy exemption.
- Recheck ADB access to Pixel `08041JEC218600` before phone work. Its signing
  certificate previously matched the built APK; preserve its existing app data.
- The remote Chrome endpoint is devbox `localhost:9222`, profile
  `~/.cache/sa-tea-chrome-profile`; the Mac viewer forwards local port 9223 to it.
  Rediscover page IDs. Local-network permissions granted through CDP are
  temporary and need the permission connection to remain open. Max authorized
  Bits and further task-required browser permissions.
- Do not send reviewers direct messages, send test messages, or request another
  policy exemption without explicit authorization. Normal review-ticket
  submission is already authorized. Do not create another timer.
- Keep unrelated untracked files and worktrees intact. Publish to ByteDance
  `origin`, not the GitHub remote.

## Important production TODO

Finish normal Bits task `2844150` / release `1229200073986` for the authenticated
build. Production has the shared token configured but still runs SCM `1.0.0.11`.
Its instance limits are already MY 1/1 and MY2/MY3/SG1 0/0.

Repair old release `1225461564162`, project pipeline/run
`1229186479874` / `1229972423170`, final job `3261393118`:
`SCM info not found in pipeline`. The 09:15 PDT retry reproduced the failure
(log ID `02178948892000000000000000000000000ffff0a925ba75b2dc4`). The SCM job output
does contain the correct artifact and commit. No check was skipped or
force-completed. This remains separate from the previously approved Nario
exemption. Investigating the official gatekeeper atom's source did not establish
the cause, so no shared plugin or policy was changed.
