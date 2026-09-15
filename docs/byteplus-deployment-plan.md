# Deploy Larklish on one small BytePlus VM

## 1. Decision and success criteria

**Use a BytePlus VM with a retained Elastic IP, Nginx for HTTPS, Certbot for certificates, and systemd to run the existing Go Backend.**

This best fits the [project ethos](/Users/bytedance/.codex/AGENTS.md): a small, understandable system, a concrete price, standard components, and a short experiment to verify the remaining uncertainty.

- **Target:** working ASAP, then remain running for daily use.
- **Cost:** the observed configuration quote is **$0.0193/hour**, approximately **$13.90 per 30 days**, plus traffic and taxes. Recheck before purchase; aim below $15/month, with $35/month available if necessary for the demo.
- **Stable address:** `https://<Elastic-IP>`. Retain this IP across deployments and machine restarts.
- **Application interface:** preserve the existing JSON protocol and Bearer authentication. No custom encryption or Android trust changes.
- **Completion:** a real Original produces its Relay and Full-text Update over cellular, with Wi-Fi and VPN off.

Already verified: Linux compilation succeeds; ordinary Go tests pass; existing user-token Lookup succeeds through both public Lark API hostnames. Local Backend memory usage was approximately 17 MiB.

## 2. Provision the machine and prove HTTPS first

### Account and VM

1. Complete BytePlus payment verification and the required account prompts.
2. Create one **pay-as-you-go** instance in **Johor**, using:
   - Instance: `ecs.e-c1m1.large`, shared **2 vCPU / 2 GiB**.
   - Image: **Ubuntu 24.04 LTS, x86-64**.
   - System disk: **20 GiB ESSD PL0**.
   - Public networking: one retained **Elastic IP**, traffic-based billing, **1 Mbps** bandwidth limit.
   - SSH: import the existing public key at `/Users/bytedance/.ssh/id_rsa.pub`.
3. Configure the security group for public TCP **80 and 443**, and SSH **22** from the administering machine’s public IP. Keep the Backend’s port private.
4. Record the instance ID, region, IP allocation, and complete price breakdown. Preserve all existing internal deployments.

### Small HTTPS experiment

Before supplying Lark credentials:

1. Install Nginx and a supported Certbot release, **version 5.4 or newer**.
2. Configure Nginx on port 80 to serve Certbot’s HTTP validation files.
3. Test certificate issuance using Certbot’s webroot mode, `--ip-address`, and the `shortlived` profile; then obtain the production certificate.
4. Configure HTTPS on port 443 and verify the IP address with normal certificate validation from the Mac and Pixel. Do not bypass certificate errors.

Let’s Encrypt explicitly supports publicly trusted IP certificates, so this requires no domain purchase. [Official issuance instructions](https://letsencrypt.org/2026/03/11/shorter-certs-certbot).

5. Verify Certbot’s automatic renewal timer is enabled. Configure a successful-renewal hook to **reload Nginx**, leaving the Go process running.
6. Run `certbot renew --dry-run --run-deploy-hooks` and verify success. IP certificates last approximately six days, making renewal part of the initial acceptance test. [Certbot renewal documentation](https://eff-certbot.readthedocs.io/en/stable/man/certbot.html).

## 3. Deploy the existing Backend

1. Run the existing `./build.sh` and record the executable’s checksum and source revision.
2. Transfer the executable over SSH and install it under `/opt/larklish/`.
3. Create an unprivileged `larklish` service account.
4. Store the existing credentials in a root-readable environment file:
   - `LARK_APP_ID`
   - `LARK_APP_SECRET`
   - `LARKLISH_BACKEND_TOKEN`
   - `LARK_HOST=https://open.larksuite.com`

   The international hostname passed our real group Lookup experiment. The phone continues managing its own user-token chain.

5. Configure a systemd service to:
   - Run `/opt/larklish/larklish-backend -port 8787 -props /dev/null`.
   - Start automatically at boot.
   - Restart after failure.
   - Write application logs to the system journal.

6. Configure Nginx to forward HTTPS requests to `127.0.0.1:8787`, preserving paths, request bodies, and the Authorization header. Use a **35-second proxy read timeout**, allowing the phone’s existing 30-second timeout to remain authoritative.
7. Preserve the existing routes:
   - `GET /v1/ping`: public health check.
   - `POST /lookup`: existing Bearer authentication.
   - `GET /chats`: existing Bearer authentication.

Keep the Nginx and systemd configuration files with a short deployment recipe in the repository. The initial deployment requires no Go or Kotlin application changes.

## 4. Verify the Backend, then move the phone

### Backend acceptance

- HTTPS validates normally and `/v1/ping` identifies a Linux/amd64 Backend.
- Missing or incorrect Bearer tokens return **401**.
- A valid token reaches Lookup validation; incomplete JSON returns **400**.
- Replay the existing test Original with the phone’s current access token. Require `found`, the expected Full text, and English.
- Exercise one uncached Lookup, repeated cached Lookups, and three concurrent requests. Require completion within the phone’s 30-second timeout.
- Confirm the service stays running through an idle period.
- Simulate one process failure and verify automatic restart.
- Reboot once and verify the same IP, valid HTTPS, and automatic Backend startup.

The existing corpus replay currently resolves **67/165** and fails its historical threshold. Record that baseline; do not change Lookup logic or weaken the test as part of hosting.

### Phone migration

1. Retain the currently installed APK and old Backend URL for rollback.
2. Confirm the new APK uses the installed app’s signing identity.
3. Change only `larklish.backendUrl` in the gitignored configuration, then build and install **in place**. Preserve app data and `user-token.json`.
4. Send clearly labeled synthetic messages to the existing **Larklish 测试群**:
   - A short message.
   - A long Chinese message that produces a truncated Original.
   - A small burst.
5. Verify the long-message sequence: Original → prompt Relay → Full-text Update.
6. Repeat the long-message test on **cellular with Wi-Fi and VPN disabled**. Confirm the Recorder identifies the new HTTPS Backend.
7. Check an existing direct-message Lookup and dismissal behavior.

If migration fails, restore the previous APK/configuration without uninstalling or clearing app data.

## 5. Leave it maintainable

- Update the existing helper’s `backend status` to report the configured HTTP Backend, removing its hardcoded SCM/ByteFaaS queries.
- Document routine updates: build, transfer, replace executable, restart service, verify health and authenticated Lookup. Retain the previous executable for rollback.
- Verify Ubuntu’s automatic security updates and Certbot renewal are active.
- Configure a monthly billing warning at **$15** and document checking the first complete daily bill. Billing warnings are not a hard spending cap.
- Record the final URL, resource IDs, configuration, checksums, measured timings, certificate-renewal test, and cellular evidence in the project records.
- Keep the authenticated internal-production TODO separate.

**No new deployment state changes are made during planning.** Implementation proceeds through HTTPS, Backend, and phone verification in that order, fixing observed failures before advancing.
