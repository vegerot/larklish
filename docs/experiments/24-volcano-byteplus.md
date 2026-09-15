# Volcano Engine and BytePlus hosting — 2026-09-15

## Status: superseded by Railway

Max subsequently reopened the provider comparison and selected Railway for its
simpler setup and upkeep. Follow the
[Railway deployment plan](../railway-deployment-plan.md) and
[Experiment 25](25-railway-fly-comparison.md). This document preserves the
BytePlus investigation; the [BytePlus plan](../byteplus-deployment-plan.md)
remains verbatim as historical material. No BytePlus resources were created.

## Earlier selected approach

**Use one small BytePlus VM in Johor with a retained Elastic IP, Nginx, Certbot,
and systemd.** Max selected BytePlus, requested an ASAP demo that remains running,
and delegated the VM-versus-function decision according to the project ethos.
The VM's concrete price, existing Go executable, stable IP, and standard HTTPS
path made it the selected approach. The first deployment experiment will prove
IP-certificate issuance and renewal before moving the phone.

The [deployment plan](../byteplus-deployment-plan.md) is saved verbatim. The
Function Service/API Gateway research below documents the earlier alternative;
it is not the selected deployment. Neither approach has been deployed.

Read together with [Experiment 23](23-public-cloud-hosting.md). Max asked for a
deeper comparison, employee-benefit research through `bytedcli mira`, and then
authorized read-only inspection of both signed-in accounts. The immediate goal
remains a public demo reachable from the phone without office Wi-Fi or VPN.
Authenticated internal production remains a separate TODO.

## Live account findings

| Check | Volcano Engine | BytePlus |
| --- | --- | --- |
| Signed in | Yes | Yes |
| Function Service | Blocked by incomplete real-name verification | Reaches cross-service authorization for `ServiceRoleForVeFaaS` |
| API Gateway | Not tested beyond the account prerequisite | Reaches cross-service authorization for `ServiceRoleForApig` |
| Coupons | None listed; account overview also shows zero resource packages | None listed |
| Balance / billing | Available balance RMB 0; no current consumption | Current bill USD 0; the ECS purchase form says payment authentication is incomplete |

No roles were authorized, terms accepted, resources created, or payments made.
Reaching an authorization page does not establish that creation will succeed
after authorization. The older BytePlus trigger guide still mentions beta access;
we did not observe a beta-denial screen, and did not verify the next stage.

### Can Max verify a personal Volcano account?

The live [verification page](https://console.volcengine.com/user/authentication)
explicitly says overseas individual identities are unsupported and directs
individual users to BytePlus. Its published personal verification methods agree:

- [WeChat/Douyin face verification](https://www.volcengine.com/docs/6261/131281)
  requires a mainland Chinese resident identity card.
- [Current-device face verification](https://www.volcengine.com/docs/6261/1264913)
  requires a Chinese resident identity card too.
- [Bank-card verification](https://www.volcengine.com/docs/6261/64939) requires
  a Chinese resident identity card, a qualifying bank card, and its registered
  phone number. Obtaining a Chinese phone number alone does not satisfy this.

No passport route for personal cloud-account verification was found. Domain
registrant verification is a different process and its accepted documents do
not establish cloud-account eligibility. Overseas enterprises have a company
document route, but the account belongs to that enterprise; employment at
ByteDance does not by itself authorize using ByteDance as the account owner.
See [account ownership and verification types](https://www.volcengine.com/docs/6261/64934).

## Employee access and credits

Asked Mira twice through `bytedcli mira chat`; the follow-up supplied the live
account findings and corrected the initial interpretation of the beta notice.
[Mira conversation](https://mira.bytedance.com/chat/547331799315).

Mira's revised choice was BytePlus. It found **no verified general employee or
AI Fullstack Demo Day sponsorship entitlement applicable to this demo**, and no
verified amount, expiry, overseas eligibility, or coverage for reserved function
instances and API Gateway. That is a research result, not proof no program exists.

Independently fetched the relevant passages of its two internal sources with
`lark-cli docs +fetch --as user`:

- [Finance-center permissions](https://bytedance.larkoffice.com/wiki/TxQJwVB1tigjiNkcrOxcVuN9n1B)
  describes administrative access, including voucher management. It does not
  grant free hosting to employees.
- [Voucher-management process](https://bytedance.larkoffice.com/wiki/wikcnaJZ7R0zkxg2OuQxWN9Umch)
  defines internal testing vouchers for Volcano product regression testing,
  with the China product team as the relevant applicant category. This does not
  establish eligibility for Larklish.

Budget for paid usage unless a specific benefit or event sponsor confirms
coverage. Account creation, free API requests, and free reserved compute are
different things. No benefit application or message to another person was sent.

## Function Service alternative: fit with the existing Backend

Both support a Native Web application running the existing Go HTTP executable.
For BytePlus, the [ZIP deployment guide](https://docs.byteplus.com/en/docs/faas/Native_runtime_Code_package_deployment)
explicitly describes Linux/amd64 Go binaries and local ZIP upload, up to 256 MiB.
This avoids setting up a container registry or a Kubernetes cluster.

Small build experiment on this Mac:

```sh
larklish_build_dir="$(mktemp -d)"
CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
  go -C backend build -o "$larklish_build_dir/larklish-backend" .
```

Succeeded: an 11,693,771-byte statically linked x86-64 Linux ELF executable.
The output was kept in a temporary directory. No source change was needed.
This proves compilation, not execution in the provider's runtime.

Earlier proposed Function Service configuration:

| Setting | Value |
| --- | --- |
| Region | Johor, `ap-southeast-1`; confirm in the account after setup |
| Application type | Native Web application, synchronous HTTP |
| Startup | `./larklish-backend -port 8000` |
| Configured listening port | `8000` |
| Instances | Minimum 1, maximum 1 |
| Initial resources | 0.5 vCPU / 1 GiB, matching the existing internal allocation; confirm selectable |
| Timeouts | Function and gateway at least 30 seconds |
| Concurrency | Enable multiple requests per instance; start with 10 |
| Outbound networking | Default shared internet access |
| Variables | `LARK_APP_ID`, `LARK_APP_SECRET`, `LARKLISH_BACKEND_TOKEN`, `LARK_HOST` |
| External API | `POST /lookup`, retaining the existing Bearer header |

The [Native contract](https://docs.byteplus.com/en/docs/faas/Native_runtime_Development_methods_Web_applications)
uses `_FAAS_RUNTIME_PORT`; our code reads `_BYTEFAAS_RUNTIME_PORT` and `PORT`.
The existing `-port` flag avoids any code change. HTTP method, path, headers and
body pass through to the process. The cache can be rebuilt after a restart, so
no database is required. One reserved instance prevents idle scale-to-zero;
it does not prevent restarts or releases from clearing the cache.

[Release controls](https://docs.byteplus.com/en/docs/faas/Release_function) cover
reserved instances. [Function configuration](https://docs.byteplus.com/en/docs/faas/Function_configuration)
covers concurrency and timeout. The default
[shared NAT](https://docs.byteplus.com/en/docs/faas/How_does_a_function_access_the_public_network)
provides outbound internet access, so the Backend needs no private NAT gateway
or fixed outbound IP for its current API calls.

### Is `open.feishu.cn` mandatory?

**No, that was too strong.** The host is configurable. Experiment 05 selected
Feishu's hostname from the tenant's configured brand, but had not established
that the international hostname rejects these credentials.

On 2026-09-15, using the existing app credentials from `local.properties`:

| Host | Tenant token request | Translate synthetic `你好，世界。` |
| --- | --- | --- |
| `https://open.feishu.cn` | HTTP 200, code 0, token returned | HTTP 200, code 0, `Hello, world.` |
| `https://open.larksuite.com` | HTTP 200, code 0, token returned | HTTP 200, code 0, `Hello, world.` |

The two checks requested tenant tokens, not user refresh tokens. The phone's
token chain and configuration were unchanged. Credentials and tokens were not
printed or written into these notes. No real message text was submitted.

### Follow-up: existing user-token Lookup through both hosts

The Pixel was connected during planning, with Larklish running and its listener
bound. Read its existing access token without refreshing it. Built a native Mac
Backend executable in a temporary directory and ran a separate process for each
API host, replaying the same recorded test-group Original from
`2026-09-09T23:04:01.464641Z` three times per process.

| Host | First Lookup | Second / third Lookup | Result | Mac process RSS |
| --- | ---: | ---: | --- | ---: |
| `open.feishu.cn` | 4.426 s | 2.946 / 2.765 s | `found`, 34 Full-text characters, English present | 16.7 MiB |
| `open.larksuite.com` | 5.410 s | 2.811 / 2.900 s | `found`, 34 Full-text characters, English present | 17.0 MiB |

All six responses returned matching Full text (SHA-256 prefix `b9b74075be1a`).
This exercised real chat search, message reads, selection and translation with
the phone's existing user token. No new message was sent, token chain rotated,
or phone setting changed. The processes were stopped after the experiment.

The selected VM plan uses `LARK_HOST=https://open.larksuite.com`. Direct-message
Lookup, the deployed Johor path, and cellular operation remain unverified.
These six local requests are not a latency benchmark or evidence about CPU
throttling. Mac RSS is not a measurement of the Linux deployment. Hostname does
not establish processing region.

Planning checks: `go -C backend test -skip Replay ./...` passed. The full suite
reproduced `TestReplayCorpus` failing at 67/165 Originals, with unchanged runtime
source. Preserve that baseline rather than weakening its threshold for hosting.

## Function Service alternative: public HTTPS uncertainty

The documented HTTP path is phone → API Gateway → Native Web application.
Configure the gateway and function in the same region, preserve the route path
and Authorization header, and send 100% of traffic to this function.

BytePlus [creates a stable default domain](https://docs.byteplus.com/en/docs/apig/Will_the_automatically_generated_access_Domain_name_remain_unchanged).
However, the retrieved documentation did not establish that this default domain
includes a usable managed HTTPS certificate for this account/region.
[Custom HTTPS domains](https://docs.byteplus.com/en/docs/apig/Adding_domain_name)
require a matching certificate and DNS configuration. That page also contains
an unqualified ICP-filing warning; its application to Johor was not verified.
Do not equate a generated URL with verified public HTTPS.

The smallest next test is an authenticated HTTPS endpoint on the actual account.
Resolve the default certificate, any domain restriction, and the quoted bill
before committing to this provider. The older
[trigger beta notice](https://docs.byteplus.com/en/docs/faas/Creating_a_API_Gateway_trigger)
is dated March 2025; the account's authorization page is newer evidence of the
first setup step only.

A later focused Mira query in the same session found BytePlus-published
[GatewayService configuration](https://www.pulumi.com/registry/packages/bytepluscc/api-docs/apig/gatewayservice/)
with `DefaultDomain`, HTTPS and public-network settings. Its documented
`serviceType` only lists `AIProvider`, so this is not conclusive evidence that
ordinary FaaS routes receive managed HTTPS. The stable-domain FAQ remains valid;
the certificate question was not settled by research.

## Function Service alternative: published cost estimate

Assumptions: 30 days (720 hours), one reserved 0.5-vCPU / 1-GiB function, the
Serverless gateway type, no coupons. Excludes requests, traffic, logs, taxes,
domains/certificates and release overlap. These are calculations from published
rate tables, not an account quote or measurement of Larklish's resource needs.

| Cost | BytePlus Johor | Volcano Johor |
| --- | ---: | ---: |
| Reserved function | USD 18.47/month | RMB 134.40/month |
| Serverless gateway hosting | 0 | 0 |
| Required gateway load balancer | USD 8.64/month | RMB 15.84/month |
| Combined base estimate | **USD 27.11/month** | **RMB 150.24/month** |
| One full day at those settings | USD 0.90 | RMB 5.01 |

Formula: `2,592,000 × (0.5 × reserved-vCPU-rate + reserved-GiB-rate)` plus
`720 × gateway-load-balancer-hourly-rate`.

- [BytePlus function prices](https://docs.byteplus.com/en/docs/faas/Pay-as-you-go):
  reserved CPU USD 0.00000975/vCPU/s; memory USD 0.00000225/GiB/s;
  requests USD 0.0000002 each; function outbound USD 0.11/GiB.
  Its example uses older rates inconsistent with the table; this estimate uses
  the table, and the account quote must settle the actual price.
- [BytePlus gateway prices](https://docs.byteplus.com/en/docs/apig/Billing_overview):
  Serverless hosting waived; Small I load balancer USD 0.012/hour; published
  outbound traffic USD 0.81/GB. Serverless is not an entirely free gateway.
- [Volcano function prices](https://www.volcengine.com/docs/6662/1269135):
  Johor reserved CPU RMB 0.0000709/vCPU/s; memory RMB 0.0000164/GiB/s.
- [Volcano gateway prices](https://www.volcengine.com/docs/6569/185249):
  the **Serverless** tabs specify waived hosting and Johor load balancing at
  RMB 0.022/hour. The Standard gateway tab has different rates.

Volcano also has a separate shared **Elastic Gateway** whose first million
monthly requests are free, with traffic billed separately. Its
[creation guide](https://www.volcengine.com/docs/6569/85693) labels access
invitation-only and lists Johor. It is not the same as the Serverless gateway,
and it does not remove the account-verification prerequisite. Do not assume
BytePlus has identical products, access rules, or prices.

The lower 100-mCPU / 512-MiB configuration would calculate to USD 14.08/month
including the gateway load balancer, before usage charges. The Native API
documents `CpuMilli` but does not establish that this specific reserved-instance
size is accepted in Johor. Do not present it as a verified purchasable option.

## Selected VM: live quote and HTTPS design

Inspected the signed-in ECS purchase form without submitting an order:

| Item | Observed configuration |
| --- | --- |
| Region | Asia Pacific (Johor), selected availability zone A |
| Instance | Shared `ecs.e-c1m1.large`, 2 vCPU / 2 GiB |
| Disk | 20 GiB ESSD PL0 |
| Public access | Elastic IP, BGP multi-line, pay by data transfer, 1 Mbps |
| Configuration quote | USD 0.0193/hour = USD 13.896 per 30 days |
| Outbound internet traffic | USD 0.0810/GB |
| Account prerequisite | Payment authentication incomplete |

This was a configuration quote in an incomplete purchase form; image and SSH
key selection were not completed. It is not a paid order. The deployment plan
targets Ubuntu 24.04 LTS x86-64 and rechecks the complete quote before purchase.
Max prefers below USD 15/month, with up to USD 35/month if necessary for the
demo, and wants the Backend to keep running afterwards.

Max has no domain. A retained Elastic IP gives the phone a stable address.
Standard HTTPS can protect that address: Let's Encrypt's
[IP certificates are generally available](https://letsencrypt.org/2026/01/15/6day-and-ip-general-availability),
and its [Certbot guide](https://letsencrypt.org/2026/03/11/shorter-certs-certbot)
documents `--ip-address`, the `shortlived` profile, and webroot support in
Certbot 5.4 or newer. Certificates last about six days; automatic renewal and
an Nginx reload hook must be tested during setup. See
[Certbot renewal](https://eff-certbot.readthedocs.io/en/stable/man/certbot.html).

The planned path is phone HTTPS → Nginx → Go Backend on port 8787 → Lark API.
systemd supervises the Go process; certificate renewal reloads Nginx without
restarting Go. This preserves the existing protocol and phone trust settings.
IP certificate issuance on the actual BytePlus machine has not been tested yet.

## Deployment tooling

Verified `@volcengine/vefaas-cli` version 0.3.1 through npm and executed its local
`deploy --help` and `gateway --help`. It accepts build/output/start/port settings,
CPU/memory, min/max instances, and `--newApp ... --gatewayName ...`. Its gateway
commands list existing resources. A gateway remains a prerequisite to that
application creation path. See the [official package](https://www.npmjs.com/package/@volcengine/vefaas-cli).

This CLI verification applies to **Volcano Engine**. We did not verify that it
can target BytePlus; Mira's follow-up grouped those findings too loosely.
For BytePlus Function Service, the verified delivery path is local ZIP upload
through the console. The selected VM plan uses the existing `build.sh` and SSH
transfer instead. The internal SCM/ByteFaaS pipeline is not required.

## Earlier VM experiment and completion criteria (superseded)

The [verbatim VM deployment plan](../byteplus-deployment-plan.md) specified payment
verification → VM and retained IP → trusted HTTPS and renewal test → Go Backend
under systemd → authenticated Lookup → in-place phone update → cellular test.
Also verify process restart and machine reboot without changing the public IP.

The existing helper's `backend status` still queries internal SCM/ByteFaaS in
addition to its configured HTTP URL. Simplifying it to the configured Backend
is planned, not implemented. The phone's configured URL remains
`https://jmc8tl6s.fn.bytedance.net` until migration succeeds.

Commit preparation also found that this Mac's `local.properties` lacks a
nonempty `larklish.backendToken`. A subsequent read-only SSH check confirmed the
existing token is still in devbox's
`/data00/home/max.coplan/code/github.com/vegerot/larklish/local.properties`.
The Mac entry was absent before formatting; the token was not deleted or
rotated. The Railway plan reuses this token by copying only the missing property.
This is separate from the phone's existing user access/refresh-token chain.

Research, compilation and local group Lookup checks are complete. No VM, public
IP, certificate, or other cloud resource was created. Payment setup, deployed
HTTPS, regional latency, and phone migration/verification remain pending.
