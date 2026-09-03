---
name: read-docs-after-compaction
description: "After every context compaction, fully read CLAUDE.md, docs/plan.md and docs/progress.md before doing anything else"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 4c03a3c0-9bf0-4e3d-bb84-51c09facf643
  modified: 2026-09-02T21:17:20.298Z
---

After every context compaction, read `CLAUDE.md`, `docs/plan.md` and `docs/progress.md` in full (not a tail) before acting. Max asked for this explicitly on 2026-09-02.

**Why:** the summary drops settled facts (rejected alternatives, the `not-truncated` bucket being by design, which `...` is Lark's) and the plan's working rules ("stop at the end of each layer", "check the working copy against the last entry"). Acting from the summary alone re-proposes rejected ideas.

**How to apply:** `cat CLAUDE.md docs/plan.md`, then Read `docs/progress.md` in ~700-line chunks (it is >1400 lines). Then `sl status` against the last `Next:` block. Related: [[larklish-test-phone]].
