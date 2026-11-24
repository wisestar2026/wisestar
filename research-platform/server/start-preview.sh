#!/usr/bin/env bash
# 启动教研平台后端（H2 文件库，首次启动自动执行幂等建表）。
#
# 用法：
#   research-platform/server/start-preview.sh
#
# 可选环境变量：
#   SERVER_PORT         监听端口，默认 7010
#   OPEN_RESEARCH_BASE_URL  远端只读接口地址，默认 http://127.0.0.1:1991
#   RESEARCH_OPEN_TOKEN     远端服务令牌，默认 dev-research-token

set -euo pipefail

SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SERVER_DIR/target/research-server.jar"

if [ ! -f "$JAR" ]; then
  echo "未找到 jar，正在构建..."
  (cd "$SERVER_DIR" && mvn -q clean package -DskipTests)
fi

mkdir -p "$SERVER_DIR/data"
cd "$SERVER_DIR"
exec java -jar "$JAR"
