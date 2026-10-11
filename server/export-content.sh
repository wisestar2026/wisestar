#!/usr/bin/env bash
# 导智慧星题库全部内容（章节/小节/知识点/题目/英语）与题目相关图片、音频。
#
# 产物默认输出到仓库根目录的 export/ 下：
#   export/math_structure.xlsx     知识点体系
#   export/math_questions.xlsx     全部题目（前 30 列可直接回导）
#   export/english_*.xlsx          英语单词/句子/语法
#   export/*.json                  结构化全量数据
#   export/files/                  题目配图、选项图、单词图片、音频
#   export/file_index.csv          引用地址 -> 本地文件对照表
#
# 用法：
#   server/export-content.sh [输出目录] [--all-files] [--english-media]
#
# 注意：H2 文件库为单进程独占，导出前必须先停止预览后端；
#       脚本先从数据库导出 CSV，再由 export_content.py 生成表格与拷贝文件。
#       --all-files      额外拷贝文件表中所有文件（不止被题目引用的）
#       --english-media  一并导出英语单词图片/音频（默认不导，可由 AI 重新生成）

set -euo pipefail

SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SERVER_DIR/.." && pwd)"
API_DIR="$SERVER_DIR/api"
DB_FILE="$API_DIR/wisestar.mv.db"
PY="$SERVER_DIR/export_content.py"

OUT_DIR="${1:-${EXPORT_OUT_DIR:-$REPO_ROOT/export}}"
shift || true

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

# H2 文件库为单进程独占，后端运行时无法从另一进程读取
if ps -eo args 2>/dev/null | grep -E '[j]ava .*wisestar-v1\.9\.0\.jar' >/dev/null 2>&1; then
  echo "错误：预览后端正在运行，请先停止后端再导出。" >&2
  exit 1
fi

RAW_DIR="$OUT_DIR/raw"
mkdir -p "$RAW_DIR"
cd "$API_DIR"

URL='jdbc:h2:file:./wisestar;MODE=MySQL;DATABASE_TO_LOWER=TRUE'

# 逐表导出为 UTF-8 CSV（CSVWRITE 内置函数）
TABLES=(
  t_subject t_subject_semester t_chapter t_chapter_repo t_section t_section_repo
  t_section_pass t_knowledge_point t_knowledge_point_question t_tag t_repo
  t_repo_template t_template t_file
  t_english_unit t_english_section t_english_word t_english_word_book
  t_english_sentence t_english_sentence_book t_english_grammar t_english_grammar_book
  t_english_ai_pack
)

SQL_FILE="$(mktemp)"
for t in "${TABLES[@]}"; do
  printf "CALL CSVWRITE('%s/%s.csv', 'SELECT * FROM %s', 'charset=UTF-8');\n" "$RAW_DIR" "$t" "$t" >> "$SQL_FILE"
done

java -cp "$H2_JAR" org.h2.tools.RunScript \
  -url "$URL" -user sa -password "" -script "$SQL_FILE" >/dev/null

: > "$SQL_FILE"

# 生成 Excel / JSON / 文件拷贝与对照表
cd "$REPO_ROOT"
python3 "$PY" "$OUT_DIR" "$@"

echo "导出完成：$OUT_DIR"
