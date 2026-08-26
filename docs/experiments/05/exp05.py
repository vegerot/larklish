import json, sys, time, urllib.request, pathlib
BASE = "https://open.feishu.cn"
APP_ID = "cli_aa949bbb72e39cde"
SECRET = (pathlib.Path(sys.argv[1]) / "app_secret").read_text()

def post(path, body, token=None):
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json; charset=utf-8",
                 **({"Authorization": "Bearer " + token} if token else {})})
    t0 = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            status, payload = r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        status, payload = e.code, json.loads(e.read())
    return status, payload, round((time.monotonic() - t0) * 1000)

# 1. token
s, p, ms = post("/open-apis/auth/v3/tenant_access_token/internal", {"app_id": APP_ID, "app_secret": SECRET})
print(f"token: http={s} code={p.get('code')} expire={p.get('expire')}s {ms}ms")
tok = p["tenant_access_token"]

def tr(text, token=tok):
    return post("/open-apis/translation/v1/text/translate",
                {"source_language": "zh", "target_language": "en", "text": text}, token)

# 2. one translate
s, p, ms = tr("Chenglong Song: oec.supply_chain.warehouse_fulfillment 需要扩容。")
print(f"translate: http={s} code={p.get('code')} {ms}ms -> {p.get('data', {}).get('text')!r}")

# 3. title + message in one call, newline separated
s, p, ms = tr("【高优】容灾建设专项-2026\nChenglong Song: oec.supply_chain.warehouse_fulfillment 需要扩容。")
print(f"combined: http={s} code={p.get('code')} {ms}ms -> {p.get('data', {}).get('text')!r}")

# 4. burst: 8 back-to-back calls
print("burst x8 (no spacing):")
for i in range(8):
    s, p, ms = tr(f"第{i}条：请在今天下午三点前完成灰度发布")
    print(f"  {i}: http={s} code={p.get('code')} {ms}ms msg={p.get('msg')!r}" + (f" -> {p['data']['text']!r}" if p.get('code') == 0 else ""))

# 5. burst with 1 s spacing
time.sleep(4)
print("spaced x5 (1 s):")
for i in range(5):
    s, p, ms = tr(f"第{i}条：预计今晚上线，请关注报警")
    print(f"  {i}: http={s} code={p.get('code')} {ms}ms")
    time.sleep(1)

# 6. bad token error shape
s, p, ms = tr("你好", token="t-bogus")
print(f"bad token: http={s} code={p.get('code')} msg={p.get('msg')!r}")
