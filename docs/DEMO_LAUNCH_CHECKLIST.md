# Demo 上线命令清单（域名 + GitHub）

> 场景：律师内测 demo，版本仍会频繁改动。  
> 完整说明见 [DEPLOY_TENCENT_CLOUD.md](DEPLOY_TENCENT_CLOUD.md)。

**开始前请替换以下占位符：**

| 占位符 | 示例 |
|--------|------|
| `YOUR_DOMAIN` | `law.yourfirm.com` |
| `GITHUB_REPO` | `git@github.com:your-org/ZGAI.git` 或 `https://github.com/your-org/ZGAI.git` |
| `SERVER_IP` | 腾讯云公网 IP |
| `DEPLOY_BRANCH` | `main`（若默认分支是 `master` 则改） |

---

## 零、本地准备（Windows，一次性）

### 0.1 确认能构建

```powershell
cd D:\ZGAI\backend
mvn -DskipTests package

cd D:\ZGAI\frontend
npm ci
npm run build
```

### 0.2 推送到 GitHub（勿提交密钥）

确认 `.gitignore` 已忽略：`backend/.env`、`backend/data/`、`frontend/node_modules/`、`*.mv.db` 等。

```powershell
cd D:\ZGAI
git status
git add .
git commit -m "chore: demo deploy ready"
git push origin main
```

私有仓库：服务器 clone 时需配置 SSH 密钥或 Personal Access Token。

---

## 一、域名与腾讯云（控制台操作）

1. **DNS**：添加 **A 记录** `YOUR_DOMAIN` → `SERVER_IP`
2. **安全组**：入站放行 **22、80、443**
3. **备案**：域名已备案则 HTTPS 可正常访问；未备案可能无法解析或访问

验证解析（本地 PowerShell）：

```powershell
nslookup YOUR_DOMAIN
```

---

## 二、服务器首次初始化（SSH 登录后执行）

### 2.1 安装依赖

```bash
sudo apt update && sudo apt install -y git nginx mysql-server redis-server \
  openjdk-11-jdk maven certbot python3-certbot-nginx

# Node 20（Vite 需要较新版本）
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v   # 应 >= 18
```

### 2.2 用户与目录

```bash
sudo useradd -r -m -s /bin/bash lawfirm 2>/dev/null || true
sudo mkdir -p /opt/lawfirm/app /var/lawfirm/{uploads,backups,data,logs}
sudo chown -R lawfirm:lawfirm /opt/lawfirm /var/lawfirm
```

### 2.3 MySQL 建库

```bash
sudo mysql
```

```sql
CREATE DATABASE lawfirm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'lawfirm'@'localhost' IDENTIFIED BY '你的MySQL强密码';
GRANT ALL ON lawfirm.* TO 'lawfirm'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 2.4 Redis

```bash
sudo systemctl enable --now redis-server
redis-cli ping
```

### 2.5 克隆 GitHub 仓库

**HTTPS（私有库需输入 GitHub 用户名 + PAT）：**

```bash
sudo -u lawfirm git clone https://github.com/你的用户名/ZGAI.git /opt/lawfirm/app
```

**SSH（推荐：先在服务器生成密钥并加到 GitHub Deploy keys）：**

```bash
sudo -u lawfirm ssh-keygen -t ed25519 -N "" -f /home/lawfirm/.ssh/id_ed25519
sudo -u lawfirm cat /home/lawfirm/.ssh/id_ed25519.pub
# 复制公钥 → GitHub 仓库 Settings → Deploy keys → Add

sudo -u lawfirm git clone git@github.com:你的用户名/ZGAI.git /opt/lawfirm/app
```

### 2.6 后端环境变量

```bash
sudo mkdir -p /etc/lawfirm
sudo cp /opt/lawfirm/app/deploy/env/lawfirm.env.example /etc/lawfirm/lawfirm.env
sudo chmod 600 /etc/lawfirm/lawfirm.env
sudo chown lawfirm:lawfirm /etc/lawfirm/lawfirm.env
sudo nano /etc/lawfirm/lawfirm.env
```

**`nano` 里至少修改：**

- `LAWFIRM_DB_PASSWORD`
- `JWT_SECRET`（可用 `openssl rand -hex 32` 生成）
- `ZHIPU_API_KEY`
- **首次建表**：取消注释 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
- **Demo 可关 Qdrant**（不装 Docker 时必关）：

```bash
QDRANT_ENABLED=false
CASE_SEARCH_SEMANTIC_ENABLED=false
```

### 2.7 前端生产变量

```bash
sudo -u lawfirm cp /opt/lawfirm/app/frontend/.env.production.example \
  /opt/lawfirm/app/frontend/.env.production
```

### 2.8 安装 Nginx + systemd

```bash
cd /opt/lawfirm/app
export LAWFIRM_DOMAIN=YOUR_DOMAIN
sudo bash scripts/server-bootstrap.sh --install-config
```

### 2.9 首次构建并启动

```bash
cd /opt/lawfirm/app
sudo bash scripts/deploy-native.sh
sudo systemctl enable --now lawfirm-backend
sudo systemctl status lawfirm-backend
```

若启动失败：

```bash
sudo journalctl -u lawfirm-backend -n 100 --no-pager
```

### 2.10 HTTPS 证书

```bash
sudo certbot --nginx -d YOUR_DOMAIN
```

按提示选择重定向 HTTP → HTTPS。

---

## 三、验收（给律师测试前）

```bash
# 后端是否在跑
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/login

# 登录接口（应返回 200 和 token）
curl -s -X POST https://YOUR_DOMAIN/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

浏览器打开：`https://YOUR_DOMAIN`

- [ ] 登录页正常
- [ ] `admin` / `admin123` 能登录 → **立即改密码**
- [ ] AI 智能中心可打开
- [ ] 上传传票测识别（约 1–2 分钟）

建表成功后，注释掉 `/etc/lawfirm/lawfirm.env` 里的 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`，然后：

```bash
sudo systemctl restart lawfirm-backend
```

---

## 四、日常更新（你改完代码 push 之后）

**本地：**

```powershell
cd D:\ZGAI
git add .
git commit -m "feat: xxx"
git push origin main
```

**服务器：**

```bash
cd /opt/lawfirm/app
sudo bash scripts/deploy-native.sh
```

仅改前端时（更快）：

```bash
cd /opt/lawfirm/app
DEPLOY_BRANCH=main SKIP_BACKEND=1 sudo bash scripts/deploy-native.sh
```

仅改后端时：

```bash
cd /opt/lawfirm/app
DEPLOY_BRANCH=main SKIP_FRONTEND=1 sudo bash scripts/deploy-native.sh
```

---

## 五、给律师的测试说明（可复制发群）

```
测试地址：https://YOUR_DOMAIN
账号：admin（密码单独私发，勿在群里明文）
说明：当前为内测 Demo，功能与数据可能随时重置，请勿录入真实客户敏感信息。
AI 识别/生成单次约 1–2 分钟，请耐心等待。
```

---

## 六、常见问题

| 问题 | 处理 |
|------|------|
| `git pull` 要密码 | 改用 SSH clone，或配置 PAT |
| 502 | `journalctl -u lawfirm-backend -n 50` |
| 域名打不开 | 查 DNS、备案、安全组 80/443 |
| AI 超时 | 确认 Nginx `proxy_read_timeout 600s`、智谱 Key |
| 构建慢 | 正常，4GB 机首次 mvn+npm 约 5–15 分钟 |
