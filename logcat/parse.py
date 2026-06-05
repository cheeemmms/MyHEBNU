import json, sys

fname = sys.argv[1] if len(sys.argv) > 1 else 'Xiaomi-24129PN74C-Android-16_2026-06-05_140737.logcat'

with open(fname, 'r', encoding='utf-8', errors='ignore') as f:
    data = json.load(f)

entries = data.get('logcatMessages', [])
print(f'Total: {len(entries)}')

# Search for our log messages in the message field
print('\n--- Messages containing MyHEBNU/WebView/hasValid/onPage ---')
for e in entries:
    hdr = e.get('header', {})
    app_id = str(hdr.get('applicationId', ''))
    tag = str(e.get('tag', ''))
    msg = str(e.get('message', ''))
    combined = msg.lower()
    keywords = ['myhebnu', 'hasvalid', 'onpage', 'onreceived', 'composing', 'showing login', 'showing loading', 'showing main']
    if any(kw in combined for kw in keywords):
        print(f'  tag=[{tag}] app={app_id}')
        print(f'  {msg[:400]}')
        print()

# Search for critical JSON structure to understand why tags are empty
print('\n--- Sample raw entry with tag ---')
for e in entries:
    if str(e.get('tag', '')) != '':
        print(json.dumps(e, indent=2)[:500])
        break

print('\n--- First 3 entries from com.myhebnu (raw) ---')
cnt = 0
for e in entries:
    hdr = e.get('header', {})
    if str(hdr.get('applicationId', '')) == 'com.myhebnu':
        print(json.dumps(e, indent=2, ensure_ascii=False)[:400])
        print()
        cnt += 1
        if cnt >= 3:
            break
