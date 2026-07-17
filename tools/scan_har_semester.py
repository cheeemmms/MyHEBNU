import json, re, glob, os

HAR_DIR = r"D:\Personal_file\VibeCoding\Program\My-University\HAR"

def get_text(entry):
    try:
        content = entry["response"]["content"]
        if "text" in content:
            return content["text"]
    except Exception:
        return ""
    return ""

def scan(path):
    print("="*70)
    print("FILE:", os.path.basename(path))
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    entries = data.get("log", {}).get("entries", [])
    for e in entries:
        url = e["request"]["url"]
        txt = get_text(e)
        if not txt:
            continue
        # 1) 找HTML里的学期select
        m = re.search(r'<select[^>]*id="xnm"[^>]*>(.*?)</select>', txt, re.S)
        if m:
            opts = re.findall(r'<option[^>]*value="([^"]*)"[^>]*>([^<]*)</option>', m.group(1))
            print(f"[HTML xnm select] {url}\n   options={opts}")
        m2 = re.search(r'<select[^>]*id="xqm"[^>]*>(.*?)</select>', txt, re.S)
        if m2:
            opts = re.findall(r'<option[^>]*value="([^"]*)"[^>]*>([^<]*)</option>', m2.group(1))
            print(f"[HTML xqm select] {url}\n   options={opts}")
        # 2) 找JSON里的学期列表（含xnmc/xqmc且为数组）
        if '"xnmc"' in txt or '"xqmc"' in txt:
            # 尝试抽取所有 xnmc/xqmc 配对
            pairs = re.findall(r'"xnm"\s*:\s*"([^"]*)".*?"xqm"\s*:\s*"([^"]*)"', txt)
            names = re.findall(r'"xnmc"\s*:\s*"([^"]*)".*?"xqmc"\s*:\s*"([^"]*)"', txt)
            if pairs or names:
                print(f"[JSON semester fields] {url}\n   xnm/xqm={pairs[:6]}\n   xnmc/xqmc={names[:6]}")
        # 3) 找“学年学期”中文 option（如 2024-2025 / 第一学期）
        if re.search(r'option[^>]*value="\d{4}-\d{4}"', txt) or '第一学期' in txt and '第二学期' in txt:
            opts = re.findall(r'<option[^>]*value="([^"]*)"[^>]*>([^<]*)</option>', txt)
            # 只打印含 学期/学年 的
            sem = [o for o in opts if ('学期' in o[1] or re.match(r'\d{4}-\d{4}', o[0]))]
            if sem:
                print(f"[HTML semester options] {url}\n   {sem[:20]}")

for p in glob.glob(os.path.join(HAR_DIR, "*.har")):
    scan(p)
