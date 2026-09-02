package cn.wisestar.server.service;

/**
 * AI 服务接口（TTS 语音 + DALL-E 图片 + GPT 文本）。
 *
 * @author wisestar
 * @date 2026/8/30
 */
public interface AiService {

	/**
	 * TTS 语音生成（生成单词发音音频）。
	 *
	 * @param text 要转换的文本（单词）
	 * @param lang 语言（en-US 英语 / zh-CN 中文）
	 * @return 音频文件 URL 或 Base64
	 */
	String generateSpeech(String text, String lang);

	/**
	 * DALL-E 图片生成（生成单词配图）。
	 *
	 * @param prompt 图片描述提示词
	 * @return 图片 URL
	 */
	String generateImage(String prompt);

	/**
	 * GPT 文本生成（生成例句/关联记忆法）。
	 *
	 * @param prompt 文本生成提示词
	 * @return 生成的文本内容
	 */
	String generateText(String prompt);

}
