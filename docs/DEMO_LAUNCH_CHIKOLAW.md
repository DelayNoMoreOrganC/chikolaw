# chikolaw.cloud Demo 上线清单（已填参数）

| 项 | 值 |
|----|-----|
| 域名 | `chikolaw.cloud` |
| GitHub | `git@github.com:DelayNoMoreOrganC/chikolaw.git` |
| 公网 IP | `81.71.136.215` |
| 服务器登录密钥 | `d:\我的资料库\Documents\Downloads\test.pem`（腾讯云 SSH，**勿提交 Git**） |
| 服务器代码目录 | `/opt/lawfirm/app` |

> `test.pem` 是**登录腾讯云服务器**用的密钥，不是 GitHub 密钥。

---

## 一、本地：推代码到 GitHub

```powershell
# 构建前请先关掉 npm run dev（Ctrl+C），避免 EPERM
cd D:\ZGAI\backend
mvn -DskipTests package

cd D:\ZGAI\frontend
npm install
npm run build

cd D:\ZGAI
git remote -v
# 若 remote 还不是 chikolaw，执行：
# git remote set-url origin git@github.com:DelayNoMoreOrganC/chikolaw.git

git add .
git commit -m "chore: demo deploy"
git push origin main
```

若默认分支是 `master`，把 `main` 改成 `master`。

---

## 二、DNS（域名控制台）

添加 **A 记录**：

| 主机记录 | 记录类型 | 记录值 |
|----------|----------|--------|
| `@` | A | `81.71.136.215` |
| `www`（可选） | A | `81.71.136.215` |

---

## 三、登录服务器（Windows PowerShell）

```powershell
# 首次使用 pem 建议限制权限（仅当前用户可读）
icacls "d:\我的资料库\Documents\Downloads\test.pem" /inheritance:r
icacls "d:\我的资料库\Documents\Downloads\test.pem" /grant:r "$($env:USERNAME):(R)"

# 登录（用户名多为 ubuntu 或 root，以腾讯云面板为准）
ssh -i "d:\我的资料库\Documents\Downloads\test.pem" ubuntu@81.71.136.215
# 若 ubuntu 不行，试：
# ssh -i "d:\我的资料库\Documents\Downloads\test.pem" root@81.71.136.215
```

---

## 四、服务器首次安装（SSH 里整段执行）

### 4.1 依赖

```bash
sudo apt update && sudo apt install -y git nginx mysql-server redis-server \
  openjdk-11-jdk maven certbot python3-certbot-nginx

curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v
```

### 4.2 用户与目录

```bash
sudo useradd -r -m -s /bin/bash lawfirm 2>/dev/null || true
sudo mkdir -p /opt/lawfirm/app /var/lawfirm/{uploads,backups,data,logs}
sudo chown -R lawfirm:lawfirm /opt/lawfirm /var/lawfirm
```

### 4.3 MySQL

```bash
sudo mysql
```

```sql
CREATE DATABASE lawfirm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'lawfirm'@'localhost' IDENTIFIED BY '请设MySQL强密码';
GRANT ALL ON lawfirm.* TO 'lawfirm'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 4.4 Redis

```bash
sudo systemctl enable --now redis-server
redis-cli ping
```

### 4.5 GitHub Deploy Key（私有库必做）

```bash
sudo -u lawfirm ssh-keygen -t ed25519 -N "" -f /home/lawfirm/.ssh/id_ed25519
sudo -u lawfirm cat /home/lawfirm/.ssh/id_ed25519.pub
```

复制输出的公钥 → GitHub 仓库 **Settings → Deploy keys → Add deploy key**（勾选 Allow write access 若需 pull 即可，一般只读够用）。

```bash
sudo -u lawfirm ssh -o StrictHostKeyChecking=accept-new -T git@github.com
sudo -u lawfirm git clone git@github.com:DelayNoMoreOrganC/chikolaw.git /opt/lawfirm/app
```

### 4.6 环境变量

```bash
sudo mkdir -p /etc/lawfirm
sudo cp /opt/lawfirm/app/deploy/env/lawfirm.env.example /etc/lawfirm/lawfirm.env
sudo chmod 600 /etc/lawfirm/lawfirm.env
sudo chown lawfirm:lawfirm /etc/lawfirm/lawfirm.env
sudo nano /etc/lawfirm/lawfirm.env
```

**必须修改：**

```bash
LAWFIRM_DB_PASSWORD=你的MySQL密码
JWT_SECRET=用下面命令生成后粘贴
ZHIPU_API_KEY=你的智谱Key

# 首次建表：取消下一行注释
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Demo 暂不装 Qdrant
QDRANT_ENABLED=false
CASE_SEARCH_SEMANTIC_ENABLED=false
```

生成 JWT：

```bash
openssl rand -hex 32
```

### 4.7 前端生产配置

```bash
sudo -u lawfirm cp /opt/lawfirm/app/frontend/.env.production.example \
  /opt/lawfirm/app/frontend/.env.production
```

### 4.8 Nginx + systemd

```bash
cd /opt/lawfirm/app
export LAWFIRM_DOMAIN=chikolaw.cloud
sudo bash scripts/server-bootstrap.sh --install-config
```

### 4.9 首次构建与启动

```bash
cd /opt/lawfirm/app
sudo bash scripts/deploy-native.sh
sudo systemctl enable --now lawfirm-backend
sudo systemctl status lawfirm-backend
```

### 4.10 HTTPS

```bash
sudo certbot --nginx -d chikolaw.cloud -d www.chikolaw.cloud
```

若未配置 `www` 的 DNS，只申请主域名：

```bash
sudo certbot --nginx -d chikolaw.cloud
```

---

## 五、验收

```bash
curl -s -X POST https://chikolaw.cloud/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

浏览器打开：**https://chikolaw.cloud**

- 登录 `admin` / `admin123` 后**立即改密码**
- 建表成功后注释 `/etc/lawfirm/lawfirm.env` 里的 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`，再执行：
  `sudo systemctl restart lawfirm-backend`

---

## 六、日常更新

**本地：**

```powershell
cd D:\ZGAI
git push origin main
```

**服务器：**

```bash
cd /opt/lawfirm/app && sudo bash scripts/deploy-native.sh
```

---

## 七、给律师的测试说明（可复制）

```
测试地址：https://chikolaw.cloud
账号：admin（密码单独私发）
说明：内测 Demo，数据可能重置，请勿录入真实客户敏感信息。
AI 功能单次约 1–2 分钟，请耐心等待。
```

---

## 八、排错

| 现象 | 处理 |
|------|------|
| ssh 连不上 | 查安全组是否放行 22；用户名 ubuntu/root；pem 路径加引号 |
| git clone 失败 | 配置 Deploy Key；`ssh -T git@github.com` |
| 502 | `sudo journalctl -u lawfirm-backend -n 80` |
| 域名无法访问 | DNS 是否指向 `81.71.136.215`；备案是否完成 |
| 后端启动失败 | 查 MySQL 密码、`lawfirm.env`、是否首次需要 `ddl-auto=update` |
