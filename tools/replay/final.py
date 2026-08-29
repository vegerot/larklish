import fixes as F

def run(before=16, after=4, **kw):
    F.BEFORE_MS, F.AFTER_MS = before * 1000, after * 1000
    return F.replay(F.REL, **kw)

base = dict(emotion=False, loose=False, disambiguate=False, lru_n=0)
b, bc, _ = run(**base)
print(f"210 Relays, 2026-08-26 → 28.  Baseline (today's code): {b} found, {bc} GETs\n")
print(f"{'fix, alone':52} {'found':>6} {'Δ':>4} {'GETs':>6}")
alone = [
    ("1  retry the message list (forward edge +4 s → +60 s)", dict(after=60)),
    ("1' widen the backward edge (15 s → 60 s)",              dict(before=60)),
    ("2a emotion → [emoji_type]",                             dict(emotion=True)),
    ("2b case-insensitive 12-char prefix",                    dict(loose=True)),
    ("3  disambiguate prefix titles",                         dict(disambiguate=True)),
    ("4  LRU 2 fallback on no-chat",                          dict(lru_n=2)),
]
for label, kw in alone:
    f, c, _ = run(**{**base, **kw})
    print(f"{label:52} {f:6} {f-b:+4} {c:6}")

print(f"\n{'stacked, in the order worth building':52} {'found':>6} {'Δ':>4} {'GETs':>6}")
cum, prev = dict(base), b
for label, kw in [("+ 1' window 60 s",            dict(before=60)),
                  ("+ 2b prefix match",           dict(loose=True)),
                  ("+ 2a emotion",                dict(emotion=True)),
                  ("+ 4  LRU 2",                  dict(lru_n=2)),
                  ("+ 3  disambiguate",           dict(disambiguate=True))]:
    cum.update(kw)
    f, c, _ = run(**cum)
    print(f"{label:52} {f:6} {f-prev:+4} {c:6}")
    prev = f
print(f"\n  {b} → {prev} of 210 Relays ({b*100//210} % → {prev*100//210} %), GETs {bc} → {c}")
