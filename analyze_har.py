import json
import sys

def analyze_har(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        har = json.load(f)
    
    entries = har['log']['entries']
    print(f"📊 Total entries: {len(entries)}")
    print()
    
    api_calls = []
    
    for entry in entries:
        request = entry['request']
        response = entry['response']
        url = request['url']
        
        # 只关注教务系统的 API 请求
        if 'jwgl.hebtu.edu.cn' in url:
            api_calls.append({
                'method': request['method'],
                'url': url,
                'post_data': request.get('postData', {}),
                'status': response['status'],
                'content_type': response.get('content', {}).get('mimeType', ''),
                'content_size': response.get('content', {}).get('size', 0),
                'content_text': response.get('content', {}).get('text', '')
            })
    
    print(f"🔍 找到 {len(api_calls)} 个教务系统请求:")
    print()
    
    for i, call in enumerate(api_calls):
        print(f"--- [ {i+1} ] ---")
        print(f"Method: {call['method']}")
        print(f"URL:    {call['url']}")
        
        if call['post_data']:
            print("📝 POST Data:")
            if 'params' in call['post_data']:
                for param in call['post_data']['params']:
                    print(f"  {param['name']} = {param['value']}")
            elif 'text' in call['post_data']:
                print(f"  Text: {call['post_data']['text'][:200]}")
        
        print(f"Status: {call['status']}")
        print(f"Content-Type: {call['content_type']}")
        print(f"Size: {call['content_size']} bytes")
        
        # 如果有内容，显示前面部分
        if call['content_text']:
            text = call['content_text']
            print("📄 Response Preview:")
            print(text[:500])  # 显示前 500 字符
            if len(text) > 500:
                print(f"... [截断了 {len(text)-500} 字符]")
        
        print()

if __name__ == "__main__":
    if len(sys.argv) > 1:
        analyze_har(sys.argv[1])
    else:
        print("Usage: python analyze_har.py <har_file>")
