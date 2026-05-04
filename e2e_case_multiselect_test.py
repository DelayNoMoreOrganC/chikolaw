#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import requests
import json
import sys
import os

# 设置UTF-8输出
if os.name == 'nt':  # Windows
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE_URL = "http://localhost:8080/api"

print("=" * 80)
print("Case Management E2E Test - Multi-select Verification")
print("=" * 80)

# 1. Test frontend
print("\n[1/5] Testing frontend service...")
try:
    resp = requests.get("http://localhost:3017", timeout=5)
    if resp.status_code == 200:
        print("[PASS] Frontend OK: http://localhost:3017")
    else:
        print(f"[FAIL] Frontend error: {resp.status_code}")
        sys.exit(1)
except Exception as e:
    print(f"[FAIL] Frontend unreachable: {e}")
    sys.exit(1)

# 2. Test backend login
print("\n[2/5] Testing backend login...")
try:
    login_resp = requests.post(f"{BASE_URL}/auth/login", json={
        "username": "admin",
        "password": "admin123"
    }, timeout=5)

    if login_resp.json().get('code') != 200:
        print(f"[FAIL] Login failed: {login_resp.text}")
        sys.exit(1)

    token = login_resp.json()['data']['token']
    print("[PASS] Backend login successful")
except Exception as e:
    print(f"[FAIL] Login error: {e}")
    sys.exit(1)

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# 3. Test case creation with multi-select
print("\n[3/5] Testing case creation (multi-select)...")
case_data = {
    "caseType": "CIVIL",
    "caseName": "端到端测试-多选验证",
    "caseReason": "买卖合同纠纷",
    "procedure": "FIRST_INSTANCE",
    "court": "北京市朝阳区人民法院",
    "acceptanceDate": "2026-05-04",
    "filingDate": "2026-05-04",
    "deadlineDate": "2026-08-04",
    "courtCaseNumber": "（2026）京0105民初9999号",
    "hearingDate": "2026-06-15",
    "businessType": "公司",
    "criminalSuspect": None,
    "disputedAmount": 50.0,
    "sourcePerson": json.dumps(["张律师", "李律师", "王律师"], ensure_ascii=False),
    "sourcePersonPercentage": 30.0,
    "departmentPercentage": 40.0,
    "firmPercentage": 30.0,
    "representationType": None,
    "hostDepartment": json.dumps(["诉讼一部"], ensure_ascii=False),
    "coDepartments": json.dumps(["行政部"], ensure_ascii=False),
    "remark": "端到端测试-多选验证",
    "ownerId": 1,
    "parties": []
}

try:
    create_resp = requests.post(f"{BASE_URL}/cases", headers=headers, json=case_data, timeout=5)

    if create_resp.json().get('code') != 200:
        print(f"[FAIL] Case creation failed:")
        print(json.dumps(create_resp.json(), indent=2, ensure_ascii=False))
        sys.exit(1)

    case_id = create_resp.json().get('data', {}).get('id')
    case_number = create_resp.json().get('data', {}).get('caseNumber')
    print(f"[PASS] Case created successfully")
    print(f"       Case ID: {case_id}")
    print(f"       Case Number: {case_number}")
except Exception as e:
    print(f"[FAIL] Case creation error: {e}")
    sys.exit(1)

# 4. Verify multi-select fields
print("\n[4/5] Verifying multi-select fields...")
try:
    detail_resp = requests.get(f"{BASE_URL}/cases/{case_id}", headers=headers, timeout=5)
    detail = detail_resp.json().get('data', {})

    source_person = detail.get('sourcePerson')
    if source_person:
        source_list = json.loads(source_person) if isinstance(source_person, str) else source_person
        print(f"[PASS] Source Persons (multi-select): {source_list}")
        print(f"       Total: {len(source_list)} lawyers selected")
    else:
        print("[FAIL] Source person field not saved")
        sys.exit(1)

    host_dept = detail.get('hostDepartment')
    if host_dept:
        dept_list = json.loads(host_dept) if isinstance(host_dept, str) else host_dept
        print(f"[PASS] Host Departments (multi-select): {dept_list}")

    total_pct = detail.get('sourcePersonPercentage', 0) + detail.get('departmentPercentage', 0) + detail.get('firmPercentage', 0)
    if abs(total_pct - 100.0) < 0.01:
        print(f"[PASS] Distribution sum = 100% (validated)")
    else:
        print(f"[FAIL] Distribution sum = {total_pct}% (validation failed)")
        sys.exit(1)

except Exception as e:
    print(f"[FAIL] Verification error: {e}")
    sys.exit(1)

# 5. Summary
print("\n" + "=" * 80)
print("[SUCCESS] All tests passed! Case management multi-select feature verified")
print("=" * 80)

print("\nBrowser Access Test:")
print("   1. Visit: http://localhost:3017/case/create")
print("   2. Login: admin / admin123")
print("   3. Click 'Source Person' dropdown")
print("   4. Select multiple lawyers (e.g., Zhang, Li, Wang)")
print("   5. Fill distribution: 30 + 40 + 30 = 100")
print("   6. Click 'Confirm Case Filing'")
print("\n[EXPECTED] Case created successfully, multi-select fields saved correctly")
print("=" * 80)
