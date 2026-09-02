"""The Open API under Max's user identity, through `lark-cli api`; the bot posts through it too."""

import json
import subprocess
import sys
from datetime import datetime
from typing import Any

from .phone import read_chat_cache, run

TEST_CHAT = "oc_e2feee77ba20b6c5285d22fb8aa90eb4"  # Larklish 测试群
TEST_TITLE = "Larklish 测试群"

Message = dict[str, Any]  # one item of im/v1/messages: `msg_type`, `create_time`, `body.content`, …


def cli(method: str, path: str, payload: dict[str, Any], identity: str = "user") -> dict[str, Any]:
    """One `lark-cli api` call, the whole response: `ok`, and `data` or `code` + `msg`."""
    flag = "--params" if method == "GET" else "--data"
    out = subprocess.run(["lark-cli", "api", method, path, flag, json.dumps(payload), "--as", identity],
                         capture_output=True, text=True)
    try:
        return json.loads(out.stdout)
    except ValueError:
        return {"ok": False, "code": -1, "msg": (out.stderr or out.stdout).strip()}


def api(method: str, path: str, payload: dict[str, Any]) -> dict[str, Any] | None:
    """The `data` of a call, or None when Lark refuses — e.g. 230002, a chat Max is not a member of."""
    d = cli(method, path, payload)
    return d["data"] if d.get("ok") else None


def send(text: str, reply_to: str | None = None) -> str:
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
    data = api("GET", "/open-apis/im/v1/chats/search", {"query": query, "page_size": page_size}) or {}
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
        data = api("GET", "/open-apis/im/v1/messages", params)
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
