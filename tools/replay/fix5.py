"""Fix 5: would Lark have handled the 11 fallback texts, and how often does it fail?"""
import json, subprocess, time

SP = "/private/tmp/claude-501/-Users-bytedance-code-github-com-vegerot-larklish/4c03a3c0-9bf0-4e3d-bb84-51c09facf643/scratchpad"

def translate(text):
    p = subprocess.run(["lark-cli", "api", "POST", "/open-apis/translation/v1/text/translate",
                        "--data", json.dumps({"source_language": "zh", "target_language": "en", "text": text}),
                        "--as", "bot"], capture_output=True, text=True)
    try:
        d = json.loads(p.stdout)
    except ValueError:
        return None, "unparsable"
    if not d.get("ok"):
        return None, d.get("error", {}).get("subtype") or d.get("error", {}).get("type") or "error"
    return d["data"]["text"], None

def has_han(s): return any("一" <= c <= "鿿" for c in s)

rows = json.load(open(f"{SP}/fallbacks.json"))
print(f"## The {len(rows)} fallback Relays, re-translated through Lark today\n")
ok = unchanged = failed = 0
for r in rows:
    for part, src in (("title", r["title"]), ("text", r["text"].split(": ", 1)[-1])):
        if not has_han(src): continue
        out, err = translate(src)
        if err:
            failed += 1; verdict = f"FAILED ({err})"
        elif out == src:
            unchanged += 1; verdict = "UNCHANGED (would still fall back)"
        else:
            ok += 1; verdict = "ok"
        print(f"  [{part}] {src[:52]}")
        print(f"      → {verdict}: {(out or '')[:70]}")
print(f"\n  ok {ok}   unchanged {unchanged}   failed {failed}")

print("\n## Live failure rate: 60 sequential translate calls")
fails, kinds = 0, {}
t0 = time.time()
for i in range(60):
    out, err = translate("这是一个测试消息，用来测量翻译接口的失败率。")
    if err:
        fails += 1; kinds[err] = kinds.get(err, 0) + 1
print(f"  {fails} of 60 failed in {time.time()-t0:.0f}s  {kinds}")
