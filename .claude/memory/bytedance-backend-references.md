---
name: bytedance-backend-references
description: "Where the ByteDance deployment advice for the Larklish Backend lives, and the verified Feishu API intranet mirror"
metadata: 
  node_type: memory
  type: reference
  originSessionId: 4c03a3c0-9bf0-4e3d-bb84-51c09facf643
  modified: 2026-09-03T00:46:46.268Z
---

- Expert answers (Mira, ByteDance internal docs assistant) on deploying the Larklish Backend:
  `~/code/github.com/vegerot/expert-answers/` (`mira-bytefaas-guide.md`, `mira-personal-service-deploy.md`, 2026-09-02).
  Recommends ByteFaaS Native HTTP + TSP for secrets + TLB for a public domain; TCE as the fallback for a disk.
- Tika (`bytedcli tika chat`) conversation on the same question: id `2973462677508`. It recommended TCE + persistent volume.
- Feishu Open API intranet mirror: `open.feishu.cn` → `fsopen.bytedance.net` (`open.larksuite.com` → `larkopen.byted.org`).
  Verified 2026-09-02 from the office network, the phone on "Inspire Creativity" Wi-Fi, and inside the IDC: serves the
  tenant-token and translate endpoints. lark-cli hard-codes `open.feishu.cn` by brand (`internal/core/types.go`), so it
  cannot use the mirror without a patch.
- `bytedcli` has full `tsp secret` commands (create, value get/set, versions) and read-only `bytetree`; TLB and PSM
  creation are console-only.

Related: [[lark-references]], [[devbox-adb-tunnel]] (Max: the dev box is not a Backend host).
