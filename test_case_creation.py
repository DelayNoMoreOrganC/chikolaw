#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import requests
import json
import sys

BASE_URL = "http://localhost:8080/api"

print("=" * 80)
print("案件管理功能端到端测试")
print("=" * 80)

# 1. 登录
print("\n1. 登录系统...")
login_resp = requests.post(f"{BASE_URL}/auth/login", json={
    "username": "admin",
    "password": "admin123"
})
if login_resp.json().get('code') != 200:
    print(f"❌ 登录失败: {login_resp.text}")
    sys.exit(1)
token = login_resp.json()['data']['token']
print("✅ 登录成功")

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# 2. 测试新建案件（包含所有新增字段）
print("\n2. 测试新建案件（包含所有新增字段）...")
case_data = {
    "caseType": "CIVIL",
    "caseName": "张三 Vs 李四 买卖合同纠纷",
    "caseReason": "买卖合同纠纷",
    "procedure": "FIRST_INSTANCE",
    "court": "北京市朝阳区人民法院",
    "acceptanceDate": "2025-05-04",
    "filingDate": "2025-05-04",
    "deadlineDate": "2025-08-04",
    "courtCaseNumber": "（2025）京0105民初1234号",
    "hearingDate": "2025-06-15",
    "businessType": "公司",
    "criminalSuspect": None,
    "disputedAmount": 50.0,
    "sourcePerson": json.dumps(["张律师", "李律师"]),
    "sourcePersonPercentage": 30.0,
    "departmentPercentage": 40.0,
    "firmPercentage": 30.0,
    "representationType": None,
    "hostDepartment": json.dumps(["诉讼一部"]),
    "coDepartments": json.dumps(["行政部"]),
    "remark": "测试备注",
    "ownerId": 1,
    "parties": [
        {
            "partyType": "INDIVIDUAL",
            "partyRole": "PLAINTIFF",
            "name": "张三",
            "isClient": True,
            "phone": "13800138000"
        }
    ]
}

create_resp = requests.post(f"{BASE_URL}/cases", headers=headers, json=case_data)
print(f"响应状态码: {create_resp.status_code}")
result = create_resp.json()

if result.get('code') != 200:
    print(f"❌ 创建案件失败:")
    print(json.dumps(result, indent=2, ensure_ascii=False))
    sys.exit(1)

case_id = result.get('data', {}).get('id')
case_number = result.get('data', {}).get('caseNumber')
print(f"✅ 案件创建成功")
print(f"   案件ID: {case_id}")
print(f"   案号: {case_number}")

# 3. 验证新字段是否正确保存
print("\n3. 验证新字段保存...")
detail_resp = requests.get(f"{BASE_URL}/cases/{case_id}", headers=headers)
detail = detail_resp.json().get('data', {})

new_fields = {
    "acceptanceDate": "收案日期",
    "courtCaseNumber": "法院案号",
    "hearingDate": "开庭日期",
    "businessType": "业务类型",
    "disputedAmount": "涉案标的",
    "sourcePerson": "案源人",
    "hostDepartment": "主办部门",
    "coDepartments": "协办部门"
}

all_ok = True
for field, name in new_fields.items():
    value = detail.get(field)
    if value:
        print(f"   ✅ {name}: {value}")
    else:
        print(f"   ❌ {name}: 未保存（为null）")
        all_ok = False

# 4. 验证多选字段
print("\n4. 验证多选字段（JSON数组）...")
source_person = detail.get('sourcePerson')
host_department = detail.get('hostDepartment')

if source_person:
    try:
        source_list = json.loads(source_person) if isinstance(source_person, str) else source_person
        print(f"   ✅ 案源人（多选）: {source_list}")
    except:
        print(f"   ❌ 案源人解析失败: {source_person}")
        all_ok = False

if host_department:
    try:
        dept_list = json.loads(host_department) if isinstance(host_department, str) else host_department
        print(f"   ✅ 主办部门（多选）: {dept_list}")
    except:
        print(f"   ❌ 主办部门解析失败: {host_department}")
        all_ok = False

# 5. 验证分配比例
print("\n5. 验证分配比例总和...")
source_pct = detail.get('sourcePersonPercentage', 0)
dept_pct = detail.get('departmentPercentage', 0)
firm_pct = detail.get('firmPercentage', 0)
total = source_pct + dept_pct + firm_pct
print(f"   案源人比例: {source_pct}%")
print(f"   部门比例: {dept_pct}%")
print(f"   律所比例: {firm_pct}%")
print(f"   总计: {total}%")

if abs(total - 100.0) < 0.01:
    print(f"   ✅ 分配比例总和=100%，验证通过")
else:
    print(f"   ❌ 分配比例总和≠100%，验证失败")
    all_ok = False

# 总结
print("\n" + "=" * 80)
if all_ok:
    print("🎉 所有测试通过！案件管理功能完善完成！")
    print("=" * 80)
else:
    print("⚠️  部分测试失败，请检查相关功能")
    print("=" * 80)
    sys.exit(1)
