# 律所局域网部署架构（约 50 人并发）

## 目标

| 指标 | 目标 |
|------|------|
| 并发用户 | ~50 名律师/助理同时在线 |
| 部署形态 | 律所内网，不依赖公网 Office/CDN |
| 页面切换 | 列表/工作台类页面 < 300ms 感知（缓存后） |
| API P95 | 常规 CRUD < 500ms（不含 LLM/文书识别） |

## 分层架构

```
[浏览器] → [Nginx 静态 + 反向代理] → [Spring Boot ×1~2] → [MySQL 主库]
                                              ↓
                                        [Redis 可选]
                                        [本地磁盘 uploads/]
                                        [LLM 内网或专线]
```

**开发环境（当前）**：Vite(3017) + H2 文件库 + Caffeine 本地缓存 — 仅用于功能开发，**不代表生产性能**。

**生产环境（推荐）**：`spring.profiles.active=prod` + MySQL + Redis + 预构建 `frontend/dist` 由 Nginx 托管。

## 瓶颈与对策

### 1. 终端 `mvn spring-boot:run` 慢

| 原因 | 对策 |
|------|------|
| 每次冷启动 JVM + Hibernate `ddl-auto=update` | 生产用 `java -jar`；开发关闭 SQL 日志（见 `application-dev.yml`） |
| devtools 热重启 | 开发配置中已建议关闭 restart |
| H2 文件库 DDL | 生产换 MySQL；开发可接受 |

**推荐启动**（开发）：

```powershell
# 首次或改依赖后
cd backend; mvn -q -DskipTests package
java -jar target/lawfirm-backend-2.0.0.jar --spring.profiles.active=dev

cd frontend; npm run dev
```

或使用仓库根目录：`npm run dev:frontend`（另开终端跑后端 JAR）。

### 2. 页面跳转卡顿

| 原因 | 对策（已实现） |
|------|----------------|
| `router-view` 每次销毁重挂载 | 主布局 `keep-alive` 缓存高频页（工作台/列表等） |
| `transition mode="out-in"` 等待动画 | 已移除阻塞式过渡 |
| 每次 GET 带 `_t` 禁用缓存 | 默认不再全量加时间戳；需刷新时 `noCache: true` |
| 布局内同步加载 AI 助手大组件 | `defineAsyncComponent` 按需加载 |
| 30s 轮询未读通知 | 改为 60s + 页面可见时才轮询 |

### 3. 50 人并发后端

| 组件 | 配置 |
|------|------|
| Tomcat | `max=200` 工作线程，`accept-count=100`（见 `application.yml`） |
| HikariCP | `maximum-pool-size=50`（与并发用户数同级） |
| 本地缓存 | Caffeine：用户/角色/配置/工作台统计/未读数 |
| 数据库 | **必须** MySQL（勿用 H2 上生产） |
| Redis | 生产 profile 启用，用于分布式会话/热点缓存（可选第二步） |
| 重任务 | LLM、Office 转 HTML、卷宗识别走异步或独立超时，不占 Tomcat 线程过久 |

### 4. 50 人并发前端

| 措施 | 说明 |
|------|------|
| `npm run build` + Nginx | 生产勿用 Vite dev server |
| 分包 | `vue-vendor` / `element-plus` 独立 chunk（已配置） |
| 按需路由 | 路由级 lazy import（已有） |
| keep-alive | 减少重复请求与组件重建 |

## 部署清单（内网单机起步）

1. 安装 JDK 11、MySQL 8、Nginx（可选 Redis）。
2. 导入数据库脚本或首次 `ddl-auto=validate` + Flyway（后续迭代）。
3. 配置环境变量：`LAWFIRM_DB_*`、`JWT_SECRET`、`REDIS_*`。
4. 构建：`cd backend && mvn package`；`cd frontend && npm run build`。
5. Nginx：`/` → `frontend/dist`；`/api/` → `http://127.0.0.1:8080/api/`。
6. 系统服务：`java -jar lawfirm-backend.jar --spring.profiles.active=prod`。

## 容量粗算

- 50 用户 × 平均 2 req/s 峰值 ≈ 100 RPS（含轮询）；Tomcat 200 线程 + 50 连接池可覆盖。
- LLM/识别接口应限流（如每用户 2 并发），避免拖垮全局。

## 后续演进（按需）

1. Redis 缓存未读数、工作台统计（多实例部署时）。
2. 卷宗/Office 预览异步队列 + WebSocket 通知完成。
3. 读写分离（>80 人时再考虑）。
