"""How much does the window itself cost us? (fix 1's real shape)"""
import fixes as F
befores = (16, 30, 60, 120, 300)
afters = (4, 15, 30, 60)
print("found, loose match + emotion, 210 Relays\n")
print("BEFORE\\AFTER " + "".join(f"{a:>7}s" for a in afters))
for b in befores:
    row = []
    for a in afters:
        F.BEFORE_MS, F.AFTER_MS = b * 1000, a * 1000
        row.append(F.replay(F.REL, emotion=True, loose=True)[0])
    print(f"{b:>10}s  " + "".join(f"{v:>8}" for v in row))
F.BEFORE_MS, F.AFTER_MS = 16_000, 4_000

print("\nfalse positives introduced by a wider window (stems matching 2+ chats):")
msgs = {c: [F.candidate(m, True) for m in ms] for c, ms in F.RAW.items()}
for b, a in ((16, 4), (60, 15), (300, 60)):
    F.BEFORE_MS, F.AFTER_MS = b * 1000, a * 1000
    coll = checked = 0
    for e in F.REL:
        _, message = F.parse_preview(e["text"])
        stem = message[:-3] if message.endswith("...") else message
        if not stem: continue
        where = [c for c, it in msgs.items() if F.pick(it, stem, F.when(e), True)[0] == "found"]
        if where: checked += 1
        if len(where) > 1: coll += 1
    print(f"  window [-{b}s, +{a}s]: {coll} of {checked} stems match more than one chat")
