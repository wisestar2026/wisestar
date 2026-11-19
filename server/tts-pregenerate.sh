#!/usr/bin/env bash
# 预生成英语课程音频缓存，并打包成可随部署分发的 tar.gz。
#
# 用法:
#   bash server/tts-pregenerate.sh [缓存目录] [tar 输出路径]
#
# 默认缓存目录: server/tts-cache（已 gitignore）
# 默认 tar:     server/tts-cache.tar.gz（已 gitignore）
#
# 说明:
#   - 扫描 t_english_sentence / t_english_word / t_english_read_aloud / t_english_grammar，
#     按学员端实际播放语速生成 mp3（句子/课文/语法 109，单词 109 与 125）。
#   - 已存在的缓存自动跳过；重复执行只补新增内容。
#   - preview profile 使用 H2 文件库，运行前需停止后端（H2 单进程独占）。
#   - 云上 MySQL 可用 SPRING_PROFILES_ACTIVE=pro 并照常提供 datasource 环境变量。
#   - 部署时把 tar 解压到 ENGLISH_TTS_CACHE_DIR，并保持音色路径与生成时一致。
set -euo pipefail

SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$SERVER_DIR/api"
SNAP_FILE="$SERVER_DIR/db-snapshot/wisestar.sql"
DB_FILE="$API_DIR/wisestar.mv.db"
JAR="$API_DIR/target/wisestar-v1.9.0.jar"

CACHE_DIR="${1:-${WISESTAR_TTS_PACK_DIR:-$SERVER_DIR/tts-cache}}"
TAR_FILE="${2:-${WISESTAR_TTS_PACK_TAR:-$SERVER_DIR/tts-cache.tar.gz}}"
PROFILE="${SPRING_PROFILES_ACTIVE:-preview}"
JAVA_BIN="${JAVA_BIN:-java}"
PREGENERATE_LIMIT="${TTS_PREGENERATE_LIMIT:-0}"
PREGENERATE_WORD_SPEEDS="${TTS_PREGENERATE_WORD_SPEEDS:-109,125}"

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
  echo "错误：未找到后端 jar $JAR" >&2
  echo "请先构建：cd server && mvn clean package -pl api -am -DskipTests" >&2
  exit 1
fi

mkdir -p "$CACHE_DIR"
CACHE_DIR="$(cd "$CACHE_DIR" && pwd)"
mkdir -p "$(dirname "$TAR_FILE")"

SQL_INIT_MODE=never
if [ "$PROFILE" = "preview" ]; then
  if command -v lsof >/dev/null 2>&1 && lsof "$DB_FILE" >/dev/null 2>&1; then
    echo "错误：H2 库正被后端占用，请先停止预览后端再预生成。" >&2
    exit 1
  fi
  if [ ! -f "$DB_FILE" ]; then
    if [ -f "$SNAP_FILE" ]; then
      H2_JAR="$(resolve_h2_jar)" || {
        echo "错误：需要从快照恢复，但找不到 h2 jar。可设置 H2_JAR 指定路径。" >&2
        exit 1
      }
      echo "未发现 H2 库，正在从快照恢复：$SNAP_FILE"
      "$JAVA_BIN" -cp "$H2_JAR" org.h2.tools.RunScript \
        -url "jdbc:h2:file:./wisestar;MODE=MySQL;DATABASE_TO_LOWER=TRUE" \
        -user sa -password "" -script "$SNAP_FILE"
      echo "快照恢复完成。"
    else
      SQL_INIT_MODE=always
    fi
  fi
fi

cd "$API_DIR"

echo "==> 预生成缓存：$CACHE_DIR"
echo "==> 使用配置：profile=$PROFILE，单词语速=$PREGENERATE_WORD_SPEEDS，limit=$PREGENERATE_LIMIT"

export SPRING_PROFILES_ACTIVE="$PROFILE"
export SPRING_SQL_INIT_MODE="$SQL_INIT_MODE"
export ENGLISH_TTS_CACHE_DIR="$CACHE_DIR"

# 用 JVM 系统属性传开关，保持 main args 为空，避免触发 DatabaseInitHelper 的交互式初始化。
"$JAVA_BIN" -Xms256m -Xmx768m \
  -Dspring.main.web-application-type=none \
  -Denglish.tts.pregenerate.enabled=true \
  -Denglish.tts.pregenerate.word-speeds="$PREGENERATE_WORD_SPEEDS" \
  -Denglish.tts.pregenerate.limit="$PREGENERATE_LIMIT" \
  -jar "$JAR"

COUNT="$(find "$CACHE_DIR" -maxdepth 1 -type f \( -name '*.mp3' -o -name '*.wav' \) | wc -l | tr -d ' ')"
SIZE="$(du -sh "$CACHE_DIR" | awk '{print $1}')"
if [ "$COUNT" = "0" ]; then
  echo "错误：缓存目录没有生成任何音频，请检查数据库内容与 TTS 日志。" >&2
  exit 1
fi

{
  echo "generated_at=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  echo "profile=$PROFILE"
  echo "audio_files=$COUNT"
  echo "cache_size=$SIZE"
  echo "word_speeds=$PREGENERATE_WORD_SPEEDS"
  echo "piper_voice=${ENGLISH_TTS_PIPER_VOICE:-/opt/wisestar-tts/voices/en_GB-jenny_dioco-medium.onnx}"
} > "$CACHE_DIR/pregenerate-manifest.txt"

tar -czf "$TAR_FILE" -C "$CACHE_DIR" .
echo
echo "完成：$COUNT 个音频，缓存目录 $SIZE"
echo "缓存目录：$CACHE_DIR"
echo "部署包：  $TAR_FILE"
echo
echo "部署到云服务器："
echo "  tar -xzf $(basename "$TAR_FILE") -C \"\$ENGLISH_TTS_CACHE_DIR\""
echo "并确保 ENGLISH_TTS_PIPER_VOICE 与生成时一致，否则缓存键对不上。"
