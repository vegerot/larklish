"""The replay corpus `tools/larklish-helper replay fetch` writes and ReplayTest.kt reads.

Four tab-separated files, so the Kotlin side needs no JSON library and the rules it
measures are the app's own. Leaf strings escape \\, newline and tab; sub-lists use the
control separators \\x01 (items), \\x02 (paragraphs) and \\x03 (fields of one element).

    originals.tsv  atMs, title, text
    titles.tsv     title, chatIds…                 (every chat chats/search names)
    dms.tsv        sender, chatId                  (from the phone's chat cache)
    messages.tsv   chatId, msgType, createTime, deleted, mentions, payload
"""

import json

from .lark import Message

FILES = ("originals.tsv", "titles.tsv", "dms.tsv", "messages.tsv")
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
