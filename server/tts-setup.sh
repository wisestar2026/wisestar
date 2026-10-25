#!/usr/bin/env bash
# 安装英语朗读音频的服务端合成环境（Piper 神经语音 + espeak-ng 回退 + lame 转码）
#
# 用法:
#   bash server/tts-setup.sh
#
# 幂等：已安装的组件会跳过。目录默认 /opt/wisestar-tts，可用环境变量覆盖：
#   WISESTAR_TTS_HOME / ENGLISH_TTS_VOICE
set -euo pipefail

TTS_HOME="${WISESTAR_TTS_HOME:-/opt/wisestar-tts}"
# 默认英式女声（小学课堂带读口径）；音色名格式 <语言_地区>-<说话人>-<质量>，
# 下载路径由音色名推导，换音色只需改这个变量（或环境变量 ENGLISH_TTS_VOICE）
VOICE_NAME="${ENGLISH_TTS_VOICE:-en_GB-jenny_dioco-medium}"
PIPER_DIR="${TTS_HOME}/venv"
VOICE_DIR="${TTS_HOME}/voices"

VOICE_LANG="${VOICE_NAME%%-*}"
VOICE_REST="${VOICE_NAME#*-}"
VOICE_SPEAKER="${VOICE_REST%-*}"
VOICE_QUALITY="${VOICE_REST##*-}"
VOICE_URL="https://huggingface.co/rhasspy/piper-voices/resolve/main/en/${VOICE_LANG}/${VOICE_SPEAKER}/${VOICE_QUALITY}"

echo "==> 安装系统依赖：espeak-ng（回退引擎）、lame（wav→mp3）"
if command -v apt-get >/dev/null 2>&1; then
	DEBIAN_FRONTEND=noninteractive apt-get install -y espeak-ng lame python3-venv >/dev/null
fi

echo "==> 准备 Piper 虚拟环境：${PIPER_DIR}"
if [ ! -x "${PIPER_DIR}/bin/piper" ]; then
	mkdir -p "${TTS_HOME}"
	python3 -m venv "${PIPER_DIR}"
	"${PIPER_DIR}/bin/pip" install --quiet --upgrade pip
	"${PIPER_DIR}/bin/pip" install --quiet piper-tts
fi

echo "==> 下载音色模型：${VOICE_NAME}"
mkdir -p "${VOICE_DIR}"
for suffix in onnx onnx.json; do
	file="${VOICE_DIR}/${VOICE_NAME}.${suffix}"
	if [ ! -f "${file}" ]; then
		curl -sL --fail --retry 3 -o "${file}" "${VOICE_URL}/${VOICE_NAME}.${suffix}"
	fi
done

echo "==> 自检"
echo "piper: $("${PIPER_DIR}/bin/piper" --help >/dev/null 2>&1 && echo OK || echo FAIL)"
echo "model: $(ls -lh "${VOICE_DIR}/${VOICE_NAME}.onnx" 2>/dev/null | awk '{print $5}')"
echo "espeak-ng: $(command -v espeak-ng || echo 缺失)"
echo "lame: $(command -v lame || echo 缺失)"
echo
echo "完成。后端配置（application.yml 的 english.tts.*）默认已指向："
echo "  piper-bin:   ${PIPER_DIR}/bin/piper"
echo "  piper-voice: ${VOICE_DIR}/${VOICE_NAME}.onnx"
