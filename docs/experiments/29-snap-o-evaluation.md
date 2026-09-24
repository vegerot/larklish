# Evaluate Snap-O for Larklish diagnostics

## Question

Would [Snap-O](https://github.com/openai/snap-o) simplify Larklish, or add more complexity to the
Android app and `larklish-helper`?

Snap-O is an Android inspection app. Its screen capture works without an Android dependency. Its
Network and Tweaks tools require libraries in the inspected app; Network also provides a standalone
`snapo-network` CLI.

## Live check

On 2026-09-23, Snap-O opened on the Mac, discovered the connected Pixel 4a and displayed its live
screen. A screenshot initiated in Snap-O completed and appeared in Capture History as an Untitled
Pixel 4a capture. During the first check the phone disappeared from both Snap-O and `adb`; Snap-O
correctly changed to **Waiting for device**. After the USB connection returned, both tools
rediscovered the phone and capture worked again.

The Tool pane showed **No apps found**. This is expected: Larklish does not include a Snap-O
Network, Tweaks or custom-tool integration.

## Fit with Larklish

Screen capture is useful immediately for notification demonstrations and visual evidence. It needs
no Larklish code and does not replace the helper's notification-shade automation.

Snap-O Network is the promising app integration. Larklish opens `HttpURLConnection` connections in
two shared places: `LarkHttp` for phone-to-Lark calls and `Backend` for phone-to-Backend Lookups.
Snap-O supports this API directly. A small experiment can therefore add its real dependency to
debug builds, its no-op dependency to release builds, and wrap those two connection-opening points.
It would expose request timing, status, headers and bodies for one live failure.

This complements rather than replaces the existing diagnostics:

| Tool | Question it answers |
| --- | --- |
| `larklish-helper` | Did the Original → Relay → Update flow work? |
| `FlowTiming` and the Recorder | What durable, content-free evidence did unattended use record? |
| `snapo-network` and the Snap-O Network pane | What happened in this individual Android HTTP transaction? |
| Snap-O Capture | What was visible on the phone? |

Snap-O on Android cannot inspect the Go Backend's outgoing Lark SDK requests. Backend timing and
logs remain necessary.

## Complexity decision

Do not make Snap-O part of Larklish's architecture. A debug-only Network integration is a small,
justified dependency that simplifies live diagnosis, but it still adds Gradle dependencies, a
debug initialization provider, an on-device event buffer and wrapped connections.

Do not wrap `snapo-network` in `larklish-helper`. The helper owns Larklish concepts such as
Originals, Relays, Updates, Lookups, flow correlation and soak grading. A wrapper would add socket
discovery, another process, another output schema and another failure surface without hiding useful
complexity. Run the standalone CLI when it is needed.

Do not add Snap-O Tweaks or a custom tool yet. Tweaks overlaps with the existing editable Backend
URL, and the helper already presents Recorder data well enough. Reconsider either only when a
specific debugging problem justifies it.

## Privacy boundary

Network inspection captures sensitive material: notification text, Full text, Lark user tokens and
the shared Backend bearer token can appear in headers or bodies. Keep the real library in debug
builds, use the no-op release artifact, keep captures local and sanitize anything exported. The
Recorder remains content-free for durable timing evidence.

## Smallest useful experiment

When a real phone HTTP failure warrants it:

1. Add the debug and release-no-op `network-httpurlconnection` dependencies.
2. Wrap only `Backend.post` first.
3. Trigger one synthetic Lookup and verify its request, response and timing in Snap-O or
   `snapo-network`.
4. Confirm tests and behavior are unchanged.
5. Wrap `LarkHttp` only if inspecting phone-to-Lark traffic is then useful.

Stop after the experiment if it does not provide clearer evidence than the existing timing record.
