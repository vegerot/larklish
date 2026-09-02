"""What `tools/larklish-helper` is built from, importable for the scripts it does not cover.

    import sys; sys.path.insert(0, "tools")
    from larklishlib import phone, record, lark
    for r in record.relays_with_outcomes(phone.read_events()):   # the phone's record
        if r["cut"] and r["outcome"] != "updated (text)":
            print(record.when(r), r["outcome"], r["text"])

Four modules, one per thing the impromptu scripts of a soak review kept re-typing:

- `phone`  — adb: `run`, `adb`, `read_events`, `read_chat_cache`, `debug_hook`, `print_log`, `top`, `shot`
- `record` — events.jsonl: `select`, `relays_with_outcomes`, `preview_message`, `has_han`, `when`, `event_ms`
- `lark`   — the Open API through lark-cli: `cli`, `api`, `chats_search`, `messages_of`, `flat_text`,
             `resolve_chat`, `send`, and the window helpers `parse_when`, `parse_span`
- `corpus` — the replay TSV format ReplayTest.kt reads: `esc`, `message_row`, the separators

Nothing here parses arguments; that is the helper's job.
"""

from . import corpus, lark, phone, record

__all__ = ["corpus", "lark", "phone", "record"]
