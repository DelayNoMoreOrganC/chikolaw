# 律所管理系统

**版本**：v2.0.3
**更新时间**：2026-05-04
**状态**：🟢 生产就绪

## 快速开始

### 环境要求
- Java 11+
- Node.js 16+
- Maven 3.6+

### 一键启动

**后端**
```bash
cd backend
mvn spring-boot:run
```
访问：http://localhost:8080（admin/admin123）

**前端**
```bash
cd frontend
npm install
npm run dev
```
访问：http://localhost:3017

## 功能模块

### 核心功能
- ✅ **案件管理** - 立案、审批、归档全流程管理
- ✅ **客户管理** - 客户库、利益冲突审查
- ✅ **日历管理** - 开庭提醒、重要日期管理
- ✅ **文档管理** - 案件文档上传下载
- ✅ **AI助手** - Ollama集成、RAG知识库
- ✅ **统计报表** - 案件类型、工作量、收费统计
- ✅ **权限管理** - RBAC角色权限、部门隔离

### 最新优化（v2.0.3）
- 新增业务类型字段（根据案件类型动态变化）
- 新增犯罪嫌疑人字段（刑事案件专用）
- 新增涉案标的字段（风险代理必填）
- 新增主办/协办部门字段（自动关联）
- 前端验证规则增强（动态验证、日期逻辑验证）
- 后端自定义验证器（@PercentageSum）

## 技术架构

### 后端
- Spring Boot 2.7.18
- Spring Security + JWT
- Spring Data JPA
- H2 Database

### 前端
- Vue 3.2+
- Element Plus 2.3+
- Vite 4.3+
- Pinia 2.1+

## 文档

- **当前状态**：见 `CURRENT_STATUS.md`
- **使用指南**：见 `用户使用指南.md`
- **开发文档**：见 `DEV_INSTRUCTIONS.md`
- **API文档**：http://localhost:8080/swagger-ui.html

## 项目结构

```
├── backend/           # Spring Boot后端
│   ├── src/main/java/com/lawfirm/
│   │   ├── controller/    # REST API
│   │   ├── service/       # 业务逻辑
│   │   ├── repository/    # JPA数据访问
│   │   ├── entity/        # 实体类
│   │   ├── dto/           # 数据传输对象
│   │   ├── vo/            # 视图对象
│   │   ├── security/      # JWT权限
│   │   └── validation/    # 自定义验证器
│   └── data/              # H2数据库文件
├── frontend/          # Vue 3前端
│   ├── src/
│   │   ├── api/           # API封装
│   │   ├── views/         # 页面组件
│   │   ├── components/    # 公共组件
│   │   ├── router/        # 路由配置
│   │   └── stores/        # 状态管理
│   └── dist/              # 构建输出
├── archive/           # 历史报告归档
├── CURRENT_STATUS.md  # 当前系统状态
└── README.md          # 本文件
```

## 常见问题

### 数据库连接失败
检查端口8080是否被占用，H2数据库默认使用内存模式

### 前端无法访问后端
检查后端是否启动，CORS配置是否正确

### AI功能无法使用
需要安装Ollama并下载模型，见相关配置文档

## 贡献

见 `DEV_INSTRUCTIONS.md`

## 许可

内部使用
