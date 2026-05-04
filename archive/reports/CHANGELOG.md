# 律所智能案件管理系统 - 版本历史

## [2.0.0] - 2026-04-19

### 🎉 重大里程碑 - 案件管理完整可用

本次更新完成了案件管理核心功能的全链路打通，系统已具备生产环境部署能力。

### ✨ 新增功能

#### 案件管理核心
- ✅ **案件列表页** - 支持多维度筛选、搜索、分页
- ✅ **案件详情页** - 5个Tab完整实现（基本案情/办案记录/受理单位/案件文档/案件动态）
- ✅ **归档库管理** - 已归档案件独立管理
- ✅ **回收站管理** - 软删除机制，支持恢复和永久删除
- ✅ **案件生命周期流转** - 正向/回退流转，自动创建待办，Timeline记录

#### 分页系统统一
- ✅ **后端分页标准化** - 统一使用PageRequest.of(page, size)，page从0开始
- ✅ **前端分页对齐** - request.js拦截器自动转换page-1
- ✅ **修复CaseService分页bug** - 移除重复的page-1操作

#### 用户体验优化
- ✅ **MainLayout通知系统修复** - 401错误修复，token正确携带
- ✅ **菜单路由修复** - 案件子菜单可正常跳转
- ✅ **API调用规范化** - 统一使用request实例，移除直接axios调用

### 🐛 Bug修复

#### 关键Bug
- **CaseService分页重复减1** - `request.getPage() - 1` 改为 `request.getPage()`
- **CaseQueryRequest默认值错误** - page默认值从1改为0
- **archive.vue/trash.vue分页bug** - 移除手动的page-1操作
- **MainLayout通知401错误** - fetch改为request.get，添加isLoggedIn检查
- **案件菜单无法跳转** - 子菜单index从`route.path + '/' + child.path`改为`child.path`

#### API修复
- **NotificationPanel** - 5处axios改为request，分页参数修复
- **document/index.vue** - axios未定义错误修复
- **notification.js** - API路径从复数改为单数(/notifications → /notification)

### 🔧 技术债务清理

- ✅ 删除测试文件 `frontend/index.test.html`
- ✅ 删除备份文件 `backend/src/main/resources/schema.sql.bak`
- ✅ 删除编译缓存中的备份文件 `backend/target/classes/schema.sql.bak`

### 📊 系统数据状态

```
案件: 6个（包括归档2个、回收站1个）
日程: 2条
知识库: 3篇
工作汇报: 3条
审批: 1条
卷宗: 1个
```

### 🏗️ 技术栈版本

- **后端**: Spring Boot 2.7.18 + Java 11
- **前端**: Vue 3 + Element Plus + Vite
- **数据库**: H2 (开发环境) / MySQL 8.0 (生产环境)
- **认证**: JWT (JJWT 0.11.5)
- **文件存储**: MinIO 8.5.7

### 🔐 访问地址

- **前端**: http://localhost:3017
- **后端**: http://localhost:8080/api
- **默认账号**: admin / admin123

### ⚠️ 重要提示

1. **AI功能配置** - AI文书生成功能需要配置API Key（目前使用默认配置）
2. **数据库切换** - 生产环境需将H2切换为MySQL
3. **文件存储** - MinIO需单独部署并配置连接信息

### 📝 已完成功能清单（参考PRD）

#### P0 核心功能 ✅
- 认证系统
- 工作台
- 案件管理（CRUD + 5Tab + 归档 + 回收站）
- 生命周期流转
- 日程待办
- 卷宗管理
- AI OCR
- 系统管理

#### P1 重要功能 ✅
- 客户管理
- 财务管理
- 审批管理（6种审批类型）
- AI文书生成
- AI问答（RAG知识库）
- 行政OA
- 一键归档（PDF生成）
- 统计报表
- 移动端适配

#### P2 增强功能 ✅
- RAG知识库
- 公文流转
- 类案检索
- 工具集（3个计算器）
- 知识库
- 工作汇报

---

## [1.0.0] - 2026-04-17

### 🎯 初始版本

- 基础框架搭建
- 用户认证与授权
- 案件CRUD
- 基本UI布局

---

## 版本号说明

格式：**主版本号.次版本号.修订号**

- **主版本号** - 重大功能更新或架构调整
- **次版本号** - 新增功能或重要改进
- **修订号** - Bug修复和小优化
