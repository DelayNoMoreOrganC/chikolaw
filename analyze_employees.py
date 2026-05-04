#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""分析员工数据"""
import json
from collections import Counter

with open(r"D:\ZGAI\employees_data.json", "r", encoding="utf-8") as f:
    data = json.load(f)

print(f"Total employees: {len(data)}")
print("\nJob types:")
types = [emp.get("类型", "Unknown") for emp in data if emp.get("类型")]
for t, c in Counter(types).most_common():
    print(f"  {t}: {c}")

print("\nLooking for '曾进朗':")
for emp in data:
    if "曾进朗" in emp.get("姓名", ""):
        print(json.dumps(emp, ensure_ascii=False, indent=2))
