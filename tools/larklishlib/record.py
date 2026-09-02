"""The Relay record: `events.jsonl`, one line per Relay, Update, skip, fallback and removal."""

from datetime import datetime
from typing import Any

from .phone import Event

Relay = dict[str, Any]  # an Event of kind `relayed`, plus `outcome` and `cut` (relays_with_outcomes)

REASONS = {  # NotificationListenerService.REASON_*
    1: "click", 2: "cancel", 3: "cancel_all", 8: "app_cancel", 9: "app_cancel_all",
    10: "listener_cancel", 12: "group_summary_canceled", 18: "snoozed", 19: "timeout",
}


def has_han(s: str) -> bool:
    return any("一" <= c <= "鿿" or "㐀" <= c <= "䶿" for c in s)


def is_fallback(e: Event) -> bool:
    # The `~` marks the translated part: the title, or the message after `Sender: `.
    return e["relayTitle"].startswith("~") or e["relayText"].split(": ", 1)[-1].startswith("~")


def when(e: Event, utc: bool = False) -> str:
    """An event's time as `MM-DD HH:MM`, local unless `utc`."""
    t = datetime.fromisoformat(e["at"].replace("Z", "+00:00"))
    if not utc:
        t = t.astimezone()
    return t.strftime("%m-%d %H:%M")


def event_ms(e: Event) -> int:
    return int(datetime.fromisoformat(e["at"].replace("Z", "+00:00")).timestamp() * 1000)


def event_chat(e: Event) -> str:
    return e["key"].split("|")[2]


def one_line(s: str) -> str:
    return s.replace("\n", " ⏎ ")


def preview_message(text: str) -> str:
    """The message part of an Original's text, as `Preview.parse` sees it (after the first `: `)."""
    sender, sep, message = text.partition(": ")
    return message if sep else text


def preview_sender(text: str) -> str:
    if text.endswith(":...") and ": " not in text:
        text = text[:-3] + " ..."
    i = text.find(": ")
    if i < 0:
        return ""
    head = text[:i]
    return head[:-4] if head.endswith(("@you", "@all")) else head


def select(events: list[Event], since: str | None = None, han: bool = False, no_removed: bool = False) -> list[Event]:
    """`since` is an ISO UTC prefix; `han` keeps only Relays with Han in the Original."""
    out = []
    for e in events:
        if since and e["at"] < since:
            continue
        relayed = e["event"] == "relayed"
        if e["event"] == "removed" and no_removed:
            continue
        if han and not (relayed and (has_han(e["text"]) or has_han(e["title"]))):
            continue
        out.append(e)
    return out


def relays_with_outcomes(events: list[Event]) -> list[Relay]:
    """Each Relay with what the Update did to it: `updated (<msgType>)`, `skipped <reason>`, or
    `canceled` (a newer Original took the key before the fetch returned), and `cut` — whether Lark
    cut the Preview. Outcomes are keyed by the Original, so they pair by key, in order."""
    relays: list[Relay] = []
    open_by_key: dict[str, Relay] = {}
    for e in events:
        kind = e["event"]
        if kind == "relayed":
            r: Relay = dict(e, outcome="canceled", cut=preview_message(e["text"]).endswith("..."))
            relays.append(r)
            open_by_key[e["key"]] = r
        elif kind == "updated" and e["key"] in open_by_key:
            open_by_key[e["key"]]["outcome"] = f"updated ({e['msgType']})"
        elif kind == "skipped" and e["key"] in open_by_key:
            reason = e["reason"]
            open_by_key[e["key"]]["outcome"] = "skipped " + (reason.split(":")[0] if reason.startswith("error") else reason)
    return relays
