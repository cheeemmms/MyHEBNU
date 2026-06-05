import re

def analyze_file(file_path):
    with open(file_path, 'rb') as f:
        content = f.read().decode('utf-8', errors='ignore')
    
    # 统计各类请求数量
    login_count = len(re.findall(r'login_slogin\.html', content))
    menu_count = len(re.findall(r'index_initMenu\.html', content))
    schedule_page_count = len(re.findall(r'xskbcx_cxXskbcxIndex\.html', content))
    schedule_data_count = len(re.findall(r'xskbcx_cxXsgrkb\.html', content))
    grade_count = len(re.findall(r'cjcx_cxXsKcList\.html', content))
    
    # 查找所有 JSESSIONID
    jsession_ids = re.findall(r'JSESSIONID=([A-Z0-9]+)', content)
    unique_jsession = set(jsession_ids)
    
    # 检查是否有 POST 请求到课表数据接口
    has_schedule_post = 'xskbcx_cxXsgrkb' in content and 'POST' in content
    
    return {
        'login': login_count,
        'menu': menu_count,
        'schedule_page': schedule_page_count,
        'schedule_data': schedule_data_count,
        'grade': grade_count,
        'jsession_count': len(unique_jsession),
        'has_schedule_post': has_schedule_post
    }

if __name__ == "__main__":
    files = [
        "D:\\Personal_file\\VibeCoding\\Program\\My-University\\mitm\\浏览器访问",
        "D:\\Personal_file\\VibeCoding\\Program\\My-University\\mitm\\MyHEBNU",
        "D:\\Personal_file\\VibeCoding\\Program\\My-University\\mitm\\MyHEBNU2"
    ]
    
    print(f"{'文件':<20} {'登录':<6} {'菜单':<6} {'课表页面':<8} {'课表数据':<8} {'成绩':<6} {'JSESSION':<10} {'POST课表':<10}")
    print("-" * 90)
    
    for file in files:
        result = analyze_file(file)
        filename = file.split('\\')[-1]
        print(f"{filename:<20} {result['login']:<6} {result['menu']:<6} {result['schedule_page']:<8} {result['schedule_data']:<8} {result['grade']:<6} {result['jsession_count']:<10} {result['has_schedule_post']:<10}")
