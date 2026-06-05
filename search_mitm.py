import re

def search_jwgl_requests(file_path):
    with open(file_path, 'rb') as f:
        content = f.read().decode('utf-8', errors='ignore')
    
    # 查找所有包含 jwgl.hebtu 的 URL
    url_pattern = r'(https?://jwgl\.hebtu[^"\s]+)'
    urls = re.findall(url_pattern, content)
    
    if not urls:
        print(f"在 {file_path} 中未找到 jwgl.hebtu 的请求")
        return
    
    print(f"📊 在 {file_path} 中找到 {len(urls)} 个教务系统请求:")
    print()
    
    for i, url in enumerate(urls):
        # 检查是否是课表相关请求
        is_schedule = 'xskbcx' in url.lower()
        is_grade = 'cjcx' in url.lower()
        is_room = 'cdjy' in url.lower()
        is_exam = 'kscx' in url.lower()
        
        print(f"--- [{i+1}] ---")
        print(f"URL: {url}")
        
        # 尝试提取请求类型
        if is_schedule:
            print("类型: 课表查询")
        elif is_grade:
            print("类型: 成绩查询")
        elif is_room:
            print("类型: 空教室查询")
        elif is_exam:
            print("类型: 考试查询")
        
        print()

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        search_jwgl_requests(sys.argv[1])
    else:
        print("Usage: python search_mitm.py <file_path>")
