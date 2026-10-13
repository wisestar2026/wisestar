package cn.wisestar.server.core.uitls;

/**
 * AI 接口地址工具。
 *
 * <p>系统 AI 设置支持在「接口地址」中填写任意 OpenAI 兼容服务；本工具把用户填写的
 * 基础地址归一为完整的 {@code /v1/chat/completions} 对话补全地址，未填写时回退到
 * 默认的 SiliconFlow 地址。</p>
 *
 * @author wisestar
 * @date 2026/10/11
 */
public final class AiEndpointUtils {

	/** 默认对话补全地址（SiliconFlow） */
	public static final String DEFAULT_CHAT_URL = "https://api.siliconflow.cn/v1/chat/completions";

	private AiEndpointUtils() {
	}

	/**
	 * 依据基础地址解析对话补全地址。
	 *
	 * <p>支持填写：完整地址（含 /chat/completions）、以版本路径结尾的基础地址
	 * （如 {@code /v1}、{@code /v4}、{@code /v1beta}、{@code /compatible-mode/v1}、
	 * {@code /openai}）、或仅到域名；为空时使用默认 SiliconFlow 地址。</p>
	 *
	 * @param baseUrl 用户配置的基础地址，可空
	 * @return 完整的对话补全地址
	 */
	public static String resolveChatUrl(String baseUrl) {
		if (baseUrl == null || baseUrl.trim().isEmpty()) {
			return DEFAULT_CHAT_URL;
		}
		String url = baseUrl.trim();
		while (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		if (url.endsWith("/chat/completions")) {
			return url;
		}
		// 已含版本段（如 /v1、/v4、/v1beta、/compatible-mode/v1）时直接补 /chat/completions
		if (url.matches(".*/v\\d+[a-zA-Z]*$")) {
			return url + "/chat/completions";
		}
		// Gemini 的 OpenAI 兼容入口以 /openai 结尾
		if (url.endsWith("/openai")) {
			return url + "/chat/completions";
		}
		return url + "/v1/chat/completions";
	}

}
