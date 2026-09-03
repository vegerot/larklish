# 🐦 Larklish — Agent Instructions

Larklish is an Android app. It intercepts Lark notifications, translates the preview
zh→en on-device, and posts an English notification in its place.

Read `docs/plan.md` for the settled decisions and `docs/progress.md` (from the
bottom) for the current state. Do not re-propose the rejected alternatives
(lark-cli polling, bot event subscriptions, third-party cloud translation, Tasker)
unless the situation changes. Read `CONTEXT.md` for the glossary and use its terms.

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
- Format Kotlin with ktfmt before you commit: `./gradlew ktfmtFormat` (`ktfmtCheck` verifies).
- Format `./tools` Python with Ruff before you commit: `uvx ruff format tools`.
- Prefer running commands through the Android Studio MCP unless running `./gradlew` is a lot easier.
- Do not search the home directory or `/` broadly.
- Turn frequently-run one-liners into tools in `./tools`
  + note: don't immediately do it.  Only for common tasks that you have done many times, like `./tools/larklish-helper`, which holds them all as subcommands.
  + For a one-off script, import the helper instead of re-typing its pieces — `tools/larklish-helper` is a
    symlink to `tools/larklish_helper.py`: `import sys; sys.path.insert(0, "tools"); from larklish_helper import read_events, …`.

## 📖 Output Standard 🏁 Goal: text that is clear, unambiguous, and easy for all readers.

- 🔁 Use one meaning per word or emoji. Use the same word or emoji for the same thing every time. Do not use synonyms.
- ❓ Explain an unfamiliar term or abbreviation at first use.

## 📚 References

- Lark Open Platform docs index: <https://open.larkoffice.com/llms.txt> (start here; follow its links)
  - Lark client docs: <https://open.larkoffice.com/document/client-docs/intro.md> (landing page only)
- `lark-cli` source: `~/code/github.com/larksuite/cli/` (use it to learn how Lark works)
  - The `lark-*` skills wrap `lark-cli`. `lark-cli auth status` shows the login state.
