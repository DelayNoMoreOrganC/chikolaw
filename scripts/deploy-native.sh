#!/usr/bin/env bash
# 腾讯云 / Ubuntu 原生部署：拉代码 → 构建前后端 → 重启服务
# 用法：sudo bash scripts/deploy-native.sh
# 环境变量：
#   APP=/opt/lawfirm/app
#   DEPLOY_BRANCH=main
#   SKIP_FRONTEND=1 | SKIP_BACKEND=1
#   MVN_OPTS="-DskipTests"

set -euo pipefail

APP="${APP:-/opt/lawfirm/app}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
MVN_OPTS="${MVN_OPTS:--DskipTests}"
JAR_NAME="lawfirm-backend-2.0.0.jar"

log() { echo "[deploy] $*"; }

if [[ ! -d "$APP/.git" ]]; then
  log "ERROR: $APP 不是 git 仓库，请先 clone 到该目录"
  exit 1
fi

cd "$APP"
log "拉取分支 $DEPLOY_BRANCH ..."
git fetch origin
git checkout "$DEPLOY_BRANCH"
git pull origin "$DEPLOY_BRANCH"
COMMIT=$(git rev-parse --short HEAD)
log "当前 commit: $COMMIT"

if [[ "${SKIP_BACKEND:-0}" != "1" ]]; then
  log "构建后端 ..."
  cd "$APP/backend"
  mvn clean package $MVN_OPTS
  if [[ ! -f "target/$JAR_NAME" ]]; then
    log "ERROR: 未找到 target/$JAR_NAME"
    exit 1
  fi
fi

if [[ "${SKIP_FRONTEND:-0}" != "1" ]]; then
  log "构建前端 ..."
  cd "$APP/frontend"
  if [[ ! -f .env.production ]]; then
    if [[ -f .env.production.example ]]; then
      log "从 .env.production.example 生成 .env.production"
      cp .env.production.example .env.production
    else
      log "WARN: 缺少 .env.production，将使用 Vite 默认 /api"
    fi
  fi
  if command -v npm >/dev/null 2>&1; then
    npm ci
    npm run build
  else
    log "ERROR: 未找到 npm"
    exit 1
  fi
fi

if systemctl is-active --quiet lawfirm-backend 2>/dev/null; then
  log "重启 lawfirm-backend ..."
  systemctl restart lawfirm-backend
  sleep 2
  if ! systemctl is-active --quiet lawfirm-backend; then
    log "ERROR: 后端启动失败，查看: journalctl -u lawfirm-backend -n 80"
    exit 1
  fi
else
  log "WARN: lawfirm-backend 未安装或未运行，跳过 systemctl restart"
  log "      请执行: systemctl enable --now lawfirm-backend"
fi

if command -v nginx >/dev/null 2>&1; then
  nginx -t
  systemctl reload nginx
fi

log "Deploy OK: $COMMIT @ $(date -Iseconds)"
