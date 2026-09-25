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

The Railway deployment came from local Backend files, while the connected GitHub `main` still
had the older Backend code. A future GitHub deployment could replace this working version; align
the tracked source and deployment workflow before relying on GitHub auto-deploys again.
