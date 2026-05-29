#!/usr/bin/env bash
# 服务器首次安装辅助：创建目录、安装 Nginx/systemd 模板
# 用法：
#   sudo bash scripts/server-bootstrap.sh --dirs-only
#   LAWFIRM_DOMAIN=law.example.com sudo bash scripts/server-bootstrap.sh --install-config

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP="${APP:-/opt/lawfirm/app}"
LAWFIRM_DOMAIN="${LAWFIRM_DOMAIN:-}"

log() { echo "[bootstrap] $*"; }

setup_dirs() {
  log "创建用户与目录 ..."
  id lawfirm &>/dev/null || useradd -r -m -s /bin/bash lawfirm
  mkdir -p "$APP" /var/lawfirm/uploads /var/lawfirm/backups /var/lawfirm/data /var/lawfirm/logs
  mkdir -p /etc/lawfirm
  chown -R lawfirm:lawfirm /opt/lawfirm /var/lawfirm 2>/dev/null || true
  log "目录就绪: $APP, /var/lawfirm/*"
}

install_nginx() {
  if [[ -z "$LAWFIRM_DOMAIN" ]]; then
    log "ERROR: 请设置环境变量 LAWFIRM_DOMAIN=你的域名"
    exit 1
  fi
  log "安装 Nginx 配置 (server_name=$LAWFIRM_DOMAIN) ..."
  sed "s/LAWFIRM_DOMAIN/$LAWFIRM_DOMAIN/g" "$REPO_ROOT/deploy/nginx/lawfirm.conf" \
    > /etc/nginx/sites-available/lawfirm
  ln -sf /etc/nginx/sites-available/lawfirm /etc/nginx/sites-enabled/lawfirm
  rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true
  nginx -t
  systemctl enable nginx
  systemctl reload nginx
  log "Nginx 已启用。HTTPS: certbot --nginx -d $LAWFIRM_DOMAIN"
}

install_systemd() {
  log "安装 systemd 单元 ..."
  cp "$REPO_ROOT/deploy/systemd/lawfirm-backend.service" /etc/systemd/system/
  systemctl daemon-reload
  systemctl enable lawfirm-backend
  log "已 enable lawfirm-backend（需先配置 /etc/lawfirm/lawfirm.env 并构建 JAR）"
}

install_env_example() {
  if [[ ! -f /etc/lawfirm/lawfirm.env ]]; then
    cp "$REPO_ROOT/deploy/env/lawfirm.env.example" /etc/lawfirm/lawfirm.env
    chmod 600 /etc/lawfirm/lawfirm.env
    chown lawfirm:lawfirm /etc/lawfirm/lawfirm.env
    log "已创建 /etc/lawfirm/lawfirm.env — 请编辑后启动服务"
  else
    log "/etc/lawfirm/lawfirm.env 已存在，跳过"
  fi
}

case "${1:-}" in
  --dirs-only)
    setup_dirs
    ;;
  --install-config)
    setup_dirs
    install_env_example
    install_nginx
    install_systemd
    ;;
  *)
    echo "用法:"
    echo "  sudo bash $0 --dirs-only"
    echo "  LAWFIRM_DOMAIN=example.com sudo bash $0 --install-config"
    exit 1
    ;;
esac
