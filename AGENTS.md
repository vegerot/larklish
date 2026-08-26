# 🐦 Larklish — Agent Instructions

Larklish is an Android app. It intercepts Lark notifications, translates the preview
zh→en on-device, and posts an English notification in its place.

Read `larklish-handoff.md` for the settled decisions. Avoid re-proposing the
rejected alternatives listed there unless the situation changes and an
alternative becomes more attractive. Read `CONTEXT.md` for the glossary and use
its terms.

## 📱 Project facts

- Package: `com.vegerot.larklish`
- Lark package on the test phone: `com.larksuite.suite` (international Lark)
- Test phone: Pixel 4a, LineageOS 23.2 (Android 16, SDK 36), rooted (Magisk), Play Services present
- Lark notifications use `BigTextStyle`: `title` = group name, `text` = `Sender: message`
- `minSdk = 36`. Support only the test phone.

## 🧅 Evolutionary design

- Grow the app in layers like an onion. Start from the smallest version that works.
- The first layers are throwaway experiments. Commit them anyway.
- 🚫 Do not preserve backwards compatibility. Remove obsolete paths. Do not add
  compatibility layers, fallbacks, or migrations. Radically refactor as the design evolves.
- 🪶 Choose the simplest implementation that meets the requirements. Avoid over-engineering.
- 😌 Don't be paranoid. Handle an error only if it is likely in practice. Otherwise assert it
  and add a comment that says why it is unlikely.
- 🧪 Before you commit to a plan, run a small experiment to validate the approach.
- 🧩 Keep components modular and concerns clearly separated.

## 🛠️ Commands

- The phone is connected over `adb`. Use it directly for experiments.
- Prefer `--long-flag` names over `-s`hort flags.
- Break long shell commands into multiple lines with `\`.
- Prefer the fff tools for file search and grep. In Bash, prefer `rg` and `fd` over `grep`
  and `find`.
- Do not search the home directory or `/` broadly.

## 📖 Output standard

Write all text (responses, docs, comments, UI text) in ASD-STE100 Simplified Technical
English, with emojis 🙏🏼.

- Use short, clear sentences: max 20 words for instructions, max 25 for descriptions.
- One instruction or one main idea per sentence. Active voice. Imperative for instructions.
- One meaning per word. Use the same word for the same thing every time.
- One topic per paragraph, max six sentences, main topic first.
- Explain an unfamiliar term or abbreviation at first use.

## 📚 References

- Lark Open Platform docs index: <https://open.larkoffice.com/llms.txt> (start here; follow its links)
  - Lark client docs: <https://open.larkoffice.com/document/client-docs/intro.md> (landing page only)
- `lark-cli` source: `~/code/github.com/larksuite/cli/` (use it to learn how Lark works)
  - The `lark-*` skills wrap `lark-cli`. `lark-cli auth status` shows the login state.
