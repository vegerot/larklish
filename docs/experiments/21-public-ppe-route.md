# Temporary public PPE route — 2026-09-15

## Decision

Use Singapore PPE for the immediate demo. Max explicitly retained authenticated
Singapore production deployment as an important TODO. It is not complete.

Public base URL: `https://shop.tiktokglobalshop.com/_/test/demo/larklish`.
The public route is not deployed; the phone still has its previous configuration.

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

The open route form contains a **local draft** of the TLB service configuration:
Consul discovery, backend cluster `faas-sg`, SG1 gateway weight 100, other gateway
locations 0, default load-balancing algorithm, no protocol conversion. A live TLB
service query still returns no registered service. This draft is not deployed and
may be lost if the browser form is closed.

## Remaining route work

After registration approval, define and annotate these HTTP APIs, then publish
their routes through NetLink's required review:

| External path suffix | Method | Backend path | Authentication |
| --- | --- | --- | --- |
| `/lookup` | POST | `/lookup` | Existing Bearer token |
| `/chats` | GET | `/chats` | Existing Bearer token |
| `/v1/ping` | GET | `/v1/ping` | Public health probe |

- Restrict routing to the chosen temporary base path. Strip
  `/_/test/demo/larklish` before forwarding to the Backend.
- Target PSM `coplan.lark.larklish` / `faas-sg` through service discovery. Set the
  gateway's service identity and pin the route to PPE `ppe_deploy_i18n_1` in TLB.
  Preserve `Authorization`. Clients omitting or changing the environment header
  must still reach authenticated PPE; the older unauthenticated production code
  must not become public through this route.
- Review the resulting configuration diff, ensuring the existing 37 routes and
  DNS records are unchanged. Complete the required external review.
- Verify the public health probe, missing/wrong/correct token behavior, and a
  Lookup using an existing test message. Do not send a new message without Max's
  authorization.
- Only after public connectivity works, set the phone's Backend URL, build and
  install the APK as an update, preserve app data and its existing user-token
  chain, verify on cellular with Wi-Fi and VPN off, then soak.

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
