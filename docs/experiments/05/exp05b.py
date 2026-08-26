import json, sys, time, urllib.request, pathlib, threading
BASE = "https://open.feishu.cn"; APP_ID = "cli_aa949bbb72e39cde"
SECRET = (pathlib.Path(sys.argv[1]) / "app_secret").read_text()
def post(path, body, token=None):
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json; charset=utf-8", **({"Authorization": "Bearer " + token} if token else {})})
    try:
        with urllib.request.urlopen(req, timeout=15) as r: return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e: return e.code, json.loads(e.read())
tok = post("/open-apis/auth/v3/tenant_access_token/internal", {"app_id": APP_ID, "app_secret": SECRET})[1]["tenant_access_token"]
def tr(text, **extra):
    s, p = post("/open-apis/translation/v1/text/translate", {"source_language": "zh", "target_language": "en", "text": text, **extra}, tok)
    return f"http={s} code={p.get('code')} -> {p.get('data', {}).get('text')!r}" + ("" if p.get('code') == 0 else f" msg={p.get('msg')!r}")
for t in ["oec.supply_chain.warehouse_fulfillment 需要扩容。",
          "需要扩容 oec.supply_chain.warehouse_fulfillment",
          "Chenglong Song: 需要扩容。",
          "Zhifu Liu: \"申请字节云IAM权限\" is still awaiting your action",
          "Approval: \"申请字节云IAM权限\" from Zhifu Liu is awaiting your approval",
          "Yifei Cheng: 【直播开启🔛】 生态构建、进化与观测：DeepSeek Harness 技术实践[「TechHub·Live...",
          "陈昱萌😀: 先别发布，等我看一下 😅",
          "Lark\nKani Permission Notification: [CN] August 26 Personal Permission..."]:
    print(f"  {t!r}\n    {tr(t)}")
print("detect-free? source_language omitted:")
s, p = post("/open-apis/translation/v1/text/translate", {"target_language": "en", "text": "需要扩容"}, tok)
print(f"    http={s} code={p.get('code')} msg={p.get('msg')!r} data={p.get('data')}")
print("concurrent x8:")
res = [None]*8
def go(i): res[i] = tr(f"第{i}条：会议改到明天上午十点")
ths = [threading.Thread(target=go, args=(i,)) for i in range(8)]
t0 = time.monotonic(); [t.start() for t in ths]; [t.join() for t in ths]
for i, r in enumerate(res): print(f"  {i}: {r[:60]}")
print(f"  wall {round((time.monotonic()-t0)*1000)}ms")
