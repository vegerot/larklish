# 📸 Showcase

Real captures of the Pixel 4a's notification shade, 2026-09-03. Each pair is one message
posted to `Larklish 测试群` by the bot; the left card is Lark's Original, the right card is
Larklish's Relay, cropped from the same shade and placed side by side (`tools/showcase.py`).

| File | What it shows |
| --- | --- |
| `01-chinese.png` | A Chinese alert → English, with `P99`, `300ms`, `customFetch`, `MR #3245` intact. Lark's process was recently active, so the Original arrived whole and the phone translated the Preview itself. |
| `02-english.png` | A 280-character English message. Lark cut it at 45 characters (`release train...`); the Backend found the message and the Relay shows all of it. |
| `03-mention.png` | A Chinese message that @-mentions Max. Lark cut it; the Relay keeps `@you`, `@Max Coplan`, the MR number and the code names, in English. |

The `-lark.png` / `-larklish.png` files are the two halves on their own.
