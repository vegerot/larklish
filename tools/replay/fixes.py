"""Measure all five Layer 6 fixes on the same replay. Each alone, then cumulative."""
import json, re, datetime, os
from collections import OrderedDict

SP = "/private/tmp/claude-501/-Users-bytedance-code-github-com-vegerot-larklish/4c03a3c0-9bf0-4e3d-bb84-51c09facf643/scratchpad"
BEFORE_MS, AFTER_MS, PREFIX = 16_000, 4_000, 12

def squash(s): return "".join(c for c in s if not c.isspace())
def resolve_mentions(t, m): return re.sub(r"@_user_\d+", lambda x: "@" + m[x.group()] if x.group() in m else x.group(), t)

def post_text(post, emotion):
    lines = []
    for para in post["content"]:
        out = ""
        for e in para:
            tag = e["tag"]
            out += (e.get("text", "") if tag in ("text", "a") else
                    "@" + e.get("user_name", "") if tag == "at" else
                    "[image]" if tag == "img" else
                    f"[{e.get('emoji_type', 'emotion')}]" if tag == "emotion" and emotion else
                    f"[{tag}]")
        lines.append(out)
    return "\n".join(x for x in [post.get("title") or ""] + lines if x)

def candidate(m, emotion):
    typ, deleted, body = m["msg_type"], m.get("deleted", False), m["body"]["content"]
    mentions = {x["key"]: x["name"] for x in (m.get("mentions") or [])}
    raw = ("" if deleted else json.loads(body)["text"] if typ == "text"
           else post_text(json.loads(body), emotion) if typ == "post" else "")
    return dict(type=typ, t=int(m["create_time"]), deleted=deleted, text=resolve_mentions(raw, mentions))

def pick(items, stem, when, loose):
    win = sorted((c for c in items if not c["deleted"] and when - BEFORE_MS <= c["t"] <= when + AFTER_MS), key=lambda c: -c["t"])
    need = squash(stem).casefold()[:PREFIX] if loose else squash(stem)
    for c in win:
        if c["text"] and (squash(c["text"]).casefold() if loose else squash(c["text"])).startswith(need):
            return "found", c
    return ("no-message", None) if not win else ("no-match", None) if win[0]["text"] else (f"type:{win[0]['type']}", None)

def parse_preview(text):
    if text.endswith(":...") and ": " not in text: text = text[:-3] + " ..."
    i = text.find(": ")
    if i < 0: return "", text
    h = text[:i]
    return (h[:-4] if h.endswith(("@you", "@all")) else h), text[i + 2:]

RAW = {f[:-5]: json.load(open(f"{SP}/chatmsgs/{f}")) for f in os.listdir(f"{SP}/chatmsgs") if f.endswith(".json")}
HITS = json.load(open(f"{SP}/titlehits.json"))
DMMAP = {k: v for k, v in json.load(open(f"{SP}/chats.json")).items() if k.startswith("dm:")}
ALL = [json.loads(l) for l in open(f"{SP}/soak2.jsonl") if l.strip()]
REL = [e for e in ALL if e["event"] == "relayed"]
def when(e): return int(datetime.datetime.fromisoformat(e["at"].replace("Z", "+00:00")).timestamp() * 1000)

def replay(ev, emotion=False, loose=False, disambiguate=False, lru_n=0):
    """fix 2a=emotion, 2b=loose, 3=disambiguate, 4=lru_n. fix 1 is implicit: the API is fully indexed."""
    msgs = {c: [candidate(m, emotion) for m in ms] for c, ms in RAW.items()}
    cache, lru = {}, OrderedDict()
    calls = found = 0
    outcomes = []
    for e in ev:
        sender, message = parse_preview(e["text"])
        stem = message[:-3] if message.endswith("...") else message
        is_dm = e["title"] == "Lark" or e["title"] == sender
        key = "dm:" + sender if is_dm else e["title"]
        if key in cache:                 cands = [cache[key]]
        elif is_dm and key in DMMAP:     cands = [DMMAP[key]]
        elif is_dm:                      cands = []
        else:
            h = [c for c, _ in (HITS.get(e["title"]) or [])]
            cands = h if disambiguate else h[:1]
        res, chat = "no-chat", None
        for cid in cands:
            if cid not in msgs: continue
            calls += 1
            r, _ = pick(msgs[cid], stem, when(e), loose)
            if r == "found": res, chat = r, cid; break
            if res == "no-chat": res = r
        if res != "found" and lru_n:
            for cid in [c for c in reversed(lru) if c in msgs][:lru_n]:
                calls += 1
                if pick(msgs[cid], stem, when(e), loose)[0] == "found":
                    res, chat = "found", cid; break
        if res == "found":
            found += 1
            cache[key] = chat
            lru.pop(chat, None); lru[chat] = 0
        elif len(cands) == 1 and not is_dm:
            cache[key] = cands[0]          # today's behaviour: cache an unconfirmed guess
        outcomes.append(res)
    return found, calls, outcomes

if __name__ == "__main__":
    base = dict(emotion=False, loose=False, disambiguate=False, lru_n=0)
    steps = [
        ("baseline (today's code)",                     base),
        ("fix 2a  emotion → [emoji_type]",              {**base, "emotion": True}),
        ("fix 2b  case-insensitive 12-char prefix",     {**base, "loose": True}),
        ("fix 2   both halves",                         {**base, "emotion": True, "loose": True}),
        ("fix 3   disambiguate prefix titles",          {**base, "disambiguate": True}),
        ("fix 4   LRU 2 fallback",                      {**base, "lru_n": 2}),
    ]
    print(f"{len(REL)} Relays, 2026-08-26 → 28\n")
    print(f"{'each fix alone':44} {'found':>6} {'Δ':>4} {'GETs':>6}")
    b, bc, _ = replay(REL, **base)
    for label, kw in steps:
        f, c, _ = replay(REL, **kw)
        print(f"{label:44} {f:6} {f-b:+4} {c:6}")
    print()
    print(f"{'cumulative':44} {'found':>6} {'Δ':>4} {'GETs':>6}")
    cum, prev = dict(base), b
    for label, kw in [("+ fix 2 (emotion + prefix)", {"emotion": True, "loose": True}),
                      ("+ fix 3 (disambiguate)",     {"disambiguate": True}),
                      ("+ fix 4 (LRU 2)",            {"lru_n": 2})]:
        cum.update(kw)
        f, c, _ = replay(REL, **cum)
        print(f"{label:44} {f:6} {f-prev:+4} {c:6}")
        prev = f
