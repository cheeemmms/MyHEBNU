
import json
import sys

def analyze_har(file_path, filter_word=None):
    with open(file_path, 'r', encoding='utf-8') as f:
        har = json.load(f)
    
    entries = har['log']['entries']
    
    for i, entry in enumerate(entries):
        request = entry['request']
        url = request['url']
        
        # 只关注教务系统的 API 请求
        if 'jwgl.hebtu.edu.cn' not in url:
            continue
            
        if filter_word and filter_word not in url:
            continue
            
        response = entry['response']
        
        print(f"--- [{i}] ---")
        print(f"Method: {request['method']}")
        print(f"URL:    {url}")
        
        if 'postData' in request:
            print("POST Data:")
            if 'params' in request['postData']:
                for param in request['postData']['params']:
                    print(f"  {param['name']} = {param['value']}")
        
        print(f"Status: {response['status']}")
        print(f"Content-Type: {response.get('content', {}).get('mimeType', '')}")
        
        if 'content' in response and 'text' in response['content']:
            text = response['content']['text']
            if len(text) > 800:
                print(f"Response: {text[:800]}...")
            else:
                print(f"Response: {text}")
        print()

if __name__ == "__main__":
    if len(sys.argv) > 1:
        filter_word = sys.argv[2] if len(sys.argv) > 2 else None
        analyze_har(sys.argv[1], filter_word)
    else:
        print("Usage: python analyze_har_simple.py <har_file> [filter_word]")
