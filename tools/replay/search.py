import json, subprocess
from concurrent.futures import ThreadPoolExecutor
SP="/private/tmp/claude-501/-Users-bytedance-code-github-com-vegerot-larklish/4c03a3c0-9bf0-4e3d-bb84-51c09facf643/scratchpad"

def search(title):
    stem = title[:-3] if title.endswith("...") else title
    out = subprocess.run(["lark-cli","api","GET","/open-apis/im/v1/chats/search",
                          "--params", json.dumps({"query": stem, "page_size": 20}), "--as","user"],
                         capture_output=True, text=True).stdout
    d = json.loads(out)
    hits = d.get("data",{}).get("items",[]) if d.get("ok") else []
    if title.endswith("..."):
        match = [h for h in hits if h["name"].startswith(stem)]
    else:
        match = [h for h in hits if h["name"] == title]
    return title, [(h["chat_id"], h["name"]) for h in match], len(hits)

titles = open(f"{SP}/titles.txt").read().splitlines()
res = {}
with ThreadPoolExecutor(max_workers=6) as ex:
    for title, match, nhits in ex.map(search, titles):
        res[title] = match
        names = ", ".join(n[:38] for _, n in match[:3])
        print(f"{len(match):2} of {nhits:2}  [{title}]  → {names}")
json.dump(res, open(f"{SP}/titlehits.json","w"), ensure_ascii=False, indent=1)
