#!/usr/bin/env bash
# 导出预览环境 H2 数据库为可提交进 git 的 SQL 快照。
#
# 背景：预览 profile 使用 H2 文件库（server/api/wisestar.mv.db），该文件被
# .gitignore 忽略，且只存在于当前容器内。环境重建后数据会丢失。本脚本把
# 数据库全量（表结构 + 数据）导出到 server/db-snapshot/wisestar.sql，由人工
# 提交进仓库，新环境启动时用 start-preview.sh 自动恢复。
#
# 用法：
#   server/db-export.sh
#
# 注意：H2 文件库为单进程独占，导出前必须先停止预览后端。

set -euo pipefail

SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$SERVER_DIR/api"
SNAP_DIR="$SERVER_DIR/db-snapshot"
SNAP_FILE="$SNAP_DIR/wisestar.sql"
DB_FILE="$API_DIR/wisestar.mv.db"

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

H2_JAR="$(resolve_h2_jar)" || {
  echo "错误：找不到 h2 jar，请先执行 maven 构建以下载依赖。" >&2
  exit 1
}

if [ ! -f "$DB_FILE" ]; then
  echo "错误：未发现数据库文件 $DB_FILE，无可导出内容。" >&2
  exit 1
fi

# 导出前需停止预览后端：H2 文件库为单进程独占，后端运行时无法从另一进程读取。
if ps -eo args 2>/dev/null | grep -E '[j]ava .*wisestar-v1\.9\.0\.jar' >/dev/null 2>&1; then
  echo "错误：预览后端正在运行，请先停止后端再导出快照。" >&2
  exit 1
fi

mkdir -p "$SNAP_DIR"
cd "$API_DIR"

URL='jdbc:h2:file:./wisestar;MODE=MySQL;DATABASE_TO_LOWER=TRUE'
TMP="$SNAP_DIR/.wisestar.sql.tmp"

# 先写临时文件，成功后再替换，避免中途失败破坏已有快照
if ! java -cp "$H2_JAR" org.h2.tools.Script \
  -url "$URL" -user sa -password "" \
  -script "$TMP" -options DROP >/dev/null 2>&1; then
  mv "$TMP" "$TMP.failed" 2>/dev/null || true
  echo "错误：导出失败，数据库可能仍被占用。请确认后端已停止后重试。" >&2
  exit 1
fi

mv "$TMP" "$SNAP_FILE"

echo "已导出快照: $SNAP_FILE ($(wc -c < "$SNAP_FILE") bytes)"
echo "下一步：git add server/db-snapshot/wisestar.sql && git commit"
