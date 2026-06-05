import json

with open('Xiaomi-24129PN74C-Android-16_2026-06-05_140737.logcat', 'r', encoding='utf-8', errors='ignore') as f:
    data = json.load(f)
entries = data.get('logcatMessages', [])

# Find crashes and error messages
for e in entries:
    msg = str(e.get('message', ''))
    tag = str(e.get('tag', ''))
    combined = tag + ' ' + msg
    if any(kw in combined for kw in ['FATAL', 'AndroidRuntime', 'Process: com.myhebnu', 'Caused by:', 'java.lang.']):
        print(msg[:400])
        print()

# Also find ALL entries with non-empty tags from our app
print('\n=== Entries with tags from com.myhebnu ===')
for e in entries:
    hdr = e.get('header', {})
    if str(hdr.get('applicationId', '')) == 'com.myhebnu':
        t = str(e.get('tag', ''))
        if t:
            print(f'tag=[{t}] {str(e.get("message",""))[:200]}')
