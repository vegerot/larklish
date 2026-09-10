# 🧪 Experiment 18 — Record VPN state and Wi-Fi SSID in debug builds

The September 3–9 soak showed connection failures, but no network state to explain
them. Max asked to record VPN state and the Wi-Fi SSID, with elevated permissions
limited to debug builds.

## Record

Every event written by the debug listener's Recorder now includes:

```json
"network": {
  "vpn": false,
  "wifiConnected": true,
  "wifiSsid": "Inspire Creativity"
}
```

- `vpn` says whether Android reports a VPN on this app's default network. It does
  not establish that the VPN reaches the corporate network, or identify the VPN app.
- `wifiConnected` records a visible Wi-Fi connection, including Wi-Fi beneath a VPN.
- `wifiSsid` is the connected network's name, with Android's surrounding quotes
  removed. It is JSON `null` when disconnected, redacted, or waiting for the first
  callback. `wifiConnected: true` with `wifiSsid: null` means the name is unavailable;
  do not classify that as cellular.

The snapshot is taken at each recorded Relay, Update, skip/error, fallback, and
removal. It is the state when the event is recorded, not a history of every network
transition during a request. No SSID or network snapshot is sent to the Backend.
Release builds omit `network` entirely and do not register the diagnostic callback.

## Phone experiment before implementation

A temporary app on the Pixel compared three ways to read the SSID:

| API | Location permission absent | Location permission granted |
| --- | --- | --- |
| `ConnectivityManager.getNetworkCapabilities` | `<unknown ssid>` | `<unknown ssid>` |
| Wi-Fi callback with `FLAG_INCLUDE_LOCATION_INFO` | `<unknown ssid>` | `"Inspire Creativity"` |
| Deprecated `WifiManager.getConnectionInfo` | `<unknown ssid>` | `"Inspire Creativity"` |

The implementation uses the callback. It observes physical Wi-Fi separately from
the default network, so a VPN does not hide the SSID. The listener owns the callback
and unregisters it when destroyed. Android documents the
[callback's location flag](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback#FLAG_INCLUDE_LOCATION_INFO)
and [SSID redaction](https://developer.android.com/reference/android/net/wifi/WifiInfo).

## Permissions and setup

`app/src/debug/AndroidManifest.xml` declares network-state access and coarse, fine,
and background location access. Location access is needed because Android treats
the SSID as location data; background access lets the listener read it with the app
in the background. The phone's Location setting must also be enabled.

After installing a debug build on the test phone:

```sh
adb shell pm grant com.vegerot.larklish android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.vegerot.larklish android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.vegerot.larklish android.permission.ACCESS_BACKGROUND_LOCATION
adb shell cmd location is-location-enabled
```

If the listener registered before the grants and the name remains unavailable,
rebind it to obtain a fresh callback:

```sh
adb shell cmd notification disallow_listener com.vegerot.larklish/.LarkListener
adb shell cmd notification allow_listener com.vegerot.larklish/.LarkListener
```

The release APK contains **no location permissions**. Its existing normal
`ACCESS_NETWORK_STATE` permission comes from `com.google.mlkit:translate:17.0.3`;
that is unchanged. Network collection itself is in the debug source set, with a
release implementation that returns no snapshot.

## Validation

- Fetched the current source from ByteDance and rebased to `f31e72a`. The dev box
  had called GitHub `origin`; its remotes now match the other machine: ByteDance is
  `origin`, GitHub is `github`, and `main` tracks `origin/main`.
- Debug and release APKs built; all 21 Kotlin unit tests passed; ktfmt passed.
- Inspected both APKs' merged permissions and traced the release network-state
  permission to ML Kit. No location permission appears in release.
- Installed the debug APK with the matching signer and granted the debug location
  permissions. The existing token file and event record were retained.
- Two new instrumented tests passed on the Pixel. One verifies a Recorder sees
  different network snapshots for a Relay, Update, and subsequent error while
  retaining the existing truncation and Backend fields. The other reads the actual
  SSID `Inspire Creativity` through the callback without `ACCESS_WIFI_STATE`.
- The listener was bound after testing; all 1,909 existing events were retained.
  No new Original had arrived at the final check, so the production record has no
  network-tagged event yet. The phone had no active VPN during the live test; a
  live VPN transition has not been exercised by this experiment.

```sh
JAVA_HOME=~/.jdks/jdk-21.0.12.1+1 ./gradlew \
  ktfmtFormat ktfmtCheck testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest
adb shell am instrument -w \
  -e class com.vegerot.larklish.NetworkStateTest \
  -e expectedSsid '"Inspire Creativity"' \
  com.vegerot.larklish.test/androidx.test.runner.AndroidJUnitRunner
```

The SSID test is skipped without `expectedSsid`; pass the phone's current SSID when
running it. Next: grade new traffic by `network.vpn` and `network.wifiSsid` to
distinguish off-network failures from failures while corporate access is available.
