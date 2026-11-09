#!/usr/bin/env bash
# 恢复运行期文件数据（server/api/files/）。
#
# 背景：server/api/files/ 被 .gitignore 忽略（约 920MB / 2142 个文件），
# 全新环境 clone 后缺失，t_file 引用的图片经 GET /api/file?id= 会 404。
# 本脚本分两步恢复：
#   1. 从 GitHub Release 资产（一个 tar.gz 被切分为 9 片）下载、逐片
#      sha256 校验、按序合并并解压，覆盖 data-20260930 之前的存量文件；
#   2. 从仓库内 server/db-snapshot/media/ 复制 release 之后新增的非单词
#      图片（10 月的 12 个文件），避免这些「后加的 t_file 记录」在新环境 404。
#
# 单词配图（约 60MB）不在备份范围，由 AI 重新生成。
#
# 用法：
#   server/fetch-files.sh                  # 恢复到 server/api/files
#   server/fetch-files.sh /path/to/files   # 恢复到指定目录
#   server/fetch-files.sh --skip-release   # 只补 media（已有 release 数据时）
#
# 依赖：bash、curl、sha256sum、tar。
# 可选环境变量：WISESTAR_FILES_WORKDIR（下载/解压临时目录，默认 /tmp/wisestar-files）

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKIP_RELEASE=0
if [ "${1:-}" = "--skip-release" ]; then
  SKIP_RELEASE=1
  shift
fi
TARGET_DIR="${1:-$SCRIPT_DIR/api/files}"
WORK_DIR="${WISESTAR_FILES_WORKDIR:-/tmp/wisestar-files}"
MEDIA_DIR="$SCRIPT_DIR/db-snapshot/media"

RELEASE_TAG="data-20260930"
BASE="https://github.com/wisestar2026/wisestar/releases/download/${RELEASE_TAG}"
PREFIX="wisestar-files-20260929.tar.gz.part"

declare -A SUM=(
  [00]=26076145a2f6d8925dcff6029e5a1b858bb86918b27a51bf88605a2790a29901
  [01]=ddb3d019f33080439f9b3f9f19483e166a31d255f9039196c249fc2a4e6c95b3
  [02]=231fb897f05e4b995d12787bb36f39816f4f6b03b071ca7dd4f6e2b3802b58e8
  [03]=3e1d1faea17b6600beae013092de3a335439f951b4fbbc1c69f533ba30c66d6e
  [04]=2e84a915453df1fd18a6d55515086cb80cf10e90fe5f4fee64d197539091d66f
  [05]=61bdd60918cdcdfbf4886c5fcac1048ba62b9bf6cc9d64a6690f7a228c42972d
  [06]=ea3fb305ad3118582961430fa2436f34b9d0032a45f0bac94cdf9c40f5fdf079
  [07]=94cce91e4f58810083517d7b7dacb040486a575eb380cec1de15225cc105afe2
  [08]=c2e429403ca04390e965fc37c8262c5a56e0df9dedbae0723d052ea7323ff209
)
declare -A SIZE=(
  [00]=104857600 [01]=104857600 [02]=104857600 [03]=104857600
  [04]=104857600 [05]=104857600 [06]=104857600 [07]=104857600
  [08]=15824513
)

command -v curl >/dev/null || { echo "错误：缺少 curl" >&2; exit 1; }
command -v sha256sum >/dev/null || { echo "错误：缺少 sha256sum" >&2; exit 1; }

mkdir -p "$TARGET_DIR"

if [ "$SKIP_RELEASE" = "0" ]; then
  mkdir -p "$WORK_DIR"
  cd "$WORK_DIR"

  for i in 00 01 02 03 04 05 06 07 08; do
    f="${PREFIX}-$i"
    if [ -f "$f" ] && [ "$(sha256sum "$f" | awk '{print $1}')" = "${SUM[$i]}" ]; then
      echo "==> 跳过 $f（已校验）"
      continue
    fi
    if [ -f "$f" ] && [ "$(stat -c%s "$f")" -gt "${SIZE[$i]}" ]; then
      truncate -s 0 "$f"
    fi
    ok=0
    for attempt in 1 2 3 4 5 6; do
      echo "==> 下载 $f（第 $attempt 次）"
      # HTTP/1.1 + 断点续传 + 低速重连：本环境 GitHub HTTP/2 大文件下载会中途 stall
      if curl -fL --http1.1 --connect-timeout 20 --speed-limit 20480 --speed-time 30 \
          --max-time 1200 --retry 5 --retry-delay 2 --retry-all-errors \
          -C - -o "$f" "$BASE/$f"; then
        ok=1; break
      fi
      echo "    第 $attempt 次失败，3s 后重试" >&2
      sleep 3
    done
    [ "$ok" = "1" ] || { echo "错误：下载失败 $f" >&2; exit 1; }
    got="$(sha256sum "$f" | awk '{print $1}')"
    [ "$got" = "${SUM[$i]}" ] || { echo "错误：sha256 不匹配 $f (got=$got)" >&2; exit 1; }
    echo "    sha256 校验通过（$f）"
  done

  ARCHIVE="wisestar-files-20260929.tar.gz"
  cat "${PREFIX}-00" "${PREFIX}-01" "${PREFIX}-02" "${PREFIX}-03" "${PREFIX}-04" \
      "${PREFIX}-05" "${PREFIX}-06" "${PREFIX}-07" "${PREFIX}-08" > "$ARCHIVE"
  gzip -t "$ARCHIVE"
  echo "==> 压缩包完整：$ARCHIVE ($(wc -c < "$ARCHIVE") bytes)"

  rm -rf "$WORK_DIR/extract"
  mkdir -p "$WORK_DIR/extract"
  tar xzf "$ARCHIVE" -C "$WORK_DIR/extract"

  cp -a "$WORK_DIR/extract/files/." "$TARGET_DIR/"
  echo "==> 已恢复 $(ls "$TARGET_DIR" | wc -l) 个文件到 $TARGET_DIR"
fi

# release 之后新增的非单词图片（10 月上传，t_file 有记录但不在 release 内）。
if [ -d "$MEDIA_DIR" ] && [ -n "$(ls -A "$MEDIA_DIR" 2>/dev/null)" ]; then
  cp -a "$MEDIA_DIR/." "$TARGET_DIR/"
  echo "==> 已补齐 $(ls "$MEDIA_DIR" | wc -l) 个增量媒体文件（db-snapshot/media）"
fi

echo "==> 目标目录共 $(ls "$TARGET_DIR" | wc -l) 个文件：$TARGET_DIR"
