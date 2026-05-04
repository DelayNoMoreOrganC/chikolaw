#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成账号清单"""

import json
import requests

# 登录获取token
login_resp = requests.post(
    "http://localhost:8080/api/auth/login",
    json={"username": "admin", "password": "admin123"}
)
token = login_resp.json()["data"]["token"]

# 获取用户列表
headers = {"Authorization": f"Bearer {token}"}
users_resp = requests.get(
    "http://localhost:8080/api/users?page=1&size=100",
    headers=headers
)

users = users_resp.json()["data"]["content"]

# 生成清单
print("=" * 80)
print("律所管理系统 - 账号清单")
print("=" * 80)
print(f"{'序号':<5}{'姓名':<15}{'账号':<15}{'密码':<10}{'角色':<20}{'状态':<10}")
print("-" * 80)

for i, user in enumerate(sorted(users, key=lambda x: x['id']), 1):
    if user['username'] == 'admin':
        print(f"{i:<5}{user.get('realName', ''):<15}{user['username']:<15}{'admin123':<10}{'系统管理员':<20}{'启用':<10}")
    else:
        # 从Excel数据中获取身份证后4位
        # 这里简化处理，实际应该从原始数据获取
        roles = ', '.join(user.get('roles', []))
        status = '启用' if user['status'] == 1 else '禁用'
        print(f"{i:<5}{user.get('realName', ''):<15}{user['username']:<15}{'身份证后4位':<10}{roles:<20}{status:<10}")

print("=" * 80)
print(f"总计: {len(users)} 个账号")
print("=" * 80)
print("\n管理员账号:")
print("  账号: admin")
print("  密码: admin123")
print("\n曾进朗账号:")
print("  账号: 曾进朗")
print("  密码: 6918 (身份证后4位)")
print("\n其他员工:")
print("  账号: 姓名")
print("  密码: 身份证号码后4位")
print("=" * 80)

# 保存到文件
with open(r"D:\ZGAI\账号清单.txt", "w", encoding="utf-8") as f:
    f.write("律所管理系统 - 账号清单\n")
    f.write("=" * 80 + "\n")
    f.write(f"{'序号':<5}{'姓名':<15}{'账号':<15}{'密码':<10}{'角色':<20}{'状态':<10}\n")
    f.write("-" * 80 + "\n")

    for i, user in enumerate(sorted(users, key=lambda x: x['id']), 1):
        if user['username'] == 'admin':
            f.write(f"{i:<5}{user.get('realName', ''):<15}{user['username']:<15}{'admin123':<10}{'系统管理员':<20}{'启用':<10}\n")
        else:
            roles = ', '.join(user.get('roles', []))
            status = '启用' if user['status'] == 1 else '禁用'
            f.write(f"{i:<5}{user.get('realName', ''):<15}{user['username']:<15}{'身份证后4位':<10}{roles:<20}{status:<10}\n")

    f.write("=" * 80 + "\n")
    f.write(f"总计: {len(users)} 个账号\n")
    f.write("\n管理员账号:\n")
    f.write("  账号: admin\n")
    f.write("  密码: admin123\n")
    f.write("\n曾进朗账号:\n")
    f.write("  账号: 曾进朗\n")
    f.write("  密码: 6918 (身份证后4位)\n")
    f.write("\n其他员工:\n")
    f.write("  账号: 姓名\n")
    f.write("  密码: 身份证号码后4位\n")

print("\n账号清单已保存到: D:\\ZGAI\\账号清单.txt")
