# 腾讯云轻量服务器部署与持续更新

> 适用：Ubuntu 24.04 LTS、2 核 4GB+、原生 Nginx + systemd + MySQL + Redis。  
> 仓库交付物：`deploy/` 模板、`scripts/deploy-native.sh`、`scripts/server-bootstrap.sh`。

## 架构

- 用户访问 `https://你的域名` → Nginx 提供 `frontend/dist` 静态资源
- 浏览器请求 `/api/*` → Nginx 反代到本机 `127.0.0.1:8080`（Spring Boot，`context-path=/api`）
- 前后端**同源**，无需单独配置 CORS
- 上传与备份目录在 `/var/lawfirm/`，与 Git 代码目录分离

## 一、上线前准备

| 项 | 说明 |
|----|------|
| 安全组 | 放行 **22、80、443**；勿对公网开放 3306 / 6379 / 8080 |
| 域名 | DNS **A 记录** 指向服务器公网 IP |
| ICP 备案 | 国内公网域名解析通常需完成备案，否则可能被拦截 |
| 密钥 | 智谱 GLM Coding Plan Key（见 [GLM_CODING_PLAN_SETUP.md](GLM_CODING_PLAN_SETUP.md)）、`JWT_SECRET`、MySQL 密码 |

## 二、服务器一次性初始化

### 2.1 安装依赖

```bash
sudo apt update && sudo apt install -y git nginx mysql-server redis-server \
  openjdk-11-jdk maven nodejs npm certbot python3-certbot-nginx
```

若 `nodejs` 版本过旧（Vite 建议 Node 18+），可改用 NodeSource：

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

可选：安装 Docker 仅用于 Qdrant（类案语义检索）：

```bash
sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker lawfirm
```

### 2.2 目录与用户

在**已克隆仓库**后执行，或手动创建：

```bash
# 若已 clone 到 /opt/lawfirm/app：
cd /opt/lawfirm/app && sudo bash scripts/server-bootstrap.sh --dirs-only
```

或手动：

```bash
sudo useradd -r -m -s /bin/bash lawfirm 2>/dev/null || true
sudo mkdir -p /opt/lawfirm/app /var/lawfirm/{uploads,backups,data,logs}
sudo chown -R lawfirm:lawfirm /opt/lawfirm /var/lawfirm
```

### 2.3 MySQL

```bash
sudo mysql
```

```sql
CREATE DATABASE lawfirm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'lawfirm'@'localhost' IDENTIFIED BY '请替换为强密码';
GRANT ALL ON lawfirm.* TO 'lawfirm'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 2.4 Redis

```bash
sudo systemctl enable --now redis-server
redis-cli ping   # 应返回 PONG
```

### 2.5 生产环境变量

```bash
sudo mkdir -p /etc/lawfirm
sudo cp /opt/lawfirm/app/deploy/env/lawfirm.env.example /etc/lawfirm/lawfirm.env
sudo chmod 600 /etc/lawfirm/lawfirm.env
sudo chown lawfirm:lawfirm /etc/lawfirm/lawfirm.env
sudo nano /etc/lawfirm/lawfirm.env   # 填入数据库密码、JWT、智谱 Key
```

**首次建表**：在 `lawfirm.env` 中**临时**取消注释：

```bash
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

首次启动成功并完成登录验收后，**注释或删除**该行，改回默认 `validate`。

### 2.6 克隆代码

```bash
sudo -u lawfirm git clone <你的仓库URL> /opt/lawfirm/app
```

### 2.7 前端生产构建变量

```bash
sudo -u lawfirm cp /opt/lawfirm/app/frontend/.env.production.example \
  /opt/lawfirm/app/frontend/.env.production
```

### 2.8 安装 Nginx 与 systemd 模板

```bash
cd /opt/lawfirm/app
export LAWFIRM_DOMAIN=law.example.com   # 替换为你的域名
sudo bash scripts/server-bootstrap.sh --install-config
```

或手动：

```bash
sudo cp deploy/nginx/lawfirm.conf /etc/nginx/sites-available/lawfirm
# 编辑 server_name、root 路径
sudo ln -sf /etc/nginx/sites-available/lawfirm /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo cp deploy/systemd/lawfirm-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
```

### 2.9 可选 Qdrant

```bash
cd /opt/lawfirm/app
sudo -u lawfirm docker compose up -d qdrant
```

不需要语义检索时，在 `lawfirm.env` 设置：

```bash
QDRANT_ENABLED=false
CASE_SEARCH_SEMANTIC_ENABLED=false
```

## 三、首次发布

可打印勾选：[FIRST_RELEASE_CHECKLIST.md](FIRST_RELEASE_CHECKLIST.md)

```bash
cd /opt/lawfirm/app
sudo bash scripts/deploy-native.sh
```

HTTPS 证书（需域名已解析到本机）：

```bash
sudo certbot --nginx -d law.example.com
```

## 四、验收清单

- [ ] `https://你的域名` 打开登录页
- [ ] 使用 `admin` / `admin123` 登录（**立即在系统内修改密码**）
- [ ] 登录后访问 AI 智能中心，健康条无致命错误
- [ ] 上传传票 PDF 测试识别（GLM 约 1–2 分钟，依赖 Nginx `proxy_read_timeout 600s`）
- [ ] `sudo systemctl status lawfirm-backend` 为 `active (running)`
- [ ] `sudo journalctl -u lawfirm-backend -n 50` 无 MySQL/Redis 连接错误

登录接口冒烟：

```bash
curl -s -X POST https://你的域名/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

## 五、后续版本更新

在服务器执行（建议固定 `main` 或 `release` 分支）：

```bash
cd /opt/lawfirm/app
sudo bash scripts/deploy-native.sh
```

或指定分支：

```bash
DEPLOY_BRANCH=release sudo bash scripts/deploy-native.sh
```

| 场景 | 做法 |
|------|------|
| 仅前端 | `cd frontend && npm ci && npm run build && sudo nginx -t && sudo systemctl reload nginx` |
| 仅后端 | `cd backend && mvn -DskipTests package && sudo systemctl restart lawfirm-backend` |
| 表结构变更 | 先 `mysqldump` 备份 → 临时 `SPRING_JPA_HIBERNATE_DDL_AUTO=update` 或执行 SQL |
| 配置变更 | 编辑 `/etc/lawfirm/lawfirm.env` → `sudo systemctl restart lawfirm-backend` |
| 回滚 | `git checkout <旧commit>` 后重新执行 `deploy-native.sh`（数据库需提前快照） |

**切勿**对 `/var/lawfirm/uploads`、`/var/lawfirm/backups` 执行 `git clean`。

## 六、备份

```bash
# MySQL
mysqldump -u lawfirm -p lawfirm | gzip > /var/lawfirm/backups/lawfirm-$(date +%F).sql.gz

# 上传目录
tar czf /var/lawfirm/backups/uploads-$(date +%F).tar.gz -C /var/lawfirm uploads
```

## 七、排错

| 现象 | 排查 |
|------|------|
| 502 Bad Gateway | `systemctl status lawfirm-backend`；JAR 是否存在；8080 是否监听 |
| 启动失败 validate | 空库未建表 → 临时 `SPRING_JPA_HIBERNATE_DDL_AUTO=update` |
| Redis 连接失败 | `redis-cli ping`；`REDIS_HOST` / 密码 |
| AI 超时 504 | Nginx `proxy_read_timeout`；智谱 Key 与端点 |
| 静态页 404 | `frontend/dist` 是否已 build；`root` 路径是否正确 |
| 上传失败 | `client_max_body_size 50m`；`LAWFIRM_UPLOAD_PATH` 权限 |

查看日志：

```bash
sudo journalctl -u lawfirm-backend -f
sudo tail -f /var/log/nginx/error.log
```

## 八、安全清单

- 修改默认 `admin` 密码
- `JWT_SECRET` 使用随机长字符串
- `/etc/lawfirm/lawfirm.env` 权限 `600`，勿提交 Git
- 定期备份数据库与 uploads
- 腾讯云监控磁盘与内存

## 九、相关文件

| 路径 | 说明 |
|------|------|
| [deploy/nginx/lawfirm.conf](../deploy/nginx/lawfirm.conf) | Nginx 站点模板 |
| [deploy/systemd/lawfirm-backend.service](../deploy/systemd/lawfirm-backend.service) | systemd 单元 |
| [deploy/env/lawfirm.env.example](../deploy/env/lawfirm.env.example) | 后端环境变量示例 |
| [scripts/deploy-native.sh](../scripts/deploy-native.sh) | 一键更新脚本 |
| [scripts/server-bootstrap.sh](../scripts/server-bootstrap.sh) | 首次安装辅助脚本 |
| [frontend/.env.production.example](../frontend/.env.production.example) | 前端生产构建变量 |
