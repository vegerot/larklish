---
name: lark-translation-api
description: "Lark Open Platform translation API constraints — scope, tenant-token-only, limits, glossary — for the Larklish cloud-fallback decision"
metadata: 
  node_type: memory
  type: reference
  originSessionId: 4c03a3c0-9bf0-4e3d-bb84-51c09facf643
  modified: 2026-08-26T00:20:57.108Z
---

Lark translation API facts (researched 2026-08-25):

- `POST /open-apis/translation/v1/text/translate` — body `{source_language, target_language, text, glossary?}`, response `data.text`. Sibling `POST .../text/detect` → `data.language`.
- Doc: <https://open.larksuite.com/document/uAjLw4CM/ukTMukTMukTM/reference/ai/translation-v1/text/translate>
- Scope `translation:text`. **Tenant token only** → `lark-cli api POST ... --as bot`. No curated lark-cli command exists. Max added the scope to CLI app `cli_aa949bbb72e39cde` on 2026-08-25; the call works.
- Limits: 1,000 chars/call, 20 QPS/tenant, not on the free tier. `glossary` takes up to 128 term pairs (useful for internal jargon).
- Language codes: `zh`, `zh-Hant`, `en` (+13 others).
- No Open Platform API exposes per-chat mute / @mentions-only / DND state.

**How to apply:** Use this when Max decides whether on-device ML Kit quality is good enough or the `LarkApiTranslator` fallback is needed. See [[lark-references]].
