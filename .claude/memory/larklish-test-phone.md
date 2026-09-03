---
name: larklish-test-phone
description: Larklish test device is a rooted company Pixel 4a on LineageOS; Max is fine with doing anything on it over adb
metadata: 
  node_type: memory
  type: project
  originSessionId: 4c03a3c0-9bf0-4e3d-bb84-51c09facf643
  modified: 2026-08-26T00:15:03.402Z
---

The Larklish test phone (as of 2026-08-25) is a **company-owned, rooted (Magisk) Pixel 4a** on LineageOS 23.2 (Android 16, SDK 36), usually connected over `adb`. Max said: "we can do _whatever_ we want on it", including root peeks inside the Lark app. No MDM.

**Why:** Root and `adb` unlock zero-code experiments (`dumpsys notification --noredact`, `cmd notification post`, `cmd notification allow_listener`), so the first onion layers need no app.

**How to apply:** Run `adb` experiments directly instead of asking. Lark's own databases are SQLCipher-encrypted, so root does not expose cached translations. See [[lark-references]].

**Network (2026-09-02):** on the office Wi-Fi `Inspire Creativity` the phone reaches the
intranet without a VPN (`fsopen.bytedance.net` answers 200, and so does a TCP port on a
10.x host), although an internal doc says mobile devices lost intranet access. The
SealSuite VPN app (`com.byteplus.sealsuite`) is installed; with it on and Wi-Fi off, the
phone on cellular still reaches `fsopen.bytedance.net` (200 in 1.1 s). `svc wifi
disable` / `enable` over adb toggles Wi-Fi for such tests; the VPN survives the switch.
