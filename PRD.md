# 律所智能案件管理系统 PRD v2.1

> **产品代号**：ZGAI LawOS（对内仓库 `D:\ZGAI`）  
> **唯一需求基准**：产品范围、模块规格、数据模型、**视觉与交互规范**以本文档为准。  
> **实现进度**：见附录 A「实现状态矩阵」；设计落地见附录 B；运行时快照见 [`CURRENT_STATUS.md`](CURRENT_STATUS.md)。  
> **历史进度文档**（如 `PRD功能清单.md`、`PRD开发任务列表.md`）已归档至 [`archive/reports/`](archive/reports/)，不得与本文档结论冲突。

### 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v2.1 | 2026-05-26 | 融合 **Alpha 律师工作系统** + **案件云** 产品范式；新增 §2.2 macOS 风蓝灰科技视觉规范；附录 B 对标矩阵；附录 A 同步真实差距（部分/MVP） |
| v2.0 | 2026-05-25 | 对齐当前实现：环境分层技术栈、17 项导航与 P2 扩展模块、AI 模型路由与可观测性、角色/审批枚举统一、实现状态矩阵附录 |
| v1.0 | 2026-04-17 | 初版：案件云分析 + 四方方案整合 |

---

## 一、产品概述

### 1.1 产品定位

面向中小规模律所（约 **60 人**）的**私有化智能案件操作系统**，在一条产品线上融合两类行业标杆能力：

| 对标产品 | 借鉴能力 | 本系统落点 |
|----------|----------|------------|
| **Alpha 律师工作系统** | 专业蓝白视觉、工作台信息密度、法律 AI 入口聚合、可信感与品牌气质 | 顶栏 AI 助手、AI 智能中心、文书识别/生成、蓝灰科技 UI（§2.2） |
| **案件云** | 案件进度可视化、工作台日历/待办一体、审限与开庭 chroma 标签、收案与卷宗效率 | Dashboard 五色日历、待办逾期规则、卷宗智能录入、生命周期进度条、类案检索 |

**核心差异化**：以 **AI 文书智能识别（IDP）+ 局域网可部署 LLM 路由** 为引擎，实现录入自动化、卷宗标准化、审批与行政 OA 一体化；数据不出所（私有化 H2/MySQL + MinIO）。

**视觉定位（v2.1）**：**macOS 风格交互骨架** + **蓝灰科技色调**（冷静、专业、低饱和），替代 v1.0 高饱和紫渐变为主视觉的方案（见 §2.2 迁移说明）。

### 1.2 目标用户与角色

60 人律所，**6 种预置角色**（与后端 `RoleCode`、§11 系统管理一致）：

| 角色编码 | 名称 | 职责 | 典型操作 |
|----------|------|------|---------|
| ADMIN | 系统管理员 | 系统运维 | 用户/角色/配置/备份、全量数据 |
| DIRECTOR | 主任 | 律所管理 | 查看全部案件与统计、审批终审 |
| LAWYER_MAIN | 主办律师 | 案件负责人 | 创建/管理案件、发起审批、团队配置 |
| LAWYER_ASSIST | 协办律师 | 案件协办 | 编辑参与案件、上传文档、记录工时 |
| ASSISTANT | 律师助理 | 基础支持 | 卷宗上传、归档、基础信息录入 |
| FINANCE | 财务 | 收支管理 | 费用/收款/开票、财务统计 |

> 说明：`MANAGER` 与 `DIRECTOR` 在代码中为同义别名；`GUEST` 为受限访客，非核心业务角色。

### 1.3 部署方式

私有化部署（本地服务器或私有云），数据完全自主可控。典型场景：**律所局域网 50 并发用户**（见 [`docs/ARCHITECTURE_LAN_50_USERS.md`](docs/ARCHITECTURE_LAN_50_USERS.md)）。

### 1.4 产品名称与对外表述

| 场景 | 推荐文案 |
|------|----------|
| 登录页 / 浏览器标题 | 律所智能案件管理系统 |
| 侧栏 Logo | 律所管理系统（可配置为律所简称） |
| 对内文档 | ZGAI LawOS / 本 PRD |
| 与 Alpha、案件云对比材料 | 「融合 Alpha 专业 AI 入口与案件云进度可视化的一体化方案」 |

---

## 二、全局设计规范

### 2.1 技术栈（环境分层）

| 层级 | 开发环境（默认） | 生产环境 |
|------|------------------|----------|
| 前端 | Vue 3 + Element Plus + Vite | 同左，静态资源由 Nginx 等托管 |
| 后端 | Spring Boot 3（Java 11+） | 同左 |
| 数据库 | **H2 文件库**（`jdbc:h2:file:./data/lawfirm`，MySQL 兼容模式） | **MySQL 8.0**（`pom.xml` 已预留驱动，切换 `spring.datasource`） |
| 缓存 | **默认禁用** Redis 自动配置；登录锁定等用内存 `LoginAttemptCache` | **Redis 可选**（会话/分布式锁等按需启用） |
| 文件存储 | **MinIO + 本地 fallback**（`minio.*` + `file.upload-path`） | 同左，建议 MinIO 集群 |
| AI 接入 | **多 Provider**：DeepSeek、通义千问、**LM Studio**（OpenAI 兼容）、历史 Ollama 配置 | 同左，密钥经环境变量注入 |
| AI 路由 | `llm.routing.*` 按场景指定首选 Provider；`llm.fallback` 云端降级 | 同左 |
| 向量检索 | **Qdrant**（`application.yml` qdrant 段，可选）；RAG MVP 亦支持 TF-IDF | 生产建议启用 Qdrant |
| OCR | **双路径**（见 §4.1）：① `AIDocumentService` — DeepSeek Vision / Tesseract；② `OcrService` — 模拟数据（遗留，待合并或标注废弃） | 生产以 Vision + LLM 提取为准 |
| 本地缓存 | Caffeine（业务热点） | 同左 |

**不再作为唯一方案的技术（v1.0 遗留表述）**：PaddleOCR 未接入；Redis/MySQL 在 PRD 中改为「按环境选型」而非写死。

### 2.2 视觉与交互规范（macOS 风 · 蓝灰科技）

> **原则**：像 macOS 一样「轻窗口、重内容」——大圆角、毛玻璃分层、柔和阴影、短动效；像 Alpha/案件云 一样「法律专业」——蓝灰主色、高可读、日历 chroma 语义色保留。

#### 2.2.1 设计 Token（CSS 变量，目标态）

前端应在 `frontend/src/styles/theme-lawos.scss`（待建或并入 `main.scss`）统一维护，Element Plus 通过 `--el-color-primary` 等覆盖：

| Token | 值 | 用途 |
|-------|-----|------|
| `--lawos-bg` | `#F2F4F8` | 页面底色（macOS 浅灰桌面感） |
| `--lawos-surface` | `rgba(255,255,255,0.78)` | 卡片/侧栏面板（配合 `backdrop-filter: blur(20px)`） |
| `--lawos-surface-solid` | `#FFFFFF` | 表格、表单区实底 |
| `--lawos-primary` | `#3B6FD9` | 主操作、链接、选中（科技蓝，替代 `#409EFF` 纯 Element 默认） |
| `--lawos-primary-hover` | `#2F5BB5` | 主按钮悬停 |
| `--lawos-accent` | `#5B8DEF` | 次级强调、图标高亮 |
| `--lawos-text` | `#1C1C1E` | 主文字（近 SF 黑） |
| `--lawos-text-secondary` | `#6B7280` | 次要说明 |
| `--lawos-border` | `rgba(15,23,42,0.08)` | 分割线、卡片描边 |
| `--lawos-radius-lg` | `12px` | 卡片、对话框 |
| `--lawos-radius-md` | `8px` | 按钮、输入框 |
| `--lawos-shadow-sm` | `0 1px 2px rgba(15,23,42,0.06), 0 4px 12px rgba(15,23,42,0.04)` | 悬浮卡片 |
| `--lawos-font` | `-apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", "Microsoft YaHei", sans-serif` | 全局字体 |

**案件云日历语义色（保留，勿改为纯蓝灰）**：

| 类型 | 色值 | 含义 |
|------|------|------|
| 开庭/听证 | `#E5484D` | 最高优先级 |
| 审限届满 | `#F76B15` | 紧急 |
| 立案 | `#3B6FD9` | 程序节点 |
| 调解/和解 | `#30A46C` | 积极结果 |
| 举证截止 | `#8B5CF6` | 提醒类 |

#### 2.2.2 macOS 风格组件约定

| 区域 | 规范 | 实现参考 |
|------|------|----------|
| 侧栏 | 窄轨 64px / 展开 220px；实色或轻毛玻璃；选中项 **左侧 3px 主色条** + 浅蓝底 `#EEF3FC` | `MainLayout.vue` |
| 顶栏 | 高度 52–56px；底部分割线 `--lawos-border`；搜索框 **圆角胶囊**、浅灰底 | `MainLayout.vue` |
| 内容区 | 最大宽度建议 1440px 居中；页面背景 `--lawos-bg`；卡片 `--lawos-surface-solid` + `--lawos-shadow-sm` | 各 `views/*` |
| 路由切换 | **淡入 + 8px 位移**，约 240ms，`ease-out`；列表页 `keep-alive` | `route-transition.scss`、`RouteContent.vue` |
| 对话框 | 圆角 12px；标题区与内容区分隔；避免过重渐变头图 | Element `el-dialog` 全局 class |
| 统计卡 | **扁平蓝灰图标底** + 细边框，禁止大面积紫粉渐变（Alpha 早期风格仅作参考） | `dashboard/index.vue` 待按 token 收敛 |
| 按钮 | 主按钮实色 `--lawos-primary`；次按钮描边灰；危险操作用 Element danger | 全局 |

#### 2.2.3 与 Alpha / 案件云的 UI 对齐清单

| 能力 | Alpha 倾向 | 案件云倾向 | v2.1 要求 |
|------|------------|------------|-----------|
| 工作台首屏 | 欢迎区 + 关键 KPI | 日历+待办并列 | 保留双栏；欢迎条改为 **浅蓝灰横幅**（非紫渐变） |
| 搜索 | 顶部全局入口 | 案件多维检索 | debounce 300ms → `/search` 独立页 |
| 案件详情 | 信息分区卡片 | 阶段进度条 | 5 Tab + 顶部进度条；卡片统一圆角与间距 |
| AI 入口 | 独立能力岛 | — | 顶栏助手 + `/ai-hub`；文案「文书智能识别」 |
| 列表密度 | 中等 | 偏高 | 表格行高 44–48px；斑马纹可选 |

#### 2.2.4 迁移说明（v2.0 → v2.1 视觉）

- **废弃为主视觉**：`linear-gradient(135deg, #667eea, #764ba2)` 大面积铺底（历史 Alpha 对齐稿）。  
- **保留**：案件云五色日历、待办逾期红/橙规则、统计卡趋势箭头与点击跳转。  
- **已落地**：路由软过渡 `route-soft`、通知中心、文档/Office 预览统一组件。  
- **待收敛**（见附录 A.4）：全站 Element 主色改 token、Dashboard 欢迎横幅、登录页背景。

### 2.3 整体布局

```
┌──────────────────────────────────────────────────────────────┐
│ 顶栏（macOS 工具条感）                                        │
│ [≡] 面包屑 / 页面标题    [🔍 胶囊搜索]  [AI] [🔔] [头像]      │
├────────┬─────────────────────────────────────────────────────┤
│ 侧栏   │  内容区（#F2F4F8 底，白卡片浮动）                    │
│ 毛玻璃 │                                                     │
│ 或实白 │   PageHeader + 业务卡片 / 表格 / 图表                │
│        │                                                     │
│ 工作台 │   （路由切换：240ms 淡入，见 §2.2.2）                 │
│ 日程   │                                                     │
│ 案件 ▾ │                                                     │
│ …      │                                                     │
└────────┴─────────────────────────────────────────────────────┘
```

### 2.4 核心导航（10 个一级模块）

v1.0 定义的 P0/P1 核心业务模块，均已实现：

| 序号 | 菜单 | 路由 | 说明 |
|------|------|------|------|
| 1 | 工作台 | /dashboard | 统计卡片、日历、待办、快捷入口 |
| 2 | 日程 | /calendar | 月/周/日视图、待办、审限提醒 |
| 3 | 案件 | /case | 含子菜单：列表、新建、**批量收案**、归档、回收站 |
| 4 | 客户 | /client | 列表、新建 |
| 5 | 文档中心 | /document | 所级文档库 |
| 6 | 财务 | /finance | 费用/收款/开票 |
| 7 | 审批 | /approval | 流程审批 |
| 8 | 行政 | /admin-oa | 通知、会议室等（办公用品/固定资产见 §2.5） |
| 9 | 统计 | /statistics | ECharts 报表 |
| 10 | 设置 | /settings | 用户/角色/审计/配置/备份 |

### 2.5 扩展模块（P2，已纳入产品范围）

以下模块已在 [`frontend/src/router/index.js`](frontend/src/router/index.js) 与 [`MainLayout.vue`](frontend/src/layouts/MainLayout.vue) 侧栏上线（共 **17** 个一级入口，含子菜单展开项）：

| 模块 | 路由 | 说明 |
|------|------|------|
| 全局搜索页 | /search | 顶部栏 debounce 300ms 后跳转独立页（非下拉浮层） |
| 批量收案 | /case/batch-import | Excel/模板批量导入案件（v1.0 未定义，v2.0 正式入册） |
| 知识库 | /knowledge/list、/knowledge/create | 文章 CRUD |
| RAG 问答 | /knowledge/rag | 知识库检索 + LLM（TF-IDF MVP，可选 Qdrant 向量） |
| 法律检索 | /legal-search | 法规/类案检索（含类案 Tab 内嵌检索） |
| AI 智能中心 | /ai-hub | OCR、文书生成、问答入口聚合 |
| 类案检索 | /case-search | 文本相似度 MVP，目标态为向量/语义检索 |
| 工具集 | /tools | 诉讼费/利息/时效等计算器 |
| 债权精算 (AC) | /ac-calculator | 外部 iframe（如 localhost:8501） |
| SSB 省时宝 | /ssb-time-saver | 外部工具集成，多子功能 |
| 工作汇报 | /work-reports | 汇报记录与模板 |
| 公文流转 | /document-flow | **基于审批 API 的公文视图**，非独立后端域 |
| 办公用品 | /office-supplies | 从行政 OA 拆出的独立菜单 |
| 固定资产 | /fixed-assets | 从行政 OA 拆出的独立菜单 |

> **实现说明**：侧栏 `menuRoutes` 与 `router` 由前端硬编码维护，信息架构以本表为准；不要求 PRD 描述代码耦合细节。

**案件子路由补充（§2 模块2）**：

| 路由 | 页面 |
|------|------|
| /case/batch-import | 批量收案 |

---

## 三、模块详细需求

### 模块1：工作台（Dashboard）

**路由：** `/dashboard`  
**权限：** 所有角色可见，数据按权限隔离  
**对标：** 案件云「工作台 + 日历 + 待办」一体；Alpha「首屏 KPI + 快捷入口」

> **视觉（v2.1）**：页面遵循 §2.2 蓝灰底 + 白卡片；日历保留案件云五色标签；统计区采用扁平科技蓝图标，避免高饱和渐变横幅。

#### 1.1 页面结构

```
┌──────────────────────────────────────────────────┐
│  统计卡片区（5个）                                  │
│  [本月案件数] [进行中] [本月开庭] [待办数] [本月收费] │
├───────────────────────────┬──────────────────────┤
│   日历视图区               │   待办事项区           │
│   月/周切换                │   按紧急程度排序        │
│   彩色标签标注开庭/审限等    │   3天内标红            │
│   点击标签弹出案件摘要      │   7天内标橙            │
│                           │   逾期置顶             │
├───────────────────────────┴──────────────────────┤
│  快捷入口：[新建案件] [新建客户] [AI助手] [上传文书]  │
└──────────────────────────────────────────────────┘
```

#### 1.2 统计卡片

| 卡片 | 数据源 | 计算规则 |
|------|--------|---------|
| 本月案件数 | Case表 | WHERE created_at BETWEEN 本月1日 AND 本月末 |
| 进行中案件 | Case表 | WHERE status = 'active' |
| 本月开庭 | Calendar表 | WHERE type = 'hearing' AND date BETWEEN 本月范围 |
| 待办数 | Todo表 | WHERE status != 'completed' AND assignee = 当前用户 |
| 本月收费 | Finance表 | WHERE type = 'income' AND date BETWEEN 本月范围 |

#### 1.3 日历视图

- 默认月视图，支持切换周视图
- 日历节点标签颜色规则：
  - 🔴 开庭/听证 → 红色
  - 🟠 审限届满 → 橙色
  - 🔵 立案 → 蓝色
  - 🟢 调解/和解 → 绿色
  - 🟣 举证截止 → 紫色
- 点击标签弹出浮层（案件名称、案号、当事人、时间地点、主办律师、跳转按钮）
- 多维筛选器：案件类型 / 状态 / 主办律师 / 法院

#### 1.4 待办事项列表

接口：GET /api/todos?assignee={userId}&status=pending&sort=urgency

| 字段 | 说明 |
|------|------|
| 待办标题 | 如"3天后开庭 - 张三诉李四案" |
| 截止时间 | YYYY-MM-DD HH:mm |
| 优先级 | 🔴紧急 🟡重要 🟢普通 |
| 关联案件 | 案件名称，可点击跳转 |
| 状态标记 | 待处理/进行中/已完成/已逾期 |

高亮规则：逾期→红色背景+置顶 / 3天内→红色文字 / 7天内→橙色文字

#### 1.5 全局快捷搜索

位置：顶部栏居中  
接口：`GET /api/search?q={keyword}&type=all`  
搜索范围：案件名称、案号、当事人姓名、手机号、客户名  
交互：输入即搜索（debounce 300ms），**跳转 `/search` 独立结果页**（对标 Alpha 顶栏搜索；**不实现** v1.0 下拉浮层，见附录 A）

#### 1.6 工作台增强（v2.0+ 已交付，v2.1 视觉待收敛）

| 能力 | 状态 | 说明 |
|------|------|------|
| 统计卡片趋势/跳转/刷新 | 已实现 | 对标案件云 KPI；5 分钟刷新 + 可见页轮询 |
| 日历月/周 + 五色标签 | 已实现 | 对标案件云 chroma 规则（§2.2.1） |
| 待办逾期置顶与 chroma 字色 | 已实现 | 3 天红 / 7 天橙 |
| 卷宗智能录入 | 已实现 | `CaseFileIntakePanel` + 立案审批桥接 |
| 日历多维筛选 | 已实现 | 类型/状态/主办/法院 |
| 快捷操作四宫格 | 已实现 | 新建案件/客户、AI 助手、上传文书 |
| macOS 风欢迎区与全站 Token | **部分** | `theme-lawos.scss` 已接入；Dashboard 欢迎条已收敛，见附录 A.4 |

---

### 模块2：案件管理（核心）

**路由：** `/case`

子路由：

| 路由 | 页面 | 说明 |
|------|------|------|
| /case/list | 案件列表 | 列表+看板视图 |
| /case/create | 新建案件 | 分步表单 |
| /case/:id | 案件详情 | 5个Tab |
| /case/:id/basic | 基本案情 | 案件信息+当事人+费用 |
| /case/:id/record | 办案记录 | 阶段+工时+文档 |
| /case/:id/unit | 受理单位 | 保全+执行+庭审+承办人 |
| /case/:id/doc | 案件文档 | 卷宗管理+AI识别 |
| /case/:id/timeline | 案件动态 | 操作日志+评论 |
| /case/archive | 归档库 | 已归档案件 |
| /case/trash | 回收站 | 已删除案件 |
| /case/batch-import | 批量收案 | Excel/模板导入、校验与结果反馈 |

#### 2.1 案件列表页 (/case/list)

**筛选条件：**

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 案件类型 | 下拉单选 | 民事/商事/仲裁/刑事/行政/非诉（实现含扩展类型如 FINANCIAL_NPA 等，见附录） |
| 案件状态 | 下拉单选 | 待立案/审理中/结案/归档 |
| 案件等级 | 下拉单选 | 重要/一般/次要 |
| 主办律师 | 人员选择 | 系统用户列表 |
| 管辖法院 | 搜索下拉 | 法院库 |
| 时间范围 | 日期范围 | 立案时间/创建时间 |

**列表字段：**

| 列名 | 字段 | 宽度 | 排序 | 说明 |
|------|------|------|------|------|
| 等级 | level | 60px | ✅ | 🔴重要 🟡一般 ⚪次要 |
| 类型 | caseType | 80px | ✅ | 民事/商事/... |
| 案件名称 | caseName | 200px | ✅ | 可点击进入详情 |
| 案号 | caseNumber | 150px | ✅ | |
| 当事人 | parties | 150px | - | 原告 vs 被告 |
| 当前阶段 | currentStage | 100px | - | 进度条样式 |
| 主办律师 | ownerName | 80px | ✅ | |
| 管辖法院 | court | 120px | - | |
| 下次开庭 | nextHearing | 100px | ✅ | 日期+高亮 |
| 操作 | actions | 150px | - | 编辑/归档/删除 |

批量操作：结案 / 归档 / 修改主办 / 删除（需权限）
视图切换：列表视图 ↔ 看板视图（按阶段分列）

#### 2.2 新建案件表单 (/case/create)

##### A. 基本信息

| 字段名 | 字段标识 | 类型 | 必填 | 校验 | 说明 |
|--------|---------|------|------|------|------|
| 案件类型 | caseType | 下拉选择 | ✅ | - | 民事/商事/仲裁/刑事/行政/非诉（实现含扩展类型，见附录 A.1） |
| 案件程序 | procedure | 下拉选择 | ✅ | - | 一审/二审/再审/执行/其他 |
| 案件名称 | caseName | 文本输入 | ✅ | ≤100字 | 为空时根据当事人生成"原告 Vs 被告" |
| 案件编号 | caseNumber | 文本输入 | ❌ | - | 为空时自动生成：年份-类型-序号 |
| 案由 | caseReason | 可搜索下拉 | ✅ | - | 预置法律案由+自定义 |
| 管辖法院 | court | 搜索下拉 | ✅ | - | 全国法院库，模糊搜索 |
| 立案时间 | filingDate | 日期选择 | ❌ | - | |
| 审限时间 | deadlineDate | 日期选择 | ❌ | - | 录入后自动生成审限届满待办 |
| 委托时间 | commissionDate | 日期选择 | ❌ | - | |
| 案件标签 | tags | 多选标签 | ❌ | - | 自定义标签 |
| 案件简述 | summary | 多行文本 | ❌ | ≤500字 | |
| 案件等级 | level | 单选 | ✅ | - | 重要/一般/次要，默认一般 |
| 案件主办 | ownerId | 人员选择 | ✅ | - | 默认当前用户 |
| 协办律师 | coOwners | 人员多选 | ❌ | - | |
| 律师助理 | assistants | 人员多选 | ❌ | - | |

**AI智能填充按钮：** 上传法院文书 → Vision OCR（DeepSeek/Tesseract，见 §4.1）→ `LlmExtractService` 要素提取 → 人工校验 → 自动填入表单
**查重功能：** GET /api/cases/check-duplicate?name=xxx → 弹窗显示疑似重复案件

##### B. 当事人及关联方（动态多行）

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 类型 | 单选 | ✅ | 个人 / 单位 |
| 姓名/单位名称 | 文本/搜索 | ✅ | 可从客户库选择 |
| 委托方 | 开关 | ✅ | 是/否，默认否 |
| 属性 | 下拉 | ✅ | 原告/被告/第三人/共同原告/共同被告/申请人/被申请人 |
| 联系电话 | 文本 | ❌ | |
| 身份证号/信用代码 | 文本 | ❌ | |
| 性别 | 单选 | ❌ | 个人类型时显示 |
| 民族 | 下拉 | ❌ | 个人类型时显示 |
| 住址/地址 | 文本 | ❌ | |
| 法定代表人 | 文本 | ❌ | 单位类型时显示 |
| 代理律师 | 文本 | ❌ | 对方律师信息 |
| 备注 | 文本 | ❌ | |
| 同步创建客户 | 复选框 | ❌ | 勾选后自动在客户库创建 |

操作：[+ 添加当事人] / [复制] / [删除]

##### C. 代理律师费

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| 收费方式 | 多选 | ✅ | 定额/风险代理/计时/计件/免费 |
| 标的额(元) | 数字 | ❌ | |
| 标的物 | 文本 | ❌ | |
| 代理费(元) | 数字 | ✅ | |
| 收费简介 | 文本 | ❌ | ≤200字 |
| 收费备注 | 多行文本 | ❌ | ≤250字 |

##### D. 应收款信息（动态多行）

| 字段名 | 类型 | 必填 |
|--------|------|------|
| 款项名称 | 文本 | ✅ |
| 应收金额(元) | 数字 | ✅ |
| 约定收款日期 | 日期 | ✅ |
| 备注 | 文本 | ❌ |

##### E. 结案/归档信息（结案时填写）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| 结案状态 | 下拉 | 达成诉求/部分达成/未达成/未委托/终止/其他 |
| 结案日期 | 日期 | 为空=在办 |
| 归档日期 | 日期 | 为空=未归档 |
| 档案保管地 | 文本 | |

##### F. 关联信息

| 字段名 | 类型 | 说明 |
|--------|------|------|
| 关联客户 | 搜索选择 | 从客户库选择 |
| 关联案件 | 搜索多选 | 从案件库选择 |
| 关联项目 | 文本 | |
| 备注 | 多行文本 | |

**表单交互：**
1. 选择案件类型后自动加载对应流程模板
2. 当事人区域切换个人/单位时字段动态变化
3. 案件名称为空时自动拼接"原告名 Vs 被告名"
4. 案件编号为空时自动生成
5. 所有编辑自动留痕

#### 2.3 案件详情页

**顶部固定区域：**
```
[← 返回] 案件名称（案号）  [编辑] [归档] [更多▼]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 进度条: 咨询 → 签约 → 立案 → 一审 → 二审 → 执行 → 结案
          ✓      ✓     ✓    ★当前
```

##### Tab① 基本案情 (/case/:id/basic)

7个信息区块（卡片式布局）：
- 区块1-案件信息（类型/案由/法院/时间/编号/等级/胜诉金额/实际回款/简述）
- 区块2-当事人及关联方（Tab卡片，属性标签颜色区分，编辑/删除/添加）
- 区块3-办理人员（主办/协办/助理，编辑团队弹窗）
- 区块4-代理律师费（收费方式/标的额/代理费/收款记录汇总）
- 区块5-案件程序（每个程序一张卡片：名称/案号/立案日/开庭/裁决/结果/地区/法院/承办人/附件）
- 区块6-关联案件（表格：名称/程序/法院/案号/办理人）
- 区块7-办案策略（富文本列表+讨论区）

##### Tab② 办案记录 (/case/:id/record)

- 阶段选择器 + 进度统计（已完成/总任务）+ 总工时
- 日期范围筛选 + 关键字搜索
- 导出Word/Excel
- 时间线展示，每条记录：标题/内容/工时/附件/操作

**添加记录字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 记录标题 | 文本 | ✅ | |
| 记录内容 | 富文本 | ✅ | |
| 案件阶段 | 下拉 | ✅ | 当前案件阶段列表 |
| 工时(h) | 数字 | ❌ | 精确到0.5h |
| 附件 | 文件上传 | ❌ | 多文件 |
| 记录日期 | 日期 | ✅ | 默认今天 |

##### Tab③ 受理单位 (/case/:id/unit)

4个子Tab：
- 财产保全：列表（被申请人/标的/金额/法院/日期/状态）
- 执行情况：列表（执行案号/法院/申请人/被执行人/标的/日期/状态）
- 庭审记录：列表（日期/类型/法庭号/审判员/书记员/笔录/附件）
- 承办人员：列表（姓名/职位/电话/法庭/备注）

##### Tab④ 案件文档 (/case/:id/doc)

- 树形目录（起诉状/答辩状/原告证据/被告证据/法院文书/代理词/判决书/其他）
- 面包屑导航
- 文件列表（checkbox/文件名/大小/上传人/时间/操作）
- 功能：拖拽上传/分片上传/图片预览/标签/搜索/批量操作/新建文档；**版本号**（`versionNo` 同文件名递增）与 **PDF/图片在线预览**（`/preview` + 鉴权 blob）— **已实现 MVP**
- AI智能识别入口
- 一键归档PDF（首面+目录+拼接+书签）

##### Tab⑤ 案件动态 (/case/:id/timeline)

- 分类Tab：全部/动态/评论
- 时间线流（操作日志+评论）
- 评论功能（@提及+回复）

---

### 模块3：案件生命周期管理

#### 3.1 状态流转

标准流程节点（民事）：咨询→签约→起草文书→待立案→已立案→一审审理中→一审结案→(二审)→执行→结案归档

| 案件类型 | 流程节点 |
|---------|---------|
| 民事 | 咨询→签约→起草→立案→一审→(二审→)执行→结案 |
| 刑事 | 咨询→签约→会见→审查起诉→一审→(二审→)结案 |
| 行政 | 咨询→签约→起草→立案→一审→(二审→)结案 |
| 商事仲裁 | 咨询→签约→起草→申请仲裁→组庭→开庭→裁决→结案 |
| 非诉 | 咨询→签约→尽职调查→出具文书→交付→结案 |

状态变更规则：
1. 点击进度条节点→弹窗确认→更新
2. 变更后自动生成该阶段默认待办
3. 记录日志（时间/操作人/原状态/新状态）
4. 支持回退（需权限+填写原因）

**实现状态（v2.0）**：进度条与阶段变更 UI 已上线；6 类案件流程模板、回退权限与原因校验已实现，见附录。

---

### 模块4：AI 智能辅助

#### 4.1 OCR 与要素提取（双路径）

**生产路径（推荐）** — `AIDocumentService` + `LlmExtractService`：

1. 上传文书（图片/PDF；PDF 可分页渲染后调 Vision，见 `ai.ocr.pdf-vision-max-pages`）
2. OCR：`ai.ocr.provider` = `deepseek`（Vision）或 `tesseract`（本地文本层）
3. LLM 要素提取：场景 `EXTRACT` / `DOCUMENT_RECOGNITION_EXTRACT`（见 §4.5）
4. 前端展示 JSON → 人工校验 → 确认回填表单 → 原件归入案件文档

**遗留路径** — `POST /api/ai/ocr-upload`（`OcrService`）：返回**模拟数据**，仅用于联调/demo，与生产路径并存，计划合并或标注废弃。

**LLM 提取字段（与 v1.0 Prompt 一致）**：案号、法院、开庭时间/地点、法官、书记员、原被告、案由、联系电话、文书类型等，输出 JSON。

#### 4.2 AI 文书生成

| 文书类型 | 输入 | 输出 | 路由场景 |
|---------|------|------|----------|
| 起诉状 | 案件信息+当事人+案由+诉求 | Word 初稿（目标态） | `DOCUMENT` |
| 答辩状 | 案件信息+对方诉求+答辩意见 | Word 初稿（目标态） | `DOCUMENT` |
| 代理词 | 材料+庭审+争议焦点 | Word 框架（目标态） | `DOCUMENT` |
| 法律意见书 | 案件信息+客户需求 | Word 框架（目标态） | `DOCUMENT` |
| 律师函等 | 按 UI 别名 | 同左 | `LEGACY_DOCUMENT`（`DocGenerateService`） |

**当前实现（v2.1）**：LLM 输出纯文本，前端下载 `.txt` 预览对话框；**`.docx` 生成为 P1 待补齐**（对标 Alpha 文书交付物）。

术语：标准四类与前端展示名已对齐 — `frontend/src/config/ai-terminology.js`、`DocumentTypeAliasResolver`（起诉状/答辩状/代理词/法律意见书/律师函）。

#### 4.3 AI 法律知识问答

| 模式 | 实现 | 场景 |
|------|------|------|
| 通用法律问答 | `LegalChatService` | `LEGAL_CHAT` |
| 案件上下文问答 | `AiChatService` / case-chat API | `GENERAL_CHAT` |
| 知识库 RAG | `/knowledge/rag`，`RAGService` | `RAG`（TF-IDF MVP；可选 Qdrant 向量） |

对话可关联案件保存；类案检索、案件分析当前为**关键词/文本相似度 MVP**，目标态为语义检索（P2）。

#### 4.4 AI 使用日志

记录：userId、caseId、functionType、inputTokens、outputTokens、model、status、duration。  
接口：`GET /api/ai/logs`

#### 4.5 AI 模型路由

按业务场景选择首选 LLM Provider（`AIModelRoutingService` + `AIModelUseCase`）：

| 场景枚举 | 用途 | 配置键（`application.yml`） |
|----------|------|---------------------------|
| LEGAL_CHAT | 法律咨询 | `llm.routing.legal-chat` |
| RAG | 知识库问答 | `llm.routing.rag` |
| DOCUMENT | 文书生成 | `llm.routing.document` |
| GENERAL_CHAT | 通用/案件对话 | `llm.routing.general-chat` |
| EXTRACT | OCR 后要素提取 | `llm.routing.extract` |
| DOCUMENT_RECOGNITION_EXTRACT | 文档识别结构化抽取 | `llm.routing.document-recognition-extract` |
| LEGACY_DOCUMENT | 兼容旧版文书生成 | `llm.routing.legacy-document` |

Provider 类型示例：`deepseek`、`qwen`、`lmstudio`（OpenAI 兼容本地模型）。库表 `ai_config.provider_type` 可覆盖默认。

**降级策略**：`llm.fallback.enabled=true` 时，本地/LM Studio 失败后由 `LLMApiService` 按 `llm.fallback.provider` 重试云端（默认 deepseek），`llm.retry` 控制次数。

#### 4.6 AI 可观测性

管理员/主任可查看路由与最近调用摘要：

- 接口：`GET /api/ai/diagnostics`（`AiDiagnosticsController`）
- 内容：各场景解析后的 Provider、fallback 配置、`LlmRecentCallSnapshot` 最近调用列表
- 配置：`GET/PUT /api/ai/config`（含按场景 Provider）

---

### 模块5：日程管理

**路由：** `/calendar`

新建日程：标题/类型/开始结束时间/地点/关联案件/参与人员/提醒设置/重复
新建待办：标题/截止时间/优先级/关联案件/负责人/提醒/备注
逾期预警：审限前7天橙色/3天红色+通知/当天红色+推送/逾期置顶

---

### 模块6：客户管理

**路由：** `/client`

客户详情：基本信息/关联案件/沟通记录/收费统计/利益冲突检索

---

### 模块7：财务管理

**路由：** `/finance`

- 费用记录（诉讼费/保全费/鉴定费/公证费/差旅费/快递费/其他）
- 律师费管理（已收/待收）
- 收款记录
- 开票记录
- 案件收支统计

---

### 模块8：审批管理

**路由：** `/approval`

#### 8.1 标准审批类型（规范枚举）

与后端 `ApprovalService.getApprovalTypes()` 一致，**PRD 规范为 6 类**：

| 编码 | 中文名称 |
|------|----------|
| SEAL | 用印申请 |
| REIMBURSEMENT | 费用报销 |
| INVOICE | 开票申请 |
| LEAVE | 请假出差 |
| PURCHASE | 采购申请 |
| LICENSE | 证照借用 |

功能：发起 / 同意 / 驳回 / 转审 / 撤回 / 催办 / 进度查看 / 关联案件。

#### 8.2 实现差距（v2.0）

| 位置 | 类型选项 | 状态 |
|------|----------|------|
| 筛选器 `ApprovalFilter.vue` | 中文标签：用印、费用报销、开票、请假出差、采购、证照借用 | 与 PRD **一致** |
| 发起表单 `approval/index.vue` | SEAL、REIMBURSEMENT、INVOICE、LEAVE、PURCHASE、LICENSE、CASE_FILING 等 | **已对齐**（类型列表可由 `getApprovalTypes()` 动态加载） |
| 自定义流程配置 | PRD 要求 | **已实现**（设置 → 审批流程） |
| 转审 / 催办 | PRD 要求 | API 存在，前端深度待 E2E 验收 |

**注意**：公文流转页复用 `/api/approval`，勿与 `/api/approvals` 混用。

---

### 模块9：行政OA

**路由：** `/admin-oa`（办公用品 `/office-supplies`、固定资产 `/fixed-assets` 已拆为独立菜单，见 §2.5）

- 通知公告（发布/范围/已读未读统计）— **已实现**
- 会议室管理（预约/冲突校验/与开庭联动）— **已实现**
- 考勤管理 — 后端 `AttendanceController` + 行政 OA「考勤管理」Tab（`AttendancePanel`）— **已实现**
- 办公用品 / 固定资产 — **已实现**（独立路由）

> **知识库**已升格为独立模块 `/knowledge`，不再归属行政 OA 章节（见 §2.5）。

---

### 模块10：数据统计与报表

**路由：** `/statistics`

- 案件统计（数量趋势/类型分布/胜诉率）
- 收费统计（收入趋势/收款率/利润率）
- 律师业绩（案件数/收费/结案率排名）
- 可视化图表（ECharts，导出Excel/PDF）

---

### 模块11：系统管理

**路由：** `/settings`（敏感操作后端 `@PreAuthorize`；前端路由 **未** 统一 `meta.roles` 限制，依赖接口鉴权）

- 用户管理（CRUD/启用禁用/重置密码）
- 角色权限 RBAC（**6 种预置角色**，见 §1.2；支持自定义角色）
- 数据权限隔离（律师看自己/主任看全部）
- 操作审计日志（AOP切面自动记录）
- 系统配置（案件类型/案由库/法院库/流程模板/提醒阈值/AI模型）
- 数据备份（每日自动/手动/180天保留/恢复）

---

## 四、数据模型总览

### 核心实体

| 实体 | 字段数 | 说明 |
|------|--------|------|
| User | 12 | 用户 |
| Role | 4 | 角色 |
| Department | 5 | 部门 |
| Case | 28 | 案件 |
| CaseProcedure | 12 | 案件程序 |
| CaseStage | 6 | 案件阶段 |
| CaseMember | 5 | 案件团队成员 |
| Party | 14 | 当事人 |
| CaseRecord | 9 | 办案记录 |
| CaseDocument | 10 | 案件文档 |
| CaseTimeline | 8 | 案件动态/评论 |
| Calendar | 10 | 日程 |
| Todo | 9 | 待办 |
| Client | 14 | 客户 |
| CommunicationRecord | 7 | 沟通记录 |
| FinanceRecord | 10 | 财务记录 |
| Invoice | 8 | 开票记录 |
| Payment | 8 | 收款记录 |
| Approval | 10 | 审批单 |
| Announcement | 9 | 公告 |
| MeetingRoom | 7 | 会议室 |
| MeetingBooking | 8 | 会议预约 |
| AILog | 9 | AI使用日志 |
| AuditLog | 8 | 操作审计日志 |
| SystemConfig | 5 | 系统配置 |

---

## 五、API 接口规划

### 5.1 认证
POST /api/auth/login | POST /api/auth/logout | POST /api/auth/change-password | GET /api/auth/current-user

### 5.2 案件
GET/POST /api/cases | GET/PUT/DELETE /api/cases/:id | PUT /api/cases/:id/status | GET /api/cases/:id/status-history | PUT /api/cases/:id/archive | GET /api/cases/check-duplicate | CRUD /api/cases/:id/parties | CRUD /api/cases/:id/procedures | CRUD /api/cases/:id/records | CRUD /api/cases/:id/timeline | POST /api/cases/:id/archive-pdf

### 5.3 日程待办
CRUD /api/calendar | CRUD /api/todos

### 5.4 AI
POST /api/ai/ocr-upload（遗留模拟） | POST /api/ai/extract | POST /api/ai/auto-fill/:caseId | POST /api/ai/generate-doc | POST /api/ai/chat | POST /api/ai/case-chat/:caseId | GET /api/ai/logs | GET/PUT /api/ai/config | **GET /api/ai/diagnostics**

### 5.4.1 批量案件
POST /api/cases/batch-import（批量收案，与 `/case/batch-import` 页面对应）

### 5.4.2 考勤
CRUD /api/attendance/*（`AttendanceController`）

### 5.5 客户
CRUD /api/clients | GET /api/clients/:id/cases | CRUD /api/clients/:id/communications | GET /api/clients/:id/conflict-check

### 5.6 财务
CRUD /api/finance/expenses | GET /api/finance/fees | CRUD /api/finance/payments | CRUD /api/finance/invoices | GET /api/finance/summary/:caseId | GET /api/finance/dashboard

### 5.7 审批
CRUD /api/approval | PUT /api/approval/:id/approve|reject|transfer|withdraw | POST /api/approval/:id/urge | GET /api/approval/types

### 5.7.1 卷宗录入（工作台核心）
POST /api/case-intake/process | POST /api/case-intake/attach | POST /api/case-intake/attach-pending | POST /api/case-intake/filing-application | GET /api/case-intake/pending | GET /api/case-intake/pending/:id/prefill

### 5.8 系统管理
CRUD /api/users | CRUD /api/roles | GET /api/audit-logs | GET/PUT /api/system/config | POST /api/system/backup

### 5.9 其他
GET /api/search | GET /api/dashboard/stats | CRUD /api/notifications | CRUD /api/documents

---

## 六、非功能需求

> v2.0 实现差距摘要见附录 A.3。

### 6.1 性能
- 页面加载 ≤2秒
- API响应 ≤500ms（普通）/ ≤3秒（AI）
- 文件上传 ≤50MB，支持断点续传
- 支持60-70人同时使用

### 6.2 安全
- 密码BCrypt加密，复杂度≥8位
- 5次登录失败锁定30分钟
- HTTPS传输
- 敏感字段加密存储
- 全量操作审计日志
- 每日自动备份，保留≥180天

### 6.3 兼容性
Chrome≥90 / Edge≥90 / Firefox≥90 / Safari≥14 / 移动端响应式适配

---

## 七、开发优先级

### P0 核心功能（约1.5个月）
认证系统 / 工作台 / 案件管理(CRUD+详情5Tab) / 生命周期流转 / 日程待办 / 卷宗管理 / AI OCR / 系统管理

### P1 重要功能（约4个月）
客户管理 / 财务管理 / 审批管理 / AI文书生成+问答 / 行政OA / 一键归档 / 统计报表 / 移动端适配

### P2 扩展模块（已上线，规格见 §2.5）

RAG 知识库 / 法律检索 / AI 智能中心 / 类案检索 / 工具集 + AC 精算 + SSB / 工作汇报 / 公文流转（审批视图）/ 批量收案 — 质量分级见附录「实现状态矩阵」。

---

## 附录 A：实现状态矩阵

> 替代原 `PRD功能清单.md` 的「100% 完成」声明。状态：**已实现** / **部分** / **未实现** / **超出 PRD**。

### A.1 核心模块（P0/P1）

| 模块 | 项 | 状态 | 备注 |
|------|-----|------|------|
| 认证 | JWT、5 次锁定 30 分钟 | 已实现 | 与 `application.yml` login 段一致 |
| 工作台 | 5 统计卡片 | 超出 | 趋势%、跳转、自动刷新 |
| 工作台 | 日历五色/筛选 | 已实现 | 五色 tag + 类型/案件类型/状态/主办/法院筛选 |
| 工作台 | 全局搜索 | 已实现 | 顶栏 debounce 300ms → `/search` 独立页 |
| 案件 | CRUD、5 Tab、看板、批量操作 | 已实现 | |
| 案件 | 批量收案 | 超出 | `/case/batch-import` |
| 案件 | 受理单位 4 子 Tab | 已实现 | |
| 案件 | 文档版本/PDF 预览 | 已实现 | `versionNo` + `/preview` MVP |
| 案件 | 卷宗智能录入+立案审批桥接 | 已实现 | 工作台录入；`CASE_FILING` 审批通过后预填建案并挂接暂存卷宗 |
| 案件 | 类型扩展枚举 | 超出 | 多于 PRD 6 类 |
| 生命周期 | 进度条流转 | 已实现 | 6 类案件模板与后端对齐；回退需原因+负责人/主任权限 |
| 日程 | 月/周/日视图 | 超出 | PRD 原仅月/周 |
| 日程 | 审限推送统一管道 | 已实现 | 待办+通知中心 DEADLINE/CASE_DEADLINE |
| 客户 | CRUD、利益冲突 | 已实现 | 建案前 PartyDTO 映射+冲突审查；未通过/冲突阻断提交；豁免走审批中心 OTHER |
| 财务 | 多 Tab UI | 已实现 | 费用/收款/开票/律师费 Tab + PageResult |
| 审批 | 6 类模板（后端） | 已实现 | |
| 审批 | 创建表单类型 | 已实现 | 与后端枚举一致（含立案/公文/终止委托） |
| 审批 | 自定义流程 | 已实现 | `/approval/workflow` + 设置页配置 |
| 行政 | 公告、会议室、办公用品、固定资产 | 已实现 | |
| 行政 | 考勤 UI | 已实现 | 行政 OA「考勤管理」Tab + 申请/审批 |
| 统计 | ECharts、导出 | 已实现 | |
| 系统 | 用户/角色/审计/配置/备份 UI | 已实现 | |
| 系统 | 前端 /settings 路由守卫 | 已实现 | 校验 SYSTEM_CONFIG / USER_VIEW / ROLE_VIEW |

### A.2 AI 与 P2 扩展

| 模块 | 项 | 状态 | 备注 |
|------|-----|------|------|
| AI | 模型路由 7 场景 | 超出 | §4.5 |
| AI | diagnostics API | 超出 | §4.6 |
| AI | Vision OCR + 提取 | 已实现 | |
| AI | ocr-upload 模拟 | 已实现 | @Deprecated；统一 `/ai/documents/recognize` |
| AI | 文书生成四类 | 部分 | 流程已实现；输出为文本/txt，非 PRD Word 初稿 |
| 视觉 | macOS 蓝灰 Design Token | 部分 | §2.2 已定义；全站收敛见附录 A.4 |
| AI | RAG /legal-chat | 已实现 | LM Studio + fallback |
| AI | 类案/案件分析语义化 | 已实现 | 文本加权 + `case-search.semantic` Embedding；`GET /cases/{id}/ai-analysis` LLM 分析 |
| P2 | 知识库、RAG 页 | 已实现 | Qdrant 可选 |
| P2 | 类案检索 | 已实现 | `POST /case-search/similar`；前端 `/case-search` 对齐案由/类型/相似度 |
| P2 | 法律检索 | 已实现 | 法规库+AI问答+类案 Tab 内嵌检索 |
| P2 | 工具集、AC、SSB | 已实现 | iframe 集成 |
| P2 | 工作汇报 | 已实现 | `GET /work-reports?status=` + CRUD/提交/审核 API 模块 |
| P2 | 公文流转 | 已实现 | `OFFICIAL_DOC` 审批类型 + 流转记录 |
| P2 | 全局搜索页 | 超出 | 非下拉 |

### A.3 非功能需求

| 需求 | 状态 | 备注 |
|------|------|------|
| MySQL 生产库 | 已实现 | `application-prod.yml` + 环境变量 |
| Redis | 已实现 | prod profile 启用 Redis |
| HTTPS | 已实现 | `forward-headers-strategy` + 反向代理 |
| 移动端响应式 | 已实现 | MainLayout `isMobile` + 主要页适配 |
| 180 天备份 | 已实现 | 自动/手动备份 + `/system/restore` + 设置页 |

### A.4 已知差距（功能与体验，2026-05-26）

> 附录 A.1–A.3 标「已实现」的模块，下列为 **PRD 目标态 vs 代码现状** 的差异，供排期；不以归档文档「100%」为准。

| 类别 | 项 | 状态 | 说明 |
|------|-----|------|------|
| 体验 | 全站蓝灰 macOS Token | 部分 | §2.2.4；Token+登录+工作台已落地，其余子页待扫 |
| 日程 | 重复规则 / 详情抽屉 | 已实现 | `calendarPayload.js` + 抽屉 |
| 行政 | 公告/会议详情与取消 | 已实现 | 行政 OA 弹窗/抽屉 |
| 文档 | 分片/断点续传 | 部分 | 后端 `ChunkedUploadController`；前端未接 |
| 文档 | 办案记录富文本+附件持久化 | 部分 | 表单有附件 UI，上传管道不完整 |
| AI | 文书 `.docx` 导出 | 未实现 | 当前 txt |
| AI | `ocr-upload` 下线 | 部分 | 已 @Deprecated，待移除调用方 |
| AI | RAG/Qdrant 生产默认 | 部分 | TF-IDF MVP；Qdrant 可选 |
| 日程 | 重复规则 `repeat`↔`repeatRule` | 部分 | 字段映射待对齐 |
| 日程 | 事件详情页 | 部分 | 点击仅提示，无详情抽屉 |
| 通知 | WebSocket 实时推送 | 未实现 | 轮询 + 通知中心 |
| P2 | SSB 省时宝 | 未实现 | 占位页 |
| 行政 | 公告/会议详情与删除 | 部分 | admin-oa TODO |
| 非功能 | 50+ 人压测报告 | 部分 | 见架构文档，缺正式验收 |

---

## 附录 B：Alpha / 案件云 能力映射

| 用户场景 | 案件云典型能力 | Alpha 典型能力 | 本系统路由/API |
|----------|----------------|----------------|----------------|
| 早上打开系统看今天干什么 | 日历 + 待办 + 开庭红色 | 工作台 KPI | `/dashboard`、`/calendar` |
| 收案/录入 | 批量导入、模板 | 文书识别 | `/case/batch-import`、`CaseFileIntakePanel`、`/ai-hub` |
| 跟案件进度 | 阶段看板、列表筛选 | 案件分析 | `/case/list`、进度条、`/case-search` |
| 写文书 | — | 文书生成、模板 | `/ai-hub`、`AIDocGenerator`、`/api/ai/generate-doc` |
| 问法律问题 | — | 法律 AI 问答 | `/knowledge/rag`、`LegalChatService` |
| 所内协作 | 审批流 | — | `/approval`、`/document-flow`（`OFFICIAL_DOC`） |
| 卷宗与预览 | 文档库 | — | 案件 doc Tab、`/document`、`/preview-html` |

**差异化陈述（对外）**：在案件云的「进度与日程」骨架上，叠加 Alpha 级「AI 入口与文书能力」，并以 **macOS 风蓝灰 UI** 统一律所私有化场景体验。

---

*版本：v2.1 | 日期：2026-05-26*
