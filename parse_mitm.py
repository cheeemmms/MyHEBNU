import sys
import json

def parse_mitm_flow_file(file_path):
    try:
        from mitmproxy import io
        from mitmproxy.http import HTTPFlow
        
        with open(file_path, 'rb') as f:
            flow_reader = io.FlowReader(f)
            flows = list(flow_reader.stream())
            
            print(f"📊 共解析到 {len(flows)} 个流量")
            print()
            
            for i, flow in enumerate(flows):
                if not flow.request or not flow.response:
                    continue
                
                url = flow.request.url
                method = flow.request.method
                status = flow.response.status_code
                
                if 'jwgl.hebtu' in url or 'cas.hebtu' in url:
                    print(f"--- [{i}] {method} {url}")
                    print(f"Status: {status}")
                    
                    # 打印请求头
                    print("请求头:")
                    for key, value in flow.request.headers.items():
                        if key.lower() in ['referer', 'cookie', 'x-requested-with', 'user-agent']:
                            print(f"  {key}: {value}")
                    
                    # 如果是 POST 请求，打印表单数据
                    if method == 'POST' and flow.request.content:
                        content_type = flow.request.headers.get('Content-Type', '')
                        if 'form' in content_type.lower():
                            print("POST 表单数据:")
                            for key, value in flow.request.urlencoded_form.items():
                                print(f"  {key} = {value}")
                    
                    # 打印响应头
                    print("响应头:")
                    content_type = flow.response.headers.get('Content-Type', '')
                    print(f"  Content-Type: {content_type}")
                    
                    # 如果响应是 JSON，打印前 500 字符
                    if 'application/json' in content_type:
                        try:
                            body = flow.response.content.decode('utf-8', errors='ignore')
                            print(f"响应内容 (前500字符): {body[:500]}")
                        except:
                            pass
                    
                    print()
                    
    except ImportError:
        print("需要安装 mitmproxy 库: pip install mitmproxy")
        sys.exit(1)
    except Exception as e:
        print(f"解析失败: {e}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        parse_mitm_flow_file(sys.argv[1])
    else:
        print("Usage: python parse_mitm.py <flow_file>")
