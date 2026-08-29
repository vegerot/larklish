"""How long after the message does its Original arrive?"""
import statistics
import fixes as F
F.BEFORE_MS, F.AFTER_MS = 600_000, 600_000
msgs = {c: [F.candidate(m, True) for m in ms] for c, ms in F.RAW.items()}
def resolve(e):
    s, _ = F.parse_preview(e["text"])
    if e["title"] == "Lark" or e["title"] == s: return F.DMMAP.get("dm:" + s)
    h = [c for c, _ in (F.HITS.get(e["title"]) or [])]
    return h[0] if h else None
d = []
for e in F.REL:
    cid = resolve(e)
    if not cid or cid not in msgs: continue
    _, message = F.parse_preview(e["text"])
    stem = message[:-3] if message.endswith("...") else message
    r, c = F.pick(msgs[cid], stem, F.when(e), True)
    if r == "found": d.append((F.when(e) - c["t"]) / 1000)
d.sort()
n = len(d)
print(f"Original minus message, {n} matched Relays (seconds; positive = the Original is later)")
for q in (0, .5, .75, .9, .95, .99, 1):
    print(f"  p{int(q*100):<3} {d[min(int(q*(n-1)), n-1)]:8.1f}s")
print(f"  mean {statistics.mean(d):.1f}s")
print("\n  under 15 s :", sum(1 for x in d if x <= 15), f"({sum(1 for x in d if x<=15)*100//n} %)")
print("  under 30 s :", sum(1 for x in d if x <= 30))
print("  under 60 s :", sum(1 for x in d if x <= 60))
print("  over  60 s :", sum(1 for x in d if x > 60), "→", [round(x) for x in d if x > 60])
