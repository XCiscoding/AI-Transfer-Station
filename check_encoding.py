# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding='utf-8')

text1 = '通义千问'
text2 = '文心一言'
text3 = '通义千问-Max'
text4 = '文心一言-4.0'

print('=== 正确UTF-8 HEX值（Python生成）===')
utf8_1 = text1.encode('utf-8')
utf8_2 = text2.encode('utf-8')
utf8_3 = text3.encode('utf-8')
utf8_4 = text4.encode('utf-8')

print(f'通义千问: {utf8_1.hex().upper()} ({len(utf8_1)} bytes)')
print(f'文心一言: {utf8_2.hex().upper()} ({len(utf8_2)} bytes)')
print(f'通义千问-Max: {utf8_3.hex().upper()} ({len(utf8_3)} bytes)')
print(f'文心一言-4.0: {utf8_4.hex().upper()} ({len(utf8_4)} bytes)')

# 验证当前SQL文件内容
sql_file = r'c:\Users\26404\.trae-cn\Project\AI调度中心——企业API Key管理系统\fix-chinese-encoding-v2.sql'
with open(sql_file, 'rb') as f:
    content = f.read()

print(f'\nSQL文件总大小: {len(content)} bytes')

# 查找"通义千问"在文件中的位置和编码
target1 = '通义千问'.encode('utf-8')
if target1 in content:
    idx = content.find(target1)
    print(f'✅ 找到"通义千问"正确UTF-8编码 at position {idx}')
    actual_hex = content[idx:idx+len(target1)].hex().upper()
    print(f'   实际HEX: {actual_hex}')
else:
    print('❌ 未找到"通义千问"的正确UTF-8编码')
    # 尝试查找GBK编码
    try:
        gbk_encoded = '通义千问'.encode('gbk')
        if gbk_encoded in content:
            idx = content.find(gbk_encoded)
            print(f'⚠️  发现GBK编码 at position {idx}')
            print(f'   GBK HEX: {gbk_encoded.hex().upper()}')
    except:
        pass
