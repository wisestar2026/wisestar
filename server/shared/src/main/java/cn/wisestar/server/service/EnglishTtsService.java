package cn.wisestar.server.service;

import java.util.Collection;

/**
 * 英语朗读音频合成服务（服务端 TTS）。
 *
 * <p>学员端设备不一定装有英文语音包，浏览器语音合成会静默失败。这里由服务端合成并缓存短句音频，
 * 前端优先播放该音频，取不到时再退回浏览器语音合成。</p>
 *
 * @author wisestar
 * @date 2026/10/24
 */
public interface EnglishTtsService {

	/**
	 * 服务端合成是否可用（引擎存在且已启用）。
	 *
	 * @return true 表示可以合成
	 */
	boolean available();

	/**
	 * 合成英文短句音频。
	 *
	 * @param text  英文文本
	 * @param speed 语速（可选，空则用默认值）
	 * @return 音频字节；服务不可用或合成失败返回 null
	 */
	byte[] synthesize(String text, String speed);

	/**
	 * 预热一批文本：后台线程逐条合成并落缓存，重复调用会跳过已缓存的文本。
	 *
	 * @param texts 待合成文本
	 */
	default void warmUp(Collection<String> texts) {
		warmUp(texts, null);
	}

	/**
	 * 按指定语速预热一批文本。
	 *
	 * <p>缓存键包含语速，练习页按 125 播、课文按默认 109 播，语速对不上的预热等于白做。</p>
	 *
	 * @param texts 待合成文本
	 * @param speed 语速（空则用默认值）
	 */
	void warmUp(Collection<String> texts, String speed);

	/**
	 * 合成结果的音频类型，与 {@link #synthesize(String, String)} 返回内容一致。
	 *
	 * @return MIME 类型，如 audio/mpeg
	 */
	String contentType();
}
