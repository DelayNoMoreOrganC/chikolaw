#!/usr/bin/env bash
# 本地校验部署交付物是否齐全（在提交前或 CI 中运行）
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MISSING=0

check() {
  if [[ -f "$1" || -d "$1" ]]; then
    echo "OK  $1"
  else
    echo "MISS $1"
    MISSING=1
  fi
}

echo "检查部署交付物 ..."
check "$ROOT/docs/DEPLOY_TENCENT_CLOUD.md"
check "$ROOT/deploy/nginx/lawfirm.conf"
check "$ROOT/deploy/systemd/lawfirm-backend.service"
check "$ROOT/deploy/env/lawfirm.env.example"
check "$ROOT/scripts/deploy-native.sh"
check "$ROOT/scripts/server-bootstrap.sh"
check "$ROOT/frontend/.env.production.example"

if [[ "$MISSING" -ne 0 ]]; then
  echo "部分文件缺失"
  exit 1
fi
echo "全部通过"
