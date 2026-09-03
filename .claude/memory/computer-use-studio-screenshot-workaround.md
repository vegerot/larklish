---
name: computer-use-studio-screenshot-workaround
description: "The computer-use tool's screenshots omit Android Studio's window on Max's Mac even when allowlisted; clicks still land, so drive Studio blind with raw screencapture + Accessibility geometry"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 4c03a3c0-9bf0-4e3d-bb84-51c09facf643
  modified: 2026-08-26T23:17:56.422Z
---

On Max's Mac (2026-08-26), `mcp__computer-use__screenshot` never shows Android Studio's
window, although Studio is allowlisted (tier "click"). Screenshots show only Studio's menu
bar and the wallpaper. Other allowlisted apps (Terminal, Flux Island) render fine. Clicks
on Studio DO land. ScreenCaptureKit captures Studio correctly when asked directly, so the
bug is in the tool's filter, not macOS or Studio. Only difference found: Studio lives in
`~/Applications/Android Studio.app` (user-local), not `/Applications`.

**Why:** Max asked to run the app from Studio's Run button with computer-use; the
screenshots made it look like Studio had no window (wrong theories: other Space,
full-screen on the Dell). Max: "I could see the window on my screen the whole time."

**How to apply:** When Studio is missing from a computer-use screenshot:
1. Trust `screencapture -x -t png FILE` from Bash (then `sips --resampleWidth 1200`) to
   see the real screen; read it with the Read tool.
2. Read geometry, never send events, with `osascript` + System Events (window position,
   menu item positions while the menu is open via a delayed background job).
3. Convert points → screenshot pixels: scale = screenshot width / screen width in points
   (1372 / 1512 = 0.907 on the built-in display); click with `computer_batch`.
4. Menu-bar clicks are blocked by notch apps: quit Alcove (`pkill -x Alcove`, Max OK'd),
   and allowlist "Flux Island" (bundle `com.bytedance.flux.desktop`) so clicks pass through.
5. IDE grants need `request_access` twice in the same turn (tier "click" confirmation).
Verified path: Window menu at (844,15) px, Run button ≈ (921,48) px when the Studio window
sits at points (221,33) 1291×949. Verify results with `adb` (`lastUpdateTime`, signer).
