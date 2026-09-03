#!/usr/bin/env python3
"""Larklish's tools, in one place. Every group keeps the shape of the tool it replaces.

    tools/larklish-helper events stats                  # the Relay record (list, stats, grade, pull)
    tools/larklish-helper events --since 2026-08-29T00:30 --han list
    tools/larklish-helper events --since 2026-09-01T03:50 grade   # the soak grade: each Relay and its outcome
    tools/larklish-helper msgs 'US Global E-Commerce' --around 2026-09-02T14:06   # what a chat said then
    tools/larklish-helper chats search 'ByteDance Research' --repeat 3            # chats/search hits
    tools/larklish-helper translate '先别发布' --repeat 30                          # one Lark translate call
    tools/larklish-helper probe "中文消息"               # post to the test group, show the Relay
    tools/larklish-helper probe --debug fetch           #   …or run a MainActivity debug hook
    tools/larklish-helper chats                         # the Backend's chat-id cache
    tools/larklish-helper phone shade shot.png          # adb helpers (top, shade, shot, home)
    tools/larklish-helper replay fetch                  # pull the corpus ReplayTest scores against
    tools/larklish-helper showcase 02-english "<message>" "<caption>"   # Lark vs Larklish, side by side
    tools/larklish-helper token --write                 # credentials from lark-cli's store

`--help` works at every level: `tools/larklish-helper events --help`, `… events list --help`.
This file is also the library for the one-off scripts it does not cover — the functions above
`cmd_*` return values and never print:

    import sys; sys.path.insert(0, "tools")
    from larklish_helper import read_events, relays_with_outcomes, when
    for r in relays_with_outcomes(read_events()):        # the phone's record
        if r["cut"] and r["outcome"] != "updated (text)":
            print(when(r), r["outcome"], r["text"])

`tools/larklish-helper` is a symlink to this file. Two subcommands need packages the system
Python lacks — `showcase` Pillow, `token` cryptography — and import them where they are used, so
everything else runs on plain `python3`. For those two, the repo's venv (once per machine:
`uv venv && uv pip install --system-certs -r tools/requirements.txt`) and then
`uv run --script tools/larklish-helper token --write`.
"""

import argparse
import base64
import importlib
import io
import json
import os
import pathlib
import re
import shlex
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
from collections import Counter
from datetime import datetime
from typing import Any

Event = dict[str, Any]      # one line of events.jsonl (Recorder.kt): `event`, `at`, `key`, …
Relay = dict[str, Any]      # an Event of kind `relayed`, plus `outcome` and `cut` (relays_with_outcomes)
Message = dict[str, Any]    # one item of im/v1/messages: `msg_type`, `create_time`, `body.content`, …
ChatCache = dict[str, str]  # the Backend's chat cache (GET /chats): title or `dm:<Sender>` → chat id


def need(module: str) -> Any:
    """A venv-only package (Pillow, cryptography), imported where it is used so that the rest of
    the helper runs on plain python3."""
    try:
        return importlib.import_module(module)
    except ImportError:
        sys.exit(f"{module} is not installed for this python — run the helper in the repo's venv:\n"
                 f"  uv run --script tools/larklish-helper {' '.join(sys.argv[1:])}\n"
                 "(once per machine: uv venv && uv pip install --system-certs -r tools/requirements.txt)")


# ══ library ═══════════════════════════════════════════════════════════════════
# ── the phone, over adb ───────────────────────────────────────────────────────
PACKAGE = "com.vegerot.larklish"
LOG_TAGS = ["LarkListener:I", "Translator:I", "FallbackTranslator:W", "*:S"]


def run(args: list[str], **kw: Any) -> str:
    """Run a command and fail loudly with whatever it said."""
    out = subprocess.run(args, capture_output=True, text=True, **kw)
    if out.returncode != 0:
        sys.exit(f"{args[0]}: {(out.stderr or out.stdout).strip()}")
    return out.stdout


def adb(*args: str, allow_fail: bool = False) -> subprocess.CompletedProcess[str]:
    out = subprocess.run(["adb", *args], capture_output=True, text=True)
    if out.returncode != 0 and not allow_fail:
        sys.exit(f"adb {' '.join(args)}: {(out.stderr or out.stdout).strip()}")
    return out


def require_device() -> None:
    """`adb logcat` waits forever when no phone is attached, so check before anything blocks."""
    out = subprocess.run(["adb", "devices"], capture_output=True, text=True)
    if not [ln for ln in out.stdout.splitlines()[1:] if ln.strip().endswith("\tdevice")]:
        sys.exit("no phone on adb — check the cable, then `adb devices`")


def read_events(path: str | None = None) -> list[Event]:
    """`filesDir/events.jsonl` from the phone, or a saved copy."""
    if path:
        text = pathlib.Path(path).read_text(encoding="utf-8")
    else:
        require_device()
        text = adb("exec-out", "run-as", PACKAGE, "cat", "files/events.jsonl").stdout
    # One JSON object per "\n". Not splitlines(): a message may carry U+2028 LINE SEPARATOR,
    # which JSON leaves unescaped and splitlines() would split on (a Relay of 2026-09-02 did).
    return [json.loads(line) for line in text.split("\n") if line.strip()]


def backend_url() -> str:
    """The Backend (Layer 7) runs on this machine; `LARKLISH_BACKEND` overrides."""
    return os.environ.get("LARKLISH_BACKEND", "http://127.0.0.1:8787")


def read_chat_cache(path: str | None = None) -> ChatCache:
    """The Backend's chat cache (`GET /chats`), or a saved copy. Empty when nothing has been resolved yet."""
    if path:
        return json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
    with urllib.request.urlopen(backend_url() + "/chats", timeout=5) as resp:
        return json.load(resp)


def debug_hook(what: str, title: str, text: str) -> None:
    """A MainActivity debug hook: user, refresh or fetch."""
    # `adb shell` hands one string to the phone's shell, which splits it again: a title with a
    # space silently truncates unless every value is quoted for *that* shell. CLEAR_TOP|NEW_TASK
    # because MainActivity reads the hook in onCreate — a running instance must be recreated.
    adb("shell", " ".join([
        "am", "start", "-f", "0x10008000", "-n", f"{PACKAGE}/.MainActivity",
        "--es", "debug", shlex.quote(what),
        "--es", "title", shlex.quote(title),
        "--es", "text", shlex.quote(text),
    ]))


def log_lines() -> list[str]:
    """The app's logcat so far: time + level + tag + message."""
    out = []
    for line in adb("logcat", "-d", "-s", *LOG_TAGS).stdout.splitlines():
        parts = line.split(None, 5)  # date time pid tid level tag: message
        out.append(" ".join(parts[1:2] + parts[4:]) if len(parts) > 5 else line)
    return out


def top() -> str:
    """The foreground activity."""
    dump = adb("shell", "dumpsys", "activity", "activities").stdout
    m = re.search(r"topResumedActivity=ActivityRecord\{\S+ u0 (\S+)", dump)
    return m.group(1) if m else "?"


def shot(path: str) -> None:
    with open(path, "wb") as f:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], check=True, stdout=f)


# ── the record: events.jsonl, one line per Relay, Update, skip, fallback and removal ─

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


# ── the Open API under Max's user identity, through `lark-cli api` ────────────
TEST_CHAT = "oc_e2feee77ba20b6c5285d22fb8aa90eb4"  # Larklish 测试群
TEST_TITLE = "Larklish 测试群"
TEST_TITLE_EN = "Larklish test group"  # the Relay's title: Lark's translation of TEST_TITLE


def lark_cli(method: str, path: str, payload: dict[str, Any], identity: str = "user") -> dict[str, Any]:
    """One `lark-cli api` call, the whole response: `ok`, and `data` or `code` + `msg`."""
    flag = "--params" if method == "GET" else "--data"
    out = subprocess.run(["lark-cli", "api", method, path, flag, json.dumps(payload), "--as", identity],
                         capture_output=True, text=True)
    try:
        return json.loads(out.stdout)
    except ValueError:
        return {"ok": False, "code": -1, "msg": (out.stderr or out.stdout).strip()}


def lark_api(method: str, path: str, payload: dict[str, Any]) -> dict[str, Any] | None:
    """The `data` of a call, or None when Lark refuses — e.g. 230002, a chat Max is not a member of."""
    d = lark_cli(method, path, payload)
    return d["data"] if d.get("ok") else None


def send_test_message(text: str, reply_to: str | None = None) -> str:
    """Post to the test group as the bot. Returns the new message id."""
    args = ["lark-cli", "im", "+messages-reply", "--message-id", reply_to, "--reply-in-thread"] if reply_to \
        else ["lark-cli", "im", "+messages-send", "--chat-id", TEST_CHAT]
    return json.loads(run(args + ["--as", "bot", "--text", text]))["data"]["message_id"]


def parse_when(s: str) -> float:
    """ISO time → epoch seconds. A naive time is local, like `events` prints it."""
    t = datetime.fromisoformat(s.replace("Z", "+00:00"))
    return t.timestamp() if t.tzinfo else t.astimezone().timestamp()


def parse_span(s: str) -> int:
    """`90s`, `5m`, `2h` → seconds."""
    return int(s[:-1]) * {"s": 1, "m": 60, "h": 3600}[s[-1]]


def names_chat(name: str, title: str, stem: str) -> bool:
    """Lark truncates a long title with `...`; then any chat whose name starts with the stem."""
    return name.startswith(stem) if title.endswith("...") else name == title


def chats_search(query: str, page_size: int = 20) -> list[dict[str, Any]]:
    """`chats/search` hits (`chat_id`, `name`, …). Not deterministic — progress.md 2026-08-28."""
    data = lark_api("GET", "/open-apis/im/v1/chats/search", {"query": query, "page_size": page_size}) or {}
    return data.get("items", [])


def resolve_chat(name: str) -> tuple[str, str]:
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


def messages_of(chat: str, start: int, end: int) -> list[Message]:
    """`im/v1/messages` of a chat between two epoch seconds, every page. Empty for a chat Max cannot read."""
    items: list[Message] = []
    token, pages = None, 0
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


def flat_text(m: Message) -> str:
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


# ── the replay corpus (`replay fetch` writes it, ReplayTest.kt reads it) ──────
#
# Four tab-separated files, so the Kotlin side needs no JSON library and the rules it
# measures are the app's own. Leaf strings escape \, newline and tab; sub-lists use the
# control separators \x01 (items), \x02 (paragraphs) and \x03 (fields of one element).
#
#     originals.tsv  atMs, title, text
#     titles.tsv     title, chatIds…                 (every chat chats/search names)
#     dms.tsv        sender, chatId                  (from the phone's chat cache)
#     messages.tsv   chatId, msgType, createTime, deleted, mentions, payload
CORPUS_FILES = ("originals.tsv", "titles.tsv", "dms.tsv", "messages.tsv")
SEP_ITEM, SEP_PARA, SEP_FIELD = "\x01", "\x02", "\x03"


def esc(s: str | None) -> str:
    s = (s or "").replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")
    assert not any(c in s for c in (SEP_ITEM, SEP_PARA, SEP_FIELD)), "text contains a separator"
    return s


def message_row(chat: str, m: Message) -> str:
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


# ── credentials: lark-cli's encrypted store (AES-256-GCM under one master key) ─
# macOS: the key is keychain item service `lark-cli`, account `master.key`, value
# `go-keyring-base64:` + base64(base64(key)); blobs in ~/Library/Application Support/lark-cli/.
# Linux: a raw 32-byte `master.key` next to the blobs in ~/.local/share/lark-cli/ (or
# $LARKSUITE_CLI_DATA_DIR/lark-cli). The user token blob is `<appId>_<openId>.enc` (JSON
# `StoredUAToken`, lark-cli `internal/auth/token_store.go`); the app secret is
# `appsecret_<appId>.enc`. Refresh tokens are single-use: once the app refreshes from the copied
# token, this machine's lark-cli must `auth login` again (Experiment 08).
REPO = pathlib.Path(__file__).resolve().parent.parent
LOCAL_PROPERTIES = REPO / "local.properties"
DARWIN = sys.platform == "darwin"


def store_dir() -> pathlib.Path:
    if DARWIN:
        return pathlib.Path.home() / "Library/Application Support/lark-cli"
    return pathlib.Path(os.environ.get("LARKSUITE_CLI_DATA_DIR", pathlib.Path.home() / ".local/share")) / "lark-cli"


def master_key(store: pathlib.Path) -> bytes:
    if not DARWIN:
        return (store / "master.key").read_bytes()
    out = subprocess.run(
        ["security", "find-generic-password", "-s", "lark-cli", "-a", "master.key", "-w"],
        check=True, capture_output=True, text=True,
    ).stdout.strip()
    prefix = "go-keyring-base64:"
    assert out.startswith(prefix), "unexpected keychain value format"  # go-keyring v0.2.8 always prefixes
    return base64.b64decode(base64.b64decode(out[len(prefix):]))


def decrypt(blob: bytes, key: bytes) -> str:
    aead = need("cryptography.hazmat.primitives.ciphers.aead")
    return aead.AESGCM(key).decrypt(blob[:12], blob[12:], None).decode()


def mask(secret: str) -> str:
    return "****" + secret[-4:]


def read_store() -> tuple[str, str, dict[str, Any] | None]:
    """(appId, appSecret, the user token or None) from lark-cli's store."""
    store = store_dir()
    key = master_key(store)
    users = sorted(store.glob("cli_*_ou_*.enc"))
    assert len(users) <= 1, f"more than one user token in {store}: {[u.name for u in users]}"  # one login per machine
    token = json.loads(decrypt(users[0].read_bytes(), key)) if users else None
    if token:
        app_id = token["appId"]
    else:
        # No user login yet: the app id comes from the only real app-secret blob.
        secrets = [p for p in store.glob("appsecret_cli_*.enc") if len(p.stem) == len("appsecret_cli_") + 16]
        assert len(secrets) == 1, f"cannot pick the app id in {store}: {[p.name for p in secrets]}"
        app_id = secrets[0].stem.removeprefix("appsecret_")
    app_secret = decrypt((store / f"appsecret_{app_id}.enc").read_bytes(), key)
    return app_id, app_secret, token


def write_local(values: dict[str, str]) -> None:
    """Replace the `lark.*` lines of local.properties; keep every other line."""
    lines = LOCAL_PROPERTIES.read_text().splitlines() if LOCAL_PROPERTIES.exists() else []
    kept = [line for line in lines if not line.startswith("lark.")]
    LOCAL_PROPERTIES.write_text("\n".join(kept + [f"{k}={v}" for k, v in values.items()]) + "\n")


# ── the shade, by uiautomator bounds (showcase) ────────────────────────────────
# Lark posts an Original two ways (progress.md 2026-09-03): composed by its own process when it
# was recently in the foreground (whole text, `[N message(s)]` in the header), or from the push
# payload when the process sat idle for hours — title and text cut at 45 characters. The cut is
# what `showcase` exists to show; `adb shell dumpsys deviceidle force-idle` reproduces it on
# demand (`unforce` after). Killing Lark's process does not: the restarted process withdraws
# the pushed Original half a second later.
SHOWCASE_DIR = "docs/showcase"
ROW = "com.android.systemui:id/expandableNotificationRow"
SHADE_BOTTOM = 2261  # the shade's scroller ends here on the Pixel 4a (below it: Clear all)
Node = ET.Element
Bounds = tuple[int, int, int, int]


def ui_dump() -> tuple[Node, dict[Node, Node]]:
    """The screen's view tree (only what is on screen), and each node's parent."""
    raw = subprocess.run(["adb", "exec-out", "uiautomator", "dump", "/dev/tty"],
                         capture_output=True, text=True).stdout
    raw = raw[raw.find("<?xml"):]
    raw = raw[: raw.rfind(">") + 1]
    root = ET.fromstring(raw)
    return root, {c: p for p in root.iter() for c in p}


def bounds(node: Node) -> Bounds:
    left, top, right, bottom = (int(x) for x in node.get("bounds", "").replace("][", ",").strip("[]").split(","))
    return left, top, right, bottom


def tap(node: Node) -> None:
    left, top, right, bottom = bounds(node)
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(1.5)


def scroll_shade(px: int) -> None:
    adb("shell", "input", "swipe", "540", str(1200 + px // 2), "540", str(1200 - px // 2), "300")
    time.sleep(1.2)


def rows_of(node: Node, parent: dict[Node, Node]) -> list[Node]:
    """The notification rows around a node, innermost first: the chat's row, then its app group."""
    out = []
    while node is not None:
        if node.get("resource-id") == ROW:
            out.append(node)
        node = parent.get(node)
    return out


def find_node(root: Node, **attrs: str) -> Node | None:
    for n in root.iter("node"):
        if all(n.get(k) == v for k, v in attrs.items()):
            return n
    return None


def button(container: Node, desc: str) -> Node | None:
    for n in container.iter("node"):
        if n.get("class") == "android.widget.Button" and n.get("content-desc") == desc:
            return n
    return None


def header_button(group: Node) -> Node | None:
    """The chevron in the header of a group or a standalone notification."""
    for n in group.iter("node"):
        if n.get("class") == "android.widget.Button" and bounds(n)[1] < bounds(group)[1] + 200:
            return n
    return None


def shade_open() -> tuple[Node, dict[Node, Node]]:
    root, parent = ui_dump()
    if find_node(root, **{"resource-id": "com.android.systemui:id/notification_stack_scroller"}) is None:
        adb("shell", "cmd", "statusbar", "expand-notifications")
        time.sleep(2.5)
        root, parent = ui_dump()
    return root, parent


def locate(app: str, title: str) -> tuple[Node, Node]:
    """The row that shows `title`, and the group row around it (the same row when standalone).
    A collapsed group lists only its first lines, so open the app's group first; uiautomator
    lists only what is on screen, so scroll when it is still missing."""
    for _ in range(5):
        root, parent = shade_open()
        node = find_node(root, text=title)
        if node is not None:
            rows = rows_of(node, parent)
            return rows[0], rows[-1]
        header = find_node(root, **{"resource-id": "android:id/app_name_text", "text": app})
        chevron = button(rows_of(header, parent)[-1], "Expand") if header is not None else None
        if chevron is not None and bounds(chevron)[1] < bounds(header)[3] + 100:
            tap(chevron)  # the collapsed group's chevron
        else:
            scroll_shade(900)
    raise AssertionError(f"no {title!r} under {app} in the shade")


def capture(app: str, title: str, out_png: str) -> Bounds:
    """Expand the chat's row under `app`, crop it with its group header into out_png."""
    child, group = locate(app, title)
    chevron = header_button(group)
    if chevron is not None and chevron.get("content-desc") == "Expand":
        tap(chevron)  # a collapsed group's chevron, or a standalone notification's
        child, group = locate(app, title)
    if child is not group and (chevron := button(child, "Expand")) is not None:
        tap(chevron)  # the chat row's own chevron: the full text
        child, group = locate(app, title)
    left, top, right, _ = bounds(group)
    bottom = bounds(child)[3]
    if bottom > SHADE_BOTTOM - 60:
        scroll_shade(bottom - SHADE_BOTTOM + 200)
        child, group = locate(app, title)
        left, top, right, _ = bounds(group)
        bottom = bounds(child)[3]
    png = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout
    need("PIL.Image").open(io.BytesIO(png)).crop((left, top, right, bottom)).save(out_png)
    if (chevron := button(group, "Collapse")) is not None:
        tap(chevron)  # so the next group moves up
    return left, top, right, bottom


PAGE_BG, INK, LABEL = (246, 247, 249), (32, 33, 36), (95, 99, 104)
GAP, PAD, RADIUS = 48, 56, 56


def rounded(card: Any) -> Any:
    Image, ImageDraw = need("PIL.Image"), need("PIL.ImageDraw")
    mask_ = Image.new("L", card.size, 0)
    ImageDraw.Draw(mask_).rounded_rectangle((0, 0, card.width - 1, card.height - 1), RADIUS, fill=255)
    out = Image.new("RGBA", card.size, PAGE_BG + (255,))
    out.paste(card, (0, 0), mask_)
    return out


def compose(slug: str, caption: str) -> str:
    """<slug>-lark.png and <slug>-larklish.png side by side under a caption → <slug>.png."""
    Image, ImageDraw, ImageFont = need("PIL.Image"), need("PIL.ImageDraw"), need("PIL.ImageFont")
    lark = rounded(Image.open(f"{SHOWCASE_DIR}/{slug}-lark.png").convert("RGBA"))
    larklish = rounded(Image.open(f"{SHOWCASE_DIR}/{slug}-larklish.png").convert("RGBA"))
    font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 44)
    small = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 36)
    top = PAD + 70 + 40  # caption line, label line
    page = Image.new("RGB", (PAD + lark.width + GAP + larklish.width + PAD, top + max(lark.height, larklish.height) + PAD), PAGE_BG)
    d = ImageDraw.Draw(page)
    d.text((PAD, PAD), caption, fill=INK, font=font)
    d.text((PAD, PAD + 70), "What Lark showed", fill=LABEL, font=small)
    d.text((PAD + lark.width + GAP, PAD + 70), "What Larklish showed", fill=LABEL, font=small)
    page.paste(lark, (PAD, top), lark)
    page.paste(larklish, (PAD + lark.width + GAP, top), larklish)
    out = f"{SHOWCASE_DIR}/{slug}.png"
    page.save(out)
    return out


# ══ driver: the subcommands. Only these print. ════════════════════════════════
# ── events: the Relay record (`filesDir/events.jsonl`, written by Recorder.kt) ─
def selected(events: list[Event], args: argparse.Namespace) -> list[Event]:
    return select(events, since=args.since, han=args.han, no_removed=args.no_removed)


def events_list(events: list[Event], args: argparse.Namespace) -> None:
    for e in selected(events, args):
        kind = e["event"]
        if kind == "relayed":
            mark = "~" if is_fallback(e) else " "
            print(f"{when(e, args.utc)} R{mark} [{e['title']}] {e['text']}")
            print(f"{'':11}  → [{e['relayTitle']}] {e['relayText']}")
        elif kind == "updated":
            mark = "~" if e["relayText"].startswith("~") else " "
            print(f"{when(e, args.utc)} U{mark} ({e['msgType']}) {e['fullText']}")
            print(f"{'':11}  → {e['relayText']}")
        elif kind == "skipped":
            print(f"{when(e, args.utc)} S  {e['reason']}")
        elif kind == "fallback":
            print(f"{when(e, args.utc)} F  {e['reason']}")
        else:
            r = e["reason"]
            print(f"{when(e, args.utc)} X  reason={r} ({REASONS.get(r, '?')}) chat {event_chat(e)}")


def events_stats(events: list[Event], args: argparse.Namespace) -> None:
    rows = selected(events, args)
    relayed = [e for e in rows if e["event"] == "relayed"]
    removed = [e for e in rows if e["event"] == "removed"]
    han_text = [e for e in relayed if has_han(e["text"])]
    han_title = [e for e in relayed if has_han(e["title"])]
    han_any = {id(e) for e in han_text} | {id(e) for e in han_title}
    updated = [e for e in rows if e["event"] == "updated"]
    skipped = [e for e in rows if e["event"] == "skipped"]
    fell_back = [e for e in rows if e["event"] == "fallback"]
    print(f"relayed {len(relayed)}  updated {len(updated)}  skipped {len(skipped)}  removed {len(removed)}")
    print(f"fallback (~) {sum(map(is_fallback, relayed))}")
    if fell_back:  # Layer 6: why the Translator gave up — an outage, or an input Lark will not translate
        why = Counter(e["reason"].split(":")[0] for e in fell_back)
        print("fallback reasons:", ", ".join(f"{r} ×{n}" for r, n in why.most_common()))
    if updated or skipped:
        types = Counter(e["msgType"] for e in updated)
        reasons = Counter(e["reason"].split(":")[0] if e["reason"].startswith("error") else e["reason"] for e in skipped)
        print("updated types:", ", ".join(f"{t} ×{n}" for t, n in types.most_common()) or "-")
        print("skipped reasons:", ", ".join(f"{r} ×{n}" for r, n in reasons.most_common()) or "-")
    print(f"han in text {len(han_text)}  han in title {len(han_title)}  english pass-through {len(relayed) - len(han_any)}")
    print("removal reasons:", ", ".join(f"{r} ({REASONS.get(r, '?')}) ×{n}" for r, n in sorted(Counter(e['reason'] for e in removed).items())))
    if relayed:
        print(f"span {when(relayed[0], args.utc)} → {when(relayed[-1], args.utc)}")


def events_grade(events: list[Event], args: argparse.Namespace) -> None:
    """The soak grade (Experiments 12–14): `not-truncated` is by design, so the denominator is the
    Relays whose Preview Lark cut — and the list of those that did not Update is the work list."""
    relays = relays_with_outcomes(selected(events, args))
    cut = [r for r in relays if r["cut"]]
    updated = [r for r in cut if r["outcome"].startswith("updated")]
    print(f"Relays {len(relays)}   Preview cut {len(cut)}   complete {len(relays) - len(cut)} (skipped by design)")
    print(f"\ncut Previews → Updated {len(updated)} of {len(cut)}")
    for outcome, n in Counter(r["outcome"] for r in cut).most_common():
        print(f"  {n:4}  {outcome}")
    misses = [r for r in cut if not r["outcome"].startswith("updated")]
    if misses:
        print("\ncut, not Updated:")
        for r in misses:
            outcome = r["outcome"].removeprefix("skipped ")
            print(f"  {when(r, args.utc)} {outcome:18} [{r['title'][:30]}] {one_line(r['text'])[:80]}")


def events_pull(events: list[Event], args: argparse.Namespace) -> None:
    with open(args.out, "w", encoding="utf-8") as f:
        for e in events:
            f.write(json.dumps(e, ensure_ascii=False) + "\n")
    print(f"{len(events)} events → {args.out}")


def cmd_events(args: argparse.Namespace) -> None:
    events = read_events(args.file)
    {"list": events_list, "stats": events_stats, "grade": events_grade, "pull": events_pull}[args.view](events, args)


# ── probe: post one message and show what Larklish did with it ────────────────
def cmd_probe(args: argparse.Namespace) -> None:
    if not args.text and not args.debug:
        sys.exit("give a message to send, or --debug HOOK")
    require_device()
    # On USB the phone reaches the Backend on its own port 8787 (Backend.kt tries it first).
    adb("reverse", "tcp:8787", "tcp:8787", allow_fail=True)
    if args.install:
        print("installing…")
        run(["./gradlew", "--quiet", "installDebug"])
    adb("logcat", "-c")
    if args.debug:
        debug_hook(args.debug, args.title, args.text or "")
    else:
        root = send_test_message(args.text)
        print(f"sent {root}")
        if args.thread:
            time.sleep(min(args.wait, 8))
            print(f"replied {send_test_message('回复：' + args.text, reply_to=root)}")
    time.sleep(args.wait)
    print(*log_lines(), sep="\n")


# ── chats: the Backend's chat-id cache (GET /chats; in memory, so a restart clears it) ──
def cmd_chats(args: argparse.Namespace) -> None:
    chats = read_chat_cache()
    if not chats:
        print("no cache yet — nothing has been resolved since the Backend started")
        return
    groups = {k: v for k, v in chats.items() if not k.startswith("dm:")}
    dms = {k[3:]: v for k, v in chats.items() if k.startswith("dm:")}
    for label, rows in (("groups", groups), ("DMs", dms)):
        print(f"\n{label} ({len(rows)})")
        for name, chat in sorted(rows.items()):
            print(f"  {chat}  {name}")


def cmd_chats_search(args: argparse.Namespace) -> None:
    """`chats/search` is not deterministic (progress.md 2026-08-28): --repeat shows which hits come and go."""
    seen, names = Counter(), {}
    for _ in range(args.repeat):
        for h in chats_search(args.query):
            seen[h["chat_id"]] += 1
            names[h["chat_id"]] = h
    if not seen:
        print("no hits")
    for chat, n in sorted(seen.items(), key=lambda kv: -kv[1]):
        h = names[chat]
        runs = f"{n}/{args.repeat}  " if args.repeat > 1 else ""
        print(f"{runs}{chat}  {h['name']}")


# ── msgs: what a chat said around a time (the start of every `no-match` investigation) ─
def cmd_msgs(args: argparse.Namespace) -> None:
    chat, how = resolve_chat(args.chat)
    if how:
        print(f"# {chat}  {how}")
    around = parse_when(args.around) if args.around else time.time()
    start, end = int(around - parse_span(args.before)), int(around + parse_span(args.after))
    items = messages_of(chat, start, end)
    if not items:
        print("no messages in the window (or a chat Max cannot read)")
    for m in sorted(items, key=lambda m: int(m["create_time"])):
        t = datetime.fromtimestamp(int(m["create_time"]) / 1000)
        if not args.utc:
            t = t.astimezone()
        mentions = " ".join(f"{x['key']}={x['name']}" for x in (m.get("mentions") or []))
        line = m["body"]["content"] if args.raw else one_line(flat_text(m))
        print(f"{t.strftime('%m-%d %H:%M:%S')}  {m['msg_type']:12} {line}" + (f"   {{{mentions}}}" if mentions else ""))


# ── translate: one call to the engine the app uses, to answer rate-limit and pass-through questions ─
def cmd_translate(args: argparse.Namespace) -> None:
    """--as bot is the tenant token the app uses; the rate limit (99991400) is per tenant either way."""
    codes = Counter()
    for i in range(args.repeat):
        if i and args.pause:
            time.sleep(args.pause)
        d = lark_cli("POST", "/open-apis/translation/v1/text/translate",
                      {"source_language": "zh", "target_language": "en", "text": args.text}, identity=args.identity)
        if d.get("ok"):
            out = d["data"]["text"]
            code = "ok" if out != args.text else "unchanged"
            codes[code] += 1
            print(f"{time.strftime('%H:%M:%S')}  {code:9} {out}")
        else:
            codes[str(d.get('code'))] += 1
            print(f"{time.strftime('%H:%M:%S')}  {d.get('code')}  {d.get('msg')}")
    if args.repeat > 1:
        print("\n" + "  ".join(f"{c} ×{n}" for c, n in codes.most_common()))


# ── phone: small adb helpers ───────────────────────────────────────────────────────────────────────────────────────────────────
def cmd_phone(args: argparse.Namespace) -> None:
    require_device()
    if args.act == "top":
        print(top())
    elif args.act == "home":
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
    elif args.act == "shot":
        shot(args.file)
        print(f"screenshot: {args.file}")
    elif args.act == "shade":
        adb("shell", "cmd", "statusbar", "expand-notifications")
        time.sleep(2)
        if args.file:
            shot(args.file)
            print(f"screenshot: {args.file}")


# ── replay: pull the corpus that ReplayTest scores the shipped rules against ───
def cmd_replay(args: argparse.Namespace) -> None:
    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    events = read_events(args.file)
    relayed = [e for e in events if e["event"] == "relayed"]
    if not relayed:
        sys.exit("no Relays in the record — nothing to replay")

    (out / "originals.tsv").write_text(
        "".join(f"{event_ms(e)}\t{esc(e['title'])}\t{esc(e['text'])}\n" for e in relayed), encoding="utf-8")

    # the phone's own DM cache: `messages/search` is not replayable offline
    cached = read_chat_cache(args.chats)
    dms = {k[3:]: v for k, v in cached.items() if k.startswith("dm:")}
    (out / "dms.tsv").write_text("".join(f"{esc(k)}\t{v}\n" for k, v in dms.items()), encoding="utf-8")

    titles, chats = {}, set(dms.values())
    for e in relayed:
        title = e["title"]
        if title == "Lark" or title == preview_sender(e["text"]) or title in titles:
            continue
        stem = title[:-3] if title.endswith("...") else title
        ids = [h["chat_id"] for h in chats_search(stem) if names_chat(h["name"], title, stem)][:5]
        titles[title] = ids
        chats.update(ids)
        print(f"  {len(ids)} chat(s) for [{title}]")
    (out / "titles.tsv").write_text(
        "".join(f"{esc(t)}\t{SEP_ITEM.join(ids)}\n" for t, ids in titles.items()), encoding="utf-8")

    start, end = min(map(event_ms, relayed)) // 1000 - 120, max(map(event_ms, relayed)) // 1000 + 120
    rows = []
    for i, chat in enumerate(sorted(chats), 1):
        got = messages_of(chat, start, end)
        print(f"  [{i}/{len(chats)}] {chat} {len(got)} messages")
        rows += [message_row(chat, m) for m in got]
    (out / "messages.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8")
    print(f"\ncorpus → {out}/  ({len(relayed)} Originals, {len(chats)} chats, {len(rows)} messages)")
    print("score it with: ./gradlew testDebugUnitTest --tests '*ReplayTest*'")


# ── showcase: one message → Lark's Original and Larklish's Relay side by side ─
def cmd_showcase(args: argparse.Namespace) -> None:
    require_device()
    if not args.no_send:
        adb("shell", "cmd", "statusbar", "collapse")
        print(f"sent {send_test_message(args.text)}")
        time.sleep(14)  # Relay in ~1 s, the Update through the Backend a few seconds later
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(1)
    adb("shell", "cmd", "statusbar", "expand-notifications")
    time.sleep(2.5)
    for app, title, half in (("Lark", TEST_TITLE, "lark"), ("Larklish", TEST_TITLE_EN, "larklish")):
        out = f"{SHOWCASE_DIR}/{args.slug}-{half}.png"
        left, top, right, bottom = capture(app, title, out)
        print(f"{app}: {out} {right - left}x{bottom - top}")
    adb("shell", "cmd", "statusbar", "collapse")
    print(compose(args.slug, args.caption))


# ── token: credentials from lark-cli's store, masked; --write puts them in local.properties ─
def cmd_token(args: argparse.Namespace) -> None:
    app_id, app_secret, token = read_store()
    print(f"store {store_dir()}")
    print(f"app {app_id}  secret {mask(app_secret)}")
    values = {"lark.appId": app_id, "lark.appSecret": app_secret}
    if token:
        expired = token["refreshExpiresAt"] < time.time() * 1000
        valid_to = datetime.fromtimestamp(token["refreshExpiresAt"] / 1000).strftime("%Y-%m-%d %H:%M")
        print(f"user {token['userOpenId']}  refresh token {mask(token['refreshToken'])} "
              f"({len(token['refreshToken'])} chars) valid to {valid_to}"
              + ("  EXPIRED — run `lark-cli auth login`" if expired else ""))
        if not expired:
            values["lark.userRefreshToken"] = token["refreshToken"]
    else:
        print("no user token — run `lark-cli auth login` first (only the app secret is available)")
    if args.write:
        write_local(values)
        print(f"wrote {LOCAL_PROPERTIES.relative_to(REPO)}: {', '.join(values)}")


def main() -> None:
    p = argparse.ArgumentParser(prog="tools/larklish-helper", description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    group = p.add_subparsers(dest="group", required=True)

    e = group.add_parser("events", help="the Relay record", description="Views of `filesDir/events.jsonl`.")
    e.add_argument("--file", help="read this saved copy instead of the phone")
    e.add_argument("--since", help="ISO UTC prefix, e.g. 2026-08-29T00:30 (Layer 6 went live)")
    e.add_argument("--han", action="store_true", help="only Relays whose Original has Han text")
    e.add_argument("--no-removed", action="store_true", help="hide removal events")
    e.add_argument("--utc", action="store_true", help="print UTC instead of local time")
    view = e.add_subparsers(dest="view", required=True)
    view.add_parser("list", help="every event, oldest first")
    view.add_parser("stats", help="counts")
    view.add_parser("grade", help="the soak grade: every Relay paired with its Update outcome, cut Previews first")
    view.add_parser("pull", help="save the raw JSONL").add_argument("out")
    e.set_defaults(fn=cmd_events)

    b = group.add_parser("probe", help="post one message and show the Relay")
    b.add_argument("text", nargs="?", help="the message to post (Chinese exercises the Translator)")
    b.add_argument("--install", action="store_true", help="./gradlew installDebug first")
    b.add_argument("--thread", action="store_true", help="post a root message, then reply inside its thread")
    b.add_argument("--debug", metavar="HOOK", help="run a MainActivity hook: user, refresh or fetch")
    b.add_argument("--title", default=TEST_TITLE, help="title for --debug fetch")
    b.add_argument("--wait", type=int, default=12, help="seconds to wait for the Update (default 12)")
    b.set_defaults(fn=cmd_probe)

    c = group.add_parser("chats", help="the Backend's chat-id cache; `chats search` asks the API")
    c.set_defaults(fn=cmd_chats)
    csub = c.add_subparsers(dest="act")
    cs = csub.add_parser("search", help="chats/search hits for a query (chat_id, name)")
    cs.add_argument("query")
    cs.add_argument("--repeat", type=int, default=1, help="ask N times; shows in how many runs each hit appeared")
    cs.set_defaults(fn=cmd_chats_search)

    m = group.add_parser("msgs", help="what a chat said around a time (im/v1/messages, flattened)")
    m.add_argument("chat", help="oc_… id, or a title — resolved through the phone's cache, then chats/search")
    m.add_argument("--around", help="ISO time, local unless it ends in Z (default: now)")
    m.add_argument("--before", default="10m", help="reach back this far: 90s, 5m, 2h (default 10m)")
    m.add_argument("--after", default="1m", help="and forward this far (default 1m)")
    m.add_argument("--raw", action="store_true", help="print body.content as the API returns it")
    m.add_argument("--utc", action="store_true", help="print UTC instead of local time")
    m.set_defaults(fn=cmd_msgs)

    x = group.add_parser("translate", help="one call to Lark's translate API, with its code")
    x.add_argument("text")
    x.add_argument("--repeat", type=int, default=1, help="call N times and tally the codes (the rate-limit probe)")
    x.add_argument("--pause", type=float, default=0, help="seconds between calls (default 0)")
    x.add_argument("--as", dest="identity", default="bot", choices=["bot", "user"], help="identity (default bot, as the app)")
    x.set_defaults(fn=cmd_translate)

    f = group.add_parser("phone", help="adb helpers")
    act = f.add_subparsers(dest="act", required=True)
    act.add_parser("top", help="foreground activity")
    act.add_parser("home", help="press HOME")
    act.add_parser("shot", help="screenshot to FILE").add_argument("file")
    act.add_parser("shade", help="expand the shade, optional screenshot").add_argument("file", nargs="?")
    f.set_defaults(fn=cmd_phone)

    r = group.add_parser("replay", help="the corpus ReplayTest scores the shipped rules against")
    rsub = r.add_subparsers(dest="act", required=True)
    fetch = rsub.add_parser("fetch", help="pull Originals, chats and messages into a corpus directory")
    fetch.add_argument("--out", default="replay-corpus", help="corpus directory (default replay-corpus/)")
    fetch.add_argument("--file", help="read this saved events.jsonl instead of the phone")
    fetch.add_argument("--chats", help="read this saved chats.json instead of the Backend (DM senders)")
    r.set_defaults(fn=cmd_replay)

    s = group.add_parser("showcase", help="post one message; crop Lark's Original and Larklish's Relay side by side into docs/showcase/")
    s.add_argument("slug", help="file name stem, e.g. 02-english")
    s.add_argument("text", help='the message to post; <at user_id="ou_…">Name</at> mentions someone')
    s.add_argument("caption", help="one line above the pair")
    s.add_argument("--no-send", action="store_true", help="the pair is in the shade already")
    s.set_defaults(fn=cmd_showcase)

    t = group.add_parser("token", help="credentials from lark-cli's store (masked); --write puts them in local.properties")
    t.add_argument("--write", action="store_true", help="update local.properties (default: only show)")
    t.set_defaults(fn=cmd_token)

    args = p.parse_args()
    args.fn(args)


if __name__ == "__main__":
    main()
