package cn.wisestar.server.core.uitls;

import java.util.HashMap;
import java.util.Map;

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

	/**
	 * 为支持思维链的模型关闭思考并放宽输出上限。
	 *
	 * <p>智谱 GLM-4.5/4.7 系列默认开启思维链，思考过程与正文共用 {@code max_tokens}：
	 * 长 JSON 生成时容易把额度耗在思考上，最终正文为空。英语内容生成属于
	 * 「按给定约束填充结构化 JSON」的任务，关闭思考后既不会降低输出质量，
	 * 又能把额度全部留给正文，同时避免单次请求耗时失控。</p>
	 *
	 * <p>非智谱模型（如 SiliconFlow 上的 Qwen、DeepSeek）不识别该参数，
	 * 因此只在模型名匹配 {@code glm-4.5}/{@code glm-4.7} 时追加。</p>
	 *
	 * @param body  对话补全请求体（可直接修改）
	 * @param model 模型名，可空
	 */
	public static void applyReasoningControl(Map<String, Object> body, String model) {
		if (body == null || model == null) {
			return;
		}
		String name = model.trim().toLowerCase();
		if (!name.startsWith("glm-4.5") && !name.startsWith("glm-4.7")) {
			return;
		}
		Map<String, Object> thinking = new HashMap<>();
		thinking.put("type", "disabled");
		body.put("thinking", thinking);
		body.put("max_tokens", 16384);
	}

}
