"""What `tools/larklish-helper` is built from, importable for the scripts it does not cover.

    import sys; sys.path.insert(0, "tools")
    from larklishlib import read_events, relays_with_outcomes, when
    for r in relays_with_outcomes(read_events()):        # the phone's record
        if r["cut"] and r["outcome"] != "updated (text)":
            print(when(r), r["outcome"], r["text"])

Three groups, each one thing the impromptu scripts of a soak review kept re-typing:

- the phone: `adb`, `read_events`, `read_chat_cache`, `send`, `debug_hook`, `print_log`
- the record: `select`, `relays_with_outcomes`, `preview_message`, `has_han`, `when`, `event_ms`
- the Open API: `lark_call`, `lark_api`, `chats_search`, `messages_of`, `flat_text`, `resolve_chat`,
  and the time helpers `parse_when`, `parse_span`

Nothing here parses arguments; that is the helper's job.
"""

import json
import pathlib
import re
import shlex
import subprocess
import sys
from datetime import datetime

PACKAGE = "com.vegerot.larklish"
TEST_CHAT = "oc_e2feee77ba20b6c5285d22fb8aa90eb4"  # Larklish 测试群
TEST_TITLE = "Larklish 测试群"
LOG_TAGS = ["LarkListener:I", "MessageFetcher:I", "Translator:I", "FallbackTranslator:W", "*:S"]
REASONS = {  # NotificationListenerService.REASON_*
    1: "click", 2: "cancel", 3: "cancel_all", 8: "app_cancel", 9: "app_cancel_all",
    10: "listener_cancel", 12: "group_summary_canceled", 18: "snoozed", 19: "timeout",
}


# ── the phone ─────────────────────────────────────────────────────────────────
def run(args, **kw):
    """Run a command and fail loudly with whatever it said."""
    out = subprocess.run(args, capture_output=True, text=True, **kw)
    if out.returncode != 0:
        sys.exit(f"{args[0]}: {(out.stderr or out.stdout).strip()}")
    return out.stdout


def adb(*args, allow_fail=False):
    out = subprocess.run(["adb", *args], capture_output=True, text=True)
    if out.returncode != 0 and not allow_fail:
        sys.exit(f"adb {' '.join(args)}: {(out.stderr or out.stdout).strip()}")
    return out


def require_device():
    """`adb logcat` waits forever when no phone is attached, so check before anything blocks."""
    out = subprocess.run(["adb", "devices"], capture_output=True, text=True)
    if not [ln for ln in out.stdout.splitlines()[1:] if ln.strip().endswith("\tdevice")]:
        sys.exit("no phone on adb — check the cable, then `adb devices`")


def read_events(path=None):
    """`filesDir/events.jsonl` (written by Recorder.kt) from the phone, or a saved copy."""
    if path:
        text = pathlib.Path(path).read_text(encoding="utf-8")
    else:
        require_device()
        text = adb("exec-out", "run-as", PACKAGE, "cat", "files/events.jsonl").stdout
    return [json.loads(line) for line in text.splitlines() if line.strip()]


def read_chat_cache(path=None):
    """`filesDir/chats.json` (written by MessageFetcher): title → chat id, `dm:<Sender>` → chat id.
    Empty when nothing has been resolved yet."""
    if path:
        raw = pathlib.Path(path).read_text(encoding="utf-8")
    else:
        # `adb exec-out` folds the device's stderr into stdout, so a missing file arrives as text.
        raw = adb("exec-out", "run-as", PACKAGE, "cat", "files/chats.json", allow_fail=True).stdout
    raw = raw.strip()
    return json.loads(raw) if raw.startswith("{") else {}


def send(text, reply_to=None):
    """Post to the test group as the bot. Returns the new message id."""
    args = ["lark-cli", "im", "+messages-reply", "--message-id", reply_to, "--reply-in-thread"] if reply_to \
        else ["lark-cli", "im", "+messages-send", "--chat-id", TEST_CHAT]
    return json.loads(run(args + ["--as", "bot", "--text", text]))["data"]["message_id"]


def debug_hook(what, title, text):
    """A MainActivity debug hook: user, refresh, fetch or thread."""
    # `adb shell` hands one string to the phone's shell, which splits it again: a title with a
    # space silently truncates unless every value is quoted for *that* shell. CLEAR_TOP|NEW_TASK
    # because MainActivity reads the hook in onCreate — a running instance must be recreated.
    adb("shell", " ".join([
        "am", "start", "-f", "0x10008000", "-n", f"{PACKAGE}/.MainActivity",
        "--es", "debug", shlex.quote(what),
        "--es", "title", shlex.quote(title),
        "--es", "text", shlex.quote(text),
    ]))


def print_log():
    """The app's logcat so far, time + level + tag + message."""
    for line in adb("logcat", "-d", "-s", *LOG_TAGS).stdout.splitlines():
        parts = line.split(None, 5)  # date time pid tid level tag: message
        print(" ".join(parts[1:2] + parts[4:]) if len(parts) > 5 else line)


def top():
    """The foreground activity."""
    dump = adb("shell", "dumpsys", "activity", "activities").stdout
    m = re.search(r"topResumedActivity=ActivityRecord\{\S+ u0 (\S+)", dump)
    return m.group(1) if m else "?"


def shot(path):
    with open(path, "wb") as f:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], check=True, stdout=f)


# ── the record ────────────────────────────────────────────────────────────────
def has_han(s):
    return any("一" <= c <= "鿿" or "㐀" <= c <= "䶿" for c in s)


def is_fallback(e):
    # The `~` marks the translated part: the title, or the message after `Sender: `.
    return e["relayTitle"].startswith("~") or e["relayText"].split(": ", 1)[-1].startswith("~")


def when(e, utc=False):
    """An event's time as `MM-DD HH:MM`, local unless `utc`."""
    t = datetime.fromisoformat(e["at"].replace("Z", "+00:00"))
    if not utc:
        t = t.astimezone()
    return t.strftime("%m-%d %H:%M")


def event_ms(e):
    return int(datetime.fromisoformat(e["at"].replace("Z", "+00:00")).timestamp() * 1000)


def event_chat(e):
    return e["key"].split("|")[2]


def one_line(s):
    return s.replace("\n", " ⏎ ")


def preview_message(text):
    """The message part of an Original's text, as `Preview.parse` sees it (after the first `: `)."""
    sender, sep, message = text.partition(": ")
    return message if sep else text


def preview_sender(text):
    if text.endswith(":...") and ": " not in text:
        text = text[:-3] + " ..."
    i = text.find(": ")
    if i < 0:
        return ""
    head = text[:i]
    return head[:-4] if head.endswith(("@you", "@all")) else head


def select(events, since=None, han=False, no_removed=False):
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


def relays_with_outcomes(events):
    """Each Relay with what the Update did to it: `updated (<msgType>)`, `skipped <reason>`, or
    `canceled` (a newer Original took the key before the fetch returned), and `cut` — whether Lark
    cut the Preview. Outcomes are keyed by the Original, so they pair by key, in order."""
    relays, open_by_key = [], {}
    for e in events:
        kind = e["event"]
        if kind == "relayed":
            r = dict(e, outcome="canceled", cut=preview_message(e["text"]).endswith("..."))
            relays.append(r)
            open_by_key[e["key"]] = r
        elif kind == "updated" and e["key"] in open_by_key:
            open_by_key[e["key"]]["outcome"] = f"updated ({e['msgType']})"
        elif kind == "skipped" and e["key"] in open_by_key:
            reason = e["reason"]
            open_by_key[e["key"]]["outcome"] = "skipped " + (reason.split(":")[0] if reason.startswith("error") else reason)
    return relays


# ── the Open API, through lark-cli ────────────────────────────────────────────
def lark_call(method, path, payload, identity="user"):
    """The whole lark-cli response: `ok`, and `data` or `code` + `msg`."""
    flag = "--params" if method == "GET" else "--data"
    out = subprocess.run(["lark-cli", "api", method, path, flag, json.dumps(payload), "--as", identity],
                         capture_output=True, text=True)
    try:
        return json.loads(out.stdout)
    except ValueError:
        return {"ok": False, "code": -1, "msg": (out.stderr or out.stdout).strip()}


def lark_api(method, path, payload):
    """None when Lark refuses — e.g. 230002, a chat Max is not a member of. Those are expected."""
    d = lark_call(method, path, payload)
    return d["data"] if d.get("ok") else None


def parse_when(s):
    """ISO time → epoch seconds. A naive time is local, like `events` prints it."""
    t = datetime.fromisoformat(s.replace("Z", "+00:00"))
    return t.timestamp() if t.tzinfo else t.astimezone().timestamp()


def parse_span(s):
    """`90s`, `5m`, `2h` → seconds."""
    return int(s[:-1]) * {"s": 1, "m": 60, "h": 3600}[s[-1]]


def names_chat(name, title, stem):
    """Lark truncates a long title with `...`; then any chat whose name starts with the stem."""
    return name.startswith(stem) if title.endswith("...") else name == title


def chats_search(query, page_size=20):
    """`chats/search` hits (`chat_id`, `name`, …). Not deterministic — progress.md 2026-08-28."""
    data = lark_api("GET", "/open-apis/im/v1/chats/search", {"query": query, "page_size": page_size}) or {}
    return data.get("items", [])


def resolve_chat(name):
    """`oc_…` as is; else the phone's cache (exact, or the truncated-title stem), else `chats/search`.
    Returns `(chat_id, how)`; exits when nothing names the chat."""
    if name.startswith("oc_"):
        return name, ""
    stem = name[:-3] if name.endswith("...") else name
    for title, chat in read_chat_cache().items():
        if title == name or title.startswith(stem):
            return chat, f"{title}  (cache)"
    hits = [h for h in chats_search(stem) if names_chat(h["name"], name, stem)] or chats_search(stem)
    if not hits:
        sys.exit(f"no chat named {name!r} in the cache or in chats/search")
    return hits[0]["chat_id"], f"{hits[0]['name']}  (chats/search, {len(hits)} hit(s))"


def messages_of(chat, start, end):
    """`im/v1/messages` of a chat between two epoch seconds, every page. Empty for a chat Max cannot read."""
    items, token, pages = [], None, 0
    while True:
        params = {"container_id_type": "chat", "container_id": chat, "sort_type": "ByCreateTimeDesc",
                  "page_size": "50", "start_time": str(start), "end_time": str(end)}
        if token:
            params["page_token"] = token
        data = lark_api("GET", "/open-apis/im/v1/messages", params)
        if data is None:  # not a member of that chat, or the call failed
            return items
        items += data.get("items", [])
        pages += 1
        token = data.get("page_token")
        if not data.get("has_more") or not token or pages > 12:
            return items


def flat_text(m):
    """One line per message, the way the app would read it: `text` and `post` flattened, the rest named."""
    kind, body = m["msg_type"], m["body"]["content"]
    if m.get("deleted"):
        return "(recalled)"
    if kind == "text":
        return json.loads(body)["text"]
    if kind == "post":
        post = json.loads(body)
        paras = [" ".join(e.get("text") or e.get("user_name") or e.get("emoji_type") or f"[{e['tag']}]" for e in para)
                 for para in post["content"]]
        return " ⏎ ".join(([post["title"]] if post.get("title") else []) + paras)
    if kind == "interactive":
        try:
            title = json.loads(body).get("title") or json.loads(body).get("header", {}).get("title", {}).get("content")
        except (ValueError, AttributeError):
            title = None
        return f"[card] {title}" if title else f"[card] {body[:80]}"
    return f"[{kind}] {body[:80]}"


# ── the replay corpus (`tools/larklish-helper replay fetch`, read by ReplayTest.kt) ─
#
# Four tab-separated files, so the Kotlin side needs no JSON library and the rules it
# measures are the app's own. Leaf strings escape \\, newline and tab; sub-lists use the
# control separators \x01 (items), \x02 (paragraphs) and \x03 (fields of one element).
#
#   originals.tsv  atMs, title, text
#   titles.tsv     title, chatIds…                 (every chat chats/search names)
#   dms.tsv        sender, chatId                  (from the phone's chat cache)
#   messages.tsv   chatId, msgType, createTime, deleted, mentions, payload
CORPUS_FILES = ("originals.tsv", "titles.tsv", "dms.tsv", "messages.tsv")
SEP_ITEM, SEP_PARA, SEP_FIELD = "\x01", "\x02", "\x03"


def esc(s):
    s = (s or "").replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")
    assert not any(c in s for c in (SEP_ITEM, SEP_PARA, SEP_FIELD)), "text contains a separator"
    return s


def message_row(chat, m):
    kind, body = m["msg_type"], m["body"]["content"]
    mentions = SEP_ITEM.join(f"{esc(x['key'])}{SEP_FIELD}{esc(x['name'])}" for x in (m.get("mentions") or []))
    if m.get("deleted"):
        payload = ""
    elif kind == "text":
        payload = esc(json.loads(body)["text"])
    elif kind == "post":
        post = json.loads(body)
        paras = [SEP_ITEM.join(
            SEP_FIELD.join(esc(e.get(k, "")) for k in ("tag", "text", "user_name", "emoji_type"))
            for e in para) for para in post["content"]]
        payload = SEP_PARA.join([esc(post.get("title") or "")] + paras)
    else:
        payload = ""
    return "\t".join([chat, kind, m["create_time"], "1" if m.get("deleted") else "0", mentions, payload])
