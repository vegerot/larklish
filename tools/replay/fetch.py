import json, subprocess, sys
from concurrent.futures import ThreadPoolExecutor

SP = "/private/tmp/claude-501/-Users-bytedance-code-github-com-vegerot-larklish/4c03a3c0-9bf0-4e3d-bb84-51c09facf643/scratchpad"
START, END = "1787727600", "1787961600"  # 2026-08-26T00:20Z .. 2026-08-29T00:00Z


def page(chat, token):
    params = {"container_id_type": "chat", "container_id": chat, "sort_type": "ByCreateTimeDesc",
              "page_size": "50", "start_time": START, "end_time": END}
    if token:
        params["page_token"] = token
    for _ in range(3):
        p = subprocess.run(["lark-cli", "api", "GET", "/open-apis/im/v1/messages",
                            "--params", json.dumps(params), "--as", "user"],
                           capture_output=True, text=True)
        try:
            return json.loads(p.stdout)
        except ValueError:
            last = (p.stdout or p.stderr or "")[:120]
    return {"ok": False, "error": "unparsable: " + last}


def fetch(chat):
    items, token, pages = [], None, 0
    while True:
        d = page(chat, token)
        if not d.get("ok"):
            return chat, items, d.get("error")
        data = d["data"]
        items += data.get("items", [])
        pages += 1
        token = data.get("page_token")
        if not data.get("has_more") or not token or pages > 12:
            break
    return chat, items, None


ids = open(f"{SP}/chatids.txt").read().split()
with ThreadPoolExecutor(max_workers=6) as ex:
    for chat, items, err in ex.map(fetch, ids):
        json.dump(items, open(f"{SP}/chatmsgs/{chat}.json", "w"), ensure_ascii=False)
        print(f"{chat} {len(items):4} messages" + (f"  ERR {err}" if err else ""))
