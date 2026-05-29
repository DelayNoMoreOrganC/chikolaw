# 首次上线检查清单（服务器端操作）

在腾讯云 Ubuntu 24.04 上按顺序执行。详细说明见 [DEPLOY_TENCENT_CLOUD.md](DEPLOY_TENCENT_CLOUD.md)。

## 准备

- [ ] 安全组已放行 22 / 80 / 443
- [ ] 域名 A 记录指向服务器 IP
- [ ] 已完成 ICP 备案（国内公网访问）
- [ ] 已准备智谱 API Key、`JWT_SECRET`、MySQL 密码

## 安装

- [ ] `apt install`：git, nginx, mysql, redis, openjdk-11-jdk, maven, nodejs, npm, certbot
- [ ] `git clone` 到 `/opt/lawfirm/app`
- [ ] `scripts/server-bootstrap.sh --dirs-only`
- [ ] MySQL 创建库 `lawfirm` 与用户
- [ ] `redis-cli ping` 返回 PONG
- [ ] 复制 `deploy/env/lawfirm.env.example` → `/etc/lawfirm/lawfirm.env` 并填写
- [ ] **首次**启用 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
- [ ] 复制 `frontend/.env.production.example` → `frontend/.env.production`
- [ ] `LAWFIRM_DOMAIN=你的域名 scripts/server-bootstrap.sh --install-config`
- [ ] （可选）`docker compose up -d qdrant`

## 发布

- [ ] `sudo bash scripts/deploy-native.sh`
- [ ] `certbot --nginx -d 你的域名`
- [ ] 注释掉 `SPRING_JPA_HIBERNATE_DDL_AUTO=update` 并 `systemctl restart lawfirm-backend`

## 验收

- [ ] HTTPS 打开登录页
- [ ] `admin` 登录成功并**立即改密**
- [ ] AI 智能中心可用
- [ ] 传票识别 smoke 测试通过

## 后续更新

```bash
cd /opt/lawfirm/app && sudo bash scripts/deploy-native.sh
```
