#!/usr/bin/env bash
# 启动预览后端。若 H2 库文件不存在但存在 SQL 快照，先从快照恢复再启动，
# 这样在新容器/新会话中也能找回上一会话提交进仓库的数据。
#
# 用法：
#   server/start-preview.sh              # 前台启动
#
# 相关文件：
#   server/db-snapshot/wisestar.sql      # 快照（commit 进 git）
#   server/api/wisestar.mv.db            # H2 库（gitignore）

set -euo pipefail

SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$SERVER_DIR/api"
SNAP_FILE="$SERVER_DIR/db-snapshot/wisestar.sql"
DB_FILE="$API_DIR/wisestar.mv.db"
JAR="$API_DIR/target/wisestar-v1.9.0.jar"

resolve_h2_jar() {
  if [ -n "${H2_JAR:-}" ] && [ -f "${H2_JAR}" ]; then
    echo "${H2_JAR}"
    return 0
  fi
  local found
  found="$(find "${HOME}/.m2/repository/com/h2database/h2" -name 'h2-*.jar' 2>/dev/null | sort -V | tail -1 || true)"
  if [ -n "${found}" ]; then
    echo "${found}"
    return 0
  fi
  return 1
}

if [ ! -f "$JAR" ]; then
  echo "错误：未找到后端 jar $JAR，请先构建：cd server && mvn clean package -pl api -am -DskipTests" >&2
  exit 1
fi

cd "$API_DIR"

URL='jdbc:h2:file:./wisestar;MODE=MySQL;DATABASE_TO_LOWER=TRUE'
SQL_INIT_MODE=always

if [ ! -f "$DB_FILE" ]; then
  if [ -f "$SNAP_FILE" ]; then
    H2_JAR="$(resolve_h2_jar)" || {
      echo "错误：需要从快照恢复，但找不到 h2 jar。" >&2
      exit 1
    }
    echo "未发现 H2 库，正在从快照恢复: $SNAP_FILE"
    java -cp "$H2_JAR" org.h2.tools.RunScript \
      -url "$URL" -user sa -password "" -script "$SNAP_FILE"
    SQL_INIT_MODE=never
    echo "快照恢复完成。"
  else
    echo "未发现 H2 库与快照，将按种子脚本初始化全新库。"
  fi
fi

exec java -Xms256m -Xmx768m -jar "$JAR" \
  --spring.profiles.active=preview \
  --spring.sql.init.mode="$SQL_INIT_MODE"
