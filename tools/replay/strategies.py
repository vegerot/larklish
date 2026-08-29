"""Fix 4 experiment: replay the whole record through three thread-resolver strategies."""
import json, re, datetime, os
from collections import OrderedDict

SP = "/private/tmp/claude-501/-Users-bytedance-code-github-com-vegerot-larklish/4c03a3c0-9bf0-4e3d-bb84-51c09facf643/scratchpad"
BEFORE_MS, AFTER_MS, PREFIX = 16_000, 4_000, 12

def squash(s): return "".join(c for c in s if not c.isspace())
def resolve_mentions(t, m): return re.sub(r"@_user_\d+", lambda x: "@" + m[x.group()] if x.group() in m else x.group(), t)

def post_text(post):
    lines = []
    for para in post["content"]:
        out = ""
        for e in para:
            tag = e["tag"]
            out += e.get("text", "") if tag in ("text", "a") else \
                   "@" + e.get("user_name", "") if tag == "at" else \
                   "[image]" if tag == "img" else f"[{tag}]"
        lines.append(out)
    return "\n".join(x for x in [post.get("title") or ""] + lines if x)

def candidate(m):
    typ, deleted, body = m["msg_type"], m.get("deleted", False), m["body"]["content"]
    mentions = {x["key"]: x["name"] for x in (m.get("mentions") or [])}
    raw = "" if deleted else json.loads(body)["text"] if typ == "text" else post_text(json.loads(body)) if typ == "post" else ""
    return dict(type=typ, t=int(m["create_time"]), deleted=deleted, text=resolve_mentions(raw, mentions))

def pick(items, stem, when, loose):
    win = sorted((c for c in items if not c["deleted"] and when - BEFORE_MS <= c["t"] <= when + AFTER_MS), key=lambda c: -c["t"])
    need = squash(stem).casefold()[:PREFIX] if loose else squash(stem)
    for c in win:
        if c["text"] and (squash(c["text"]).casefold() if loose else squash(c["text"])).startswith(need):
            return "found", c
    return ("no-message", None) if not win else \
           ("no-match", None) if win[0]["text"] else (f"type:{win[0]['type']}", None)

def parse_preview(text):
    if text.endswith(":...") and ": " not in text: text = text[:-3] + " ..."
    i = text.find(": ")
    if i < 0: return "", text
    h = text[:i]
    return (h[:-4] if h.endswith(("@you", "@all")) else h), text[i + 2:]

# ---------- data ----------
msgs = {f[:-5]: [candidate(m) for m in json.load(open(f"{SP}/chatmsgs/{f}"))]
        for f in os.listdir(f"{SP}/chatmsgs") if f.endswith(".json")}
hits = json.load(open(f"{SP}/titlehits.json"))
dmmap = {k: v for k, v in json.load(open(f"{SP}/chats.json")).items() if k.startswith("dm:")}
ev = [json.loads(l) for l in open(f"{SP}/soak2.jsonl") if l.strip() and json.loads(l)["event"] == "relayed"]
def when(e): return int(datetime.datetime.fromisoformat(e["at"].replace("Z", "+00:00")).timestamp() * 1000)

def run(strategy, n_lru=3, loose=False, root_cache=False, also_on_miss=False):
    cache, lru, roots = {}, OrderedDict(), {}
    calls = found = 0
    outcomes = []
    for e in ev:
        sender, message = parse_preview(e["text"])
        stem = message[:-3] if message.endswith("...") else message
        is_dm = e["title"] == "Lark" or e["title"] == sender
        key = "dm:" + sender if is_dm else e["title"]
        chat = cache.get(key) or (dmmap.get(key) if is_dm else None)
        if chat is None and not is_dm:                     # baseline title lookup
            h = hits.get(e["title"]) or []
            if h: chat = h[0][0]
        if chat: cache[key] = chat
        res, cand = ("no-chat", None)
        if chat and chat in msgs:
            calls += 1
            res, cand = pick(msgs[chat], stem, when(e), loose)
        retry = res == "no-chat" or (also_on_miss and res in ("no-message", "no-match"))
        if retry and strategy != "none":
            if root_cache and roots.get(e["title"]) in msgs:
                calls += 1
                r2, c2 = pick(msgs[roots[e["title"]]], stem, when(e), loose)
                if r2 == "found": res, cand, chat = r2, c2, roots[e["title"]]
            if res != "found":
                order = [c for c in reversed(lru) if c in msgs]
                order = order if strategy == "all" else order[:n_lru]
                for cid in order:
                    calls += 1
                    r2, c2 = pick(msgs[cid], stem, when(e), loose)
                    if r2 == "found":
                        res, cand, chat = r2, c2, cid
                        roots[e["title"]] = cid
                        break
        if res == "found":
            found += 1
            lru.pop(chat, None); lru[chat] = 0
        outcomes.append(res)
    return found, calls, outcomes

print(f"{len(ev)} Relays replayed (2026-08-26 → 28)\n")
print(f"{'strategy':34} {'strict':>8} {'loose':>8} {'GETs(loose)':>12}")
rows = [
    ("baseline (chats/search only)",      dict(strategy="none")),
    ("A: LRU 1",                          dict(strategy="lru", n_lru=1)),
    ("A: LRU 2",                          dict(strategy="lru", n_lru=2)),
    ("A: LRU 3",                          dict(strategy="lru", n_lru=3)),
    ("A: LRU 5",                          dict(strategy="lru", n_lru=5)),
    ("B: scan every cached chat",         dict(strategy="all")),
    ("C: thread-root cache alone",        dict(strategy="none", root_cache=True)),
    ("C+A(3): root cache, then LRU 3",     dict(strategy="lru", n_lru=3, root_cache=True)),
    ("C+B: root cache, then scan all",    dict(strategy="all", root_cache=True)),
]
for label, kw in rows:
    s, _, _ = run(loose=False, **kw)
    l, calls, _ = run(loose=True, **kw)
    print(f"{label:34} {s:8} {l:8} {calls:12}")

# ---------- detail: every Relay whose title chats/search cannot resolve ----------
print("\nRelays whose title has no chats/search hit (the thread/reaction/reminder titles):")
_, _, base = run(strategy="none", loose=True)
_, _, best = run(strategy="all", loose=True)
n = 0
for e, b, a in zip(ev, base, best):
    if (hits.get(e["title"]) or []) or e["title"] == "Lark":
        continue
    sender, _ = parse_preview(e["text"])
    if e["title"] == sender: continue
    n += 1
    mark = "RECOVERED" if a == "found" else a
    print(f"  {e['at'][5:16]} {mark:11} [{e['title']}] {e['text'][:56]}")
print(f"  → {n} rows, {sum(1 for e,b,a in zip(ev,base,best) if a=='found' and not (hits.get(e['title']) or []) and e['title']!='Lark' and e['title']!=parse_preview(e['text'])[0])} recovered")

print("\nIf the same fallback also runs after no-message / no-match:")
for label, kw in [("A: LRU 3  + on miss", dict(strategy="lru", n_lru=3, also_on_miss=True)),
                  ("B: scan all + on miss", dict(strategy="all", also_on_miss=True))]:
    s, _, _ = run(loose=False, **kw); l, c, _ = run(loose=True, **kw)
    print(f"  {label:24} strict {s}  loose {l}  GETs {c}")

# ---------- false-positive risk: could another chat match the same stem? ----------
print("\nCollision check — stems that match in more than one chat (loose, 12 chars):")
collisions = 0
checked = 0
for e in ev:
    sender, message = parse_preview(e["text"])
    stem = message[:-3] if message.endswith("...") else message
    if not stem: continue
    where = [cid for cid, items in msgs.items() if pick(items, stem, when(e), True)[0] == "found"]
    if not where: continue
    checked += 1
    if len(where) > 1:
        collisions += 1
        print(f"  {e['at'][5:16]} {len(where)} chats  [{e['title']}] {e['text'][:50]}")
print(f"  → {collisions} of {checked} stems match in more than one chat")

# ---------- fix 3 in the same harness: try every prefix hit, cache only what matched ----------
def run3(n_lru=2, loose=True, disambiguate=True):
    cache, lru = {}, OrderedDict()
    calls = found = 0
    for e in ev:
        sender, message = parse_preview(e["text"])
        stem = message[:-3] if message.endswith("...") else message
        is_dm = e["title"] == "Lark" or e["title"] == sender
        key = "dm:" + sender if is_dm else e["title"]
        cands = []
        if key in cache: cands = [cache[key]]
        elif is_dm and key in dmmap: cands = [dmmap[key]]
        elif not is_dm:
            h = [c for c, _ in (hits.get(e["title"]) or [])]
            cands = h if disambiguate else h[:1]
        res, chat = "no-chat", None
        for cid in cands:
            if cid not in msgs: continue
            calls += 1
            r, _ = pick(msgs[cid], stem, when(e), loose)
            if r == "found": res, chat = r, cid; break
            if res == "no-chat": res = r
        if res != "found":
            for cid in [c for c in reversed(lru) if c in msgs][:n_lru]:
                calls += 1
                if pick(msgs[cid], stem, when(e), loose)[0] == "found":
                    res, chat = "found", cid; break
        if res == "found":
            found += 1
            cache[key] = chat                       # cache only a chat a message confirmed
            lru.pop(chat, None); lru[chat] = 0
        elif len(cands) == 1 and not is_dm:
            cache[key] = cands[0]
    return found, calls

for label, kw in [("first prefix hit only (today)", dict(disambiguate=False, n_lru=0)),
                  ("+ LRU 2",                       dict(disambiguate=False, n_lru=2)),
                  ("try every prefix hit",          dict(disambiguate=True,  n_lru=0)),
                  ("try every prefix hit + LRU 2",  dict(disambiguate=True,  n_lru=2))]:
    f, c = run3(**kw)
    print(f"  {label:32} found {f:4}   GETs {c:5}")

# ---------- same window as the real Layer 5 run, for a like-for-like number ----------
ev = [e for e in ev if e["at"] >= "2026-08-27T23:09"]
print(f"\nRestricted to the Layer 5 soak window ({len(ev)} Relays; the real run Updated 34):")
for label, kw in [("baseline, strict match", dict(strategy="none", loose=False)),
                  ("baseline, loose match",  dict(strategy="none", loose=True)),
                  ("loose + LRU 2",          dict(strategy="lru", n_lru=2, loose=True))]:
    f, c, _ = run(**kw)
    print(f"  {label:26} found {f:3} / {len(ev)}   GETs {c}")
