#!/usr/bin/env bash
# 导出可导入 MySQL 的业务数据（data-only SQL）。
#
# 背景：server/db-snapshot/wisestar.sql 是 H2 的 SCRIPT 全量导出，含
# CREATE CACHED TABLE / U&'\4e2d\6587' / ALTER ... ADD CONSTRAINT 等
# H2 专有语法，MySQL 无法直接执行。本脚本用 H2 的 CSVWRITE 逐表导出 CSV，
# 再由 h2-to-mysql.py 生成列清单显式的 INSERT，只搬数据不碰结构。
#
# 目标库结构由 server/rdbms/src/main/resources/scripts/init-mysql.sql 负责。
#
# 用法：
#   server/db-export-mysql.sh              # 导出到 server/db-snapshot/wisestar-mysql.sql.gz
#   server/db-export-mysql.sh --plain      # 同时保留未压缩的 .sql（体积约 30MB）
#
# 注意：H2 文件库为单进程独占，导出前必须先停止预览后端。
#
# 恢复方式（MySQL 8）：
#   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS wisestar CHARACTER SET utf8mb4 COLLATE utf8mb4_bin"
#   mysql -u root -p wisestar < server/rdbms/src/main/resources/scripts/init-mysql.sql
#   gunzip -c server/db-snapshot/wisestar-mysql.sql.gz | mysql -u root -p wisestar

set -euo pipefail

SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$SERVER_DIR/api"
SNAP_DIR="$SERVER_DIR/db-snapshot"
DB_FILE="$API_DIR/wisestar.mv.db"
SCHEMA="$SERVER_DIR/rdbms/src/main/resources/scripts/init-mysql.sql"
PY="$SERVER_DIR/h2-to-mysql.py"
OUT_PLAIN="$SNAP_DIR/wisestar-mysql.sql"
OUT_GZ="$SNAP_DIR/wisestar-mysql.sql.gz"

KEEP_PLAIN=0
for arg in "$@"; do
  case "$arg" in
    --plain) KEEP_PLAIN=1 ;;
    *) echo "未知参数：$arg" >&2; exit 2 ;;
  esac
done

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

[ -f "$DB_FILE" ] || { echo "错误：未发现数据库文件 $DB_FILE。" >&2; exit 1; }
[ -f "$SCHEMA" ] || { echo "错误：未发现 MySQL 建表脚本 $SCHEMA。" >&2; exit 1; }

if ps -eo args 2>/dev/null | grep -E '[j]ava .*wisestar-v1\.9\.0\.jar' >/dev/null 2>&1; then
  echo "错误：预览后端正在运行，请先停止后端再导出。" >&2
  exit 1
fi

echo "==> 逐表导出 CSV 到临时目录"
cd "$API_DIR"
URL='jdbc:h2:file:./wisestar;MODE=MySQL;DATABASE_TO_LOWER=TRUE'
TABLES="$(java -cp "$H2_JAR" org.h2.tools.Shell \
  -url "$URL" -user sa -password "" \
  -sql "SELECT LOWER(TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='public' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME" \
  2>/dev/null | grep -E '^t_' || true)"

[ -n "$TABLES" ] || { echo "错误：未从 H2 读到任何业务表。" >&2; exit 1; }

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/wisestar-mysql.XXXXXX")"
cleanup() { rm -rf "$WORK_DIR"; }
trap cleanup EXIT

# CSVWRITE 对空串输出 `""`（带引号）、对 SQL NULL 输出空字段（不带引号），
# 转换脚本据此区分 NULL 与空串，无需哨兵值。
CSV_SQL="$WORK_DIR/export.sql"
: > "$CSV_SQL"
for t in $TABLES; do
  printf "CALL CSVWRITE('%s/%s.csv', 'SELECT * FROM %s', 'charset=UTF-8');\n" \
    "$WORK_DIR" "$t" "$t" >> "$CSV_SQL"
done

java -cp "$H2_JAR" org.h2.tools.RunScript \
  -url "$URL" -user sa -password "" -script "$CSV_SQL" >/dev/null

echo "==> CSV 共 $(ls "$WORK_DIR"/*.csv | wc -l) 个"

echo "==> 生成 MySQL data-only SQL"
cd "$SERVER_DIR"
python3 "$PY" "$WORK_DIR" "$SCHEMA" "$OUT_PLAIN"

echo "==> 压缩"
gzip -9 -c "$OUT_PLAIN" > "$OUT_GZ"
if [ "$KEEP_PLAIN" = "0" ]; then
  rm -f "$OUT_PLAIN"
fi

echo "已导出：$OUT_GZ ($(wc -c < "$OUT_GZ") bytes)"
[ "$KEEP_PLAIN" = "1" ] && echo "未压缩版本：$OUT_PLAIN ($(wc -c < "$OUT_PLAIN") bytes)"
echo
echo "导入 MySQL："
echo "  mysql -u root -p -e \"CREATE DATABASE IF NOT EXISTS wisestar CHARACTER SET utf8mb4 COLLATE utf8mb4_bin\""
echo "  mysql -u root -p wisestar < $SCHEMA"
echo "  gunzip -c $OUT_GZ | mysql -u root -p wisestar"
