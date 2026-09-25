# Work-profile Lark notification flow

## Question

After Lark moved into the Android Work profile, could Larklish's personal-profile listener still
receive Originals and post Relays?

## Phone state and repair

On 2026-09-24, `adb shell pm list users` showed personal user 0 and a running Work profile,
user 10. `dumpsys package` showed `com.larksuite.suite` installed only for user 10 and
`com.vegerot.larklish` installed only for user 0. Larklish's notification listener was approved
and bound in user 0.

AirWatch, the Work profile owner, had `permittedNotificationListeners=[]`, which excludes
personal-profile listeners from Work notification events. With Max's permission and phone root,
we changed only that list in `/data/system/users/10/device_policies.xml` to contain
`com.vegerot.larklish`. The previous binary policy is at
`/data/system/users/10/device_policies.xml.before-larklish`. We also granted Lark's
`POST_NOTIFICATIONS` permission and set its `POST_NOTIFICATION` app-op to `allow` for user 10;
beforehand, notifications were disabled. The temporary live Magisk SELinux rule used to edit the
policy was cleared by reboot.

After reboot and profile unlock, `dumpsys device_policy` still showed
`permittedNotificationListeners=[com.vegerot.larklish]`, Lark's notification importance was
`DEFAULT`, and Larklish's personal-profile listener was bound. AirWatch could replace the
listener list during a later policy sync, so this readback proves the current state, not
permanent administrator approval.

## Marked message

We sent exactly one bot message to `Larklish 测试群` through `larklish-helper probe`:
`[probe:7adb3730] 工作资料夹通知测试：你好，Larklish。` Lark accepted message
`om_x100b646a4b1118a4d4f2a36e05cd5ca`.

The Recorder saw the Original under key
`10|com.larksuite.suite|-1580155703|null|1010229`. Larklish posted an English Relay in user 0
about 590 ms after receiving it. `dumpsys notification` showed both the Work-profile Original
and the personal-profile Relay; the Relay tag contained the Original key. This verifies the
cross-profile Original → Relay path on a real Lark notification.

The Update did not succeed. The Recorder logged `Backend /lookup: http 404 404 page not found`.
An independent request returned 404 from the public PPE URL both with and without the PPE
selection header; the former direct PPE trigger returned `function_not_found`. The older Railway
Backend answered `/v1/ping` but rejected the current request shape with HTTP 400. At this point,
the first Relay worked, while Backend translation and Full-text Updates were unavailable. No
second test message had been sent yet.

## Backend recovery later that day

The I18N-TT FaaS control plane returned `service not found for serviceID mmyp0srw`; the
production function `kpb2dvsn` still existed. The public PPE route and former direct trigger
could no longer serve the phone. The cause of the PPE function's removal was not established.

Railway's `larklish/backend` service still ran GitHub commit `7b7e1f9` with the older `/lookup`
contract. The current Backend's focused request-contract and authentication tests passed locally.
The complete Go suite still failed its established `TestReplayCorpus` baseline: 67 of 165
Originals resolved. We uploaded the current `/backend` source from the local working copy to the
existing Railway production service. Deployment `3180aafe-95e7-4c06-995a-ec69da4d6f48`
reached `SUCCESS`; an authenticated complete-Preview request returned HTTP 200 with
`source: preview`. Two earlier CLI attempts did not replace the old healthy deployment: one
failed locally with `prefix not found`, and one archive lacked Railway's configured `/backend`
root and failed during deployment.

We saved `https://backend-production-a712b.up.railway.app` as the phone's Backend URL, without
rebuilding or clearing app data. One new marked message to the test chat,
`[probe:8f8fd653]`, was accepted as `om_x100b646a0da928a4d31924e36cb78ec`. Its Original
had a user-10 key and a cut Preview. The first Relay contained no Han; the Backend returned a
Full-text Update that exactly matched the sent message 6.257 seconds after the Relay, also with
no Han. This verifies the complete Work-profile Original → Relay → Update path over Wi-Fi.

At that point, the Railway deployment came from local Backend files while the connected GitHub
`main` still had the older Backend code. We addressed that source mismatch in the next step.

## Tracked-source Railway deployment

GitHub `main` and ByteDance `main` had diverged after the Backend translation plan. We rebased
the full ByteDance stack onto GitHub `main`; the resulting tree matched the original ByteDance
tip, including the current Backend. We committed this experiment note as `5b52719` and pushed
that tip to both remotes, with a force update needed only for the rewritten ByteDance `main`.

Railway skipped the GitHub webhook because the tip commit changed only docs. A from-source
redeploy then built GitHub commit `5b52719` as deployment
`d528867f-ecc4-473b-bf0e-178908aff5ca`, which reached `SUCCESS`. A fresh phone probe,
`[probe:8711a910]` / `om_x100b646a8a54aca0d319b32cecdb584`, produced a cut Work-profile
Original, English Relay, and matching Full-text Update 6.374 seconds after the Relay. The phone
kept the same Railway URL.

## ByteCloud PPE recovery

The old PPE function `mmyp0srw` remained absent; its removal cause was not established. A new
PPE-only Bits task, `2888453`, pinned SCM `oec/seller/larklish:1.0.0.33` from commit `5b52719`.
Self-test run `1235799827970` succeeded and created function `t648e7x4` in lane
`ppe_deploy_i18n_1`, cluster `faas-sg`, revision `1.0.1` / `p5np3r88d0`. Its direct `/v1/ping`
returned HTTP 200. The existing public PPE route returned 401 without Backend authentication
and HTTP 200 with a valid Backend token and complete Han Preview (`source: preview`, English
without Han). A synthetic cut request with an intentionally invalid user token reached Lark but
could not verify Full text.

For phone acceptance, we temporarily selected the public PPE URL and sent one marked message,
`[probe:89abed8e]` / `om_x100b646a937008a4d343f9004631fae`. Its Work-profile Original had
a cut Preview; Larklish posted an English Relay and a matching Full-text Update through PPE
4.603 seconds later, with no Han. We then restored the phone's saved Railway URL. The
production function and its Bits manual checks were not changed.
