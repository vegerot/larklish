# Public-cloud demo hosting research — 2026-09-15

## Current decision

Max selected **Railway with one always-running US West service and managed HTTPS**
for the immediate demo and ongoing daily use. Use California (`us-west2`) and
deploy from GitHub `vegerot/larklish` on `main`, with service root `/backend` and
Backend-only watch paths. Follow the
[saved Railway deployment plan](../railway-deployment-plan.md).
[Experiment 25](25-railway-fly-comparison.md) compares Railway, Fly and the
earlier BytePlus VM proposal, including the cache and sleeping options.
[Experiment 24](24-volcano-byteplus.md) retains the BytePlus account checks, VM
quote, employee-credit research and build/API experiments.
[Experiment 26](26-railway-deployment.md) records the completed Railway deployment
and cellular verification, including the switch to verified Trial and native
service settings required by the live platform.
The local-upload and Singapore sketch below records the initial proposal; the
current plan uses Railway's native GitHub deployment integration and US West.

## Initial recommendation

Max chose public-cloud hosting for the immediate demo, superseding the temporary
internal PPE/public-route plan. At this initial comparison, the provider had not
been selected. The recommendation was Railway for the shortest deployment path
from this checkout; DigitalOcean App Platform was the simplest of the named
options (GCP, Volcano Engine, DigitalOcean, and the subsequent BytePlus
comparison). This is a source-based comparison, not a completed deployment.

## What the Backend needs

Inspected `backend/main.go`, `server.go`, `config.go`, `go.mod`, and the build plan:

- One ordinary Go HTTP process; Go 1.25 in `go.mod`, public SDK dependency.
- No database or persistent disk. The in-memory chat cache is learned again after
  a restart; the user refresh-token chain stays on the phone.
- Keep one instance running, preserving the existing preference against idle
  shutdown and cold starts. Hosting cannot guarantee that a process never restarts.
- `PORT` is supported, and the listener binds on all interfaces.
- Runtime variables: `LARK_APP_ID`, `LARK_APP_SECRET`,
  `LARKLISH_BACKEND_TOKEN`, and `LARK_HOST`. The initial public host was
  `https://open.feishu.cn`; Experiment 24 verified an existing group Lookup
  through `https://open.larksuite.com`, now selected for the Railway plan.
  Public hosting needs the public API host, not the IDC's intranet mirror.
- Existing Bearer authentication protects `/lookup` and `/chats`;
  `/v1/ping` is the public health probe.

## Initial comparison

| Platform | Deployment and operational work | Price / assessment |
| --- | --- | --- |
| Railway | Upload the Backend directory with its CLI; Railpack detects and builds Go. Generate a managed HTTPS domain. Configure one replica with Serverless disabled. | Hobby has a $5/month minimum including $5 of usage; excess usage costs extra. Simplest for this checkout because no source-repository integration is necessary. |
| DigitalOcean App Platform | Connect a supported repository or supply a container image; set source directory to `backend`, environment variables, and health probe. Go 1.25 is supported. | $5/month for one shared-CPU, 512 MiB container with 50 GiB transfer. Straightforward fixed-size hosting; source delivery is an additional step from the current internal origin. |
| GCP Cloud Run | Deploy local Go source with `gcloud run deploy --source`; Google builds the image. Set minimum and maximum instances to one. Requires project, billing, APIs, and build/deployment permissions. | Usage billing; a minimum instance costs money while idle. Good fit, but more initial setup unless a suitable project already exists. |
| Volcano Engine veFaaS | Native runtime accepts a compiled Go application. The documented public-access path adds API Gateway configuration and a function trigger. | Technically suitable; more components to configure for this demo. No price estimate established. |
| BytePlus Function Service | Native Web application runs the Go HTTP process; configure startup/port, release with min/max one, then connect an API Gateway instance/service and trigger. | Similar setup shape to Volcano Engine. Reserved instances are billed continuously; a reliable total price was not established. The trigger guide still labels gateway access beta, but its March 2025 date makes current account availability uncertain. |

The ranking assumes a new usable account on each platform. Existing configured
accounts may change which is quickest. No account entitlements were checked
during this initial comparison; Experiment 24 adds the account observations.

## Initial BytePlus Function Service follow-up

The suitable managed option is Function Service with the Native Web application
runtime and API Gateway. The native runtime forwards ordinary HTTP requests to
the Go process. Its documented port variable is `_FAAS_RUNTIME_PORT`; our code
supports `PORT` and `-port`, so matching the configured listening port through
the existing flag avoids a code change. Use the same public Lark host and secrets.

The release controls support reserved instances, allowing min/max one. The
region page lists Asia Pacific (Johor), `ap-southeast-1`; this page is dated
March 2025 and was not checked against a logged-in console.

The gateway trigger requires a released function, gateway instance, gateway
service, and route. Its guide says API Gateway and the trigger are beta and
directs users to their customer manager. That is an observed documentation
statement, not proof that Max's account is blocked today. Some newer setup and
billing pages did not expose their body text through the documentation reader;
current access, exact pricing, and HTTPS certificate provisioning remain unverified.

At this stage, the recommendation remained Railway first for simplest
local-source deployment, DigitalOcean App Platform second, Cloud Run next,
with BytePlus and Volcano Engine requiring more setup. If Max already has
BytePlus Function Service and API Gateway enabled, its similarity to the
existing deployment makes it a reasonable choice.
No internal release-ticket workflow was inferred to apply to public BytePlus.

## Initial Railway deployment sketch

The [current deployment plan](../railway-deployment-plan.md) expands and updates
this initial sketch, including the existing token on devbox and phone migration.

1. Create one project/service and add the four runtime variables above. Keep
   credential values out of source and terminal output.
2. Upload only `backend` as the build root. The CLI supports
   `railway up backend --path-as-root`; subsequent deployments use the same path.
3. Select one Singapore replica (available on Railway), disable Serverless, set
   the health probe to `/v1/ping`, and generate the public HTTPS domain.
   Singapore continues the existing deployment location; latency is unmeasured.
4. Verify missing/wrong/correct Bearer behavior and replay an existing test
   message through `/lookup`, including successful calls to `open.feishu.cn`.
5. Set the phone's Backend URL, install as an update preserving app data and its
   token chain, then verify on cellular with Wi-Fi and VPN disabled.

No application rewrite is expected from source inspection. The build, public
Lark API access from the selected region, actual resource use, and phone path
still require the deployment experiment. No cloud resources, credentials, phone
configuration, internal tickets, or existing deployments changed in this research.
The previously retained authenticated internal production TODO remains separate.

## Official sources

- [Railway CLI deployment](https://docs.railway.com/cli/up)
- [Railpack Go detection and build](https://railpack.com/languages/golang/)
- [Railway HTTPS and domains](https://docs.railway.com/networking/public-networking)
- [Railway pricing](https://docs.railway.com/pricing/plans)
- [Railway Serverless setting](https://docs.railway.com/deployments/serverless)
- [Railway regions](https://docs.railway.com/deployments/regions)
- [DigitalOcean quickstart](https://docs.digitalocean.com/products/app-platform/getting-started/quickstart/)
- [DigitalOcean Go support](https://docs.digitalocean.com/products/app-platform/reference/buildpacks/go/)
- [DigitalOcean pricing](https://docs.digitalocean.com/products/app-platform/details/pricing/)
- [Cloud Run source deployment and permissions](https://docs.cloud.google.com/run/docs/deploying-source-code)
- [Cloud Run minimum instances and idle billing](https://docs.cloud.google.com/run/docs/configuring/min-instances)
- [veFaaS Native code-package deployment](https://www.volcengine.com/docs/6662/1206695)
- [veFaaS CLI hosting workflow and gateway prerequisite](https://www.volcengine.com/docs/6662/760348)
- [API Gateway integration with veFaaS](https://www.volcengine.com/docs/6569/111770)
- [BytePlus Native Web application contract](https://docs.byteplus.com/en/docs/faas/Native_runtime_Development_methods_Web_applications)
- [BytePlus release and reserved-instance controls](https://docs.byteplus.com/en/docs/faas/Release_function)
- [BytePlus API Gateway trigger and access note](https://docs.byteplus.com/en/docs/faas/Creating_a_API_Gateway_trigger)
- [BytePlus Function Service regions](https://docs.byteplus.com/en/docs/faas/Regions_and_availability_zones)
