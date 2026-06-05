import json

with open('Xiaomi-24129PN74C-Android-16_2026-06-05_140136.logcat', 'r', encoding='utf-8', errors='ignore') as f:
    data = json.load(f)

entries = data.get('logcatMessages', [])
print(f'Total: {len(entries)}')

# ALL logs from our app
print('\n=== ALL com.myhebnu process logs ===')
cnt = 0
for e in entries:
    hdr = e.get('header', {})
    app_id = str(hdr.get('applicationId', ''))
    if app_id == 'com.myhebnu':
        tag = str(e.get('tag', ''))
        msg = str(e.get('message', ''))
        lvl = str(hdr.get('logLevel', ''))
        print(f'{lvl:6s} tag=[{tag}] {msg[:300]}')
        cnt += 1
print(f'\nCount: {cnt}')

# Look for crashes
print('\n=== FATAL / AndroidRuntime ===')
for e in entries:
    tag = str(e.get('tag', ''))
    msg = str(e.get('message', ''))
    if 'FATAL' in tag or 'AndroidRuntime' in tag or 'crash' in tag.lower():
        print(f'{msg[:500]}')
