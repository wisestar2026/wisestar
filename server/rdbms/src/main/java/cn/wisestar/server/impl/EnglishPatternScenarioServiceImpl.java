package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.AiEndpointUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.english.EnglishPatternScenarioView;
import cn.wisestar.server.domain.dto.english.EnglishPatternTurn;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.service.EnglishPatternScenarioService;
import cn.wisestar.server.service.SystemService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 英语重点句式情景生成服务实现。
 *
 * <p>依据指定 版本+年级+册别+单元 的重点句式（{@code t_english_sentence}），调用系统 AI 设置
 * （与 AI 问答/单元内容包同一套 enabled/token/models/baseUrl 配置）实时生成 3-4 个生活情景
 * 对话：给出部分英文台词，若干轮次仅给中文情景提示，由学员从候选项中选出符合情景的重点
 * 句式完成交际。结果不落库，每次调用按需重新生成。</p>
 *
 * @author wisestar
 * @date 2026/10/12
 */
@Slf4j
@Service
public class EnglishPatternScenarioServiceImpl implements EnglishPatternScenarioService {

	/** 未配置模型时的兜底模型 */
	private static final String DEFAULT_MODEL = "deepseek-chat";

	/** 生成结果 JSON 允许的场景数组 key 别名 */
	private static final String[] SCENARIO_ARRAY_KEYS = { "scenarios", "dialogs", "dialogues", "conversations" };

	/** 生成结果 JSON 允许的轮次数组 key 别名 */
	private static final String[] TURN_ARRAY_KEYS = { "turns", "lines", "dialog", "messages" };

	/** 生成结果 JSON 允许的候选项数组 key 别名 */
	private static final String[] OPTION_ARRAY_KEYS = { "options", "choices", "candidates" };

	private final EnglishSentenceMapper sentenceMapper;
	private final SystemService systemService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public EnglishPatternScenarioServiceImpl(EnglishSentenceMapper sentenceMapper, SystemService systemService) {
		this.sentenceMapper = sentenceMapper;
		this.systemService = systemService;
		this.objectMapper = new ObjectMapper();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(15_000);
		factory.setReadTimeout(180_000);
		this.restTemplate = new RestTemplate(factory);
	}

	@Override
	public List<EnglishPatternScenarioView> generateScenarios(String version, String grade, String term, String unit) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(unit, "单元");

		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(term != null && !term.trim().isEmpty(), EnglishSentence::getTerm, term)
				.eq(EnglishSentence::getUnit, unit)
				.orderByAsc(EnglishSentence::getSort));
		if (sentences.isEmpty()) {
			return new ArrayList<>();
		}

		String raw = callChat(grade, unit, sentences);
		JsonNode root = parseRawJson(raw);
		return normalizeScenarios(root);
	}

	/**
	 * 调用大模型生成句式情景。
	 */
	private String callChat(String grade, String unit, List<EnglishSentence> sentences) {
		SystemInfo.AiSetting aiSetting = systemService.getSystemAiSetting();
		if (aiSetting == null || aiSetting.getEnabled() == null || !aiSetting.getEnabled()) {
			throw new ValidationException("AI 服务未启用：请先在系统设置中开启 AI 并完成平台配置");
		}
		String token = aiSetting.getToken();
		if (token == null || token.trim().isEmpty()) {
			throw new ValidationException("AI Token 未配置：请先在系统设置中填写 AI 平台 Token");
		}

		String model = (aiSetting.getModels() != null && !aiSetting.getModels().isEmpty())
				? aiSetting.getModels().get(0)
				: DEFAULT_MODEL;

		int scenarioCount = sentences.size() >= 8 ? 4 : 3;
		String userPrompt = buildUserPrompt(grade, unit, sentences, scenarioCount);

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(token.trim());

			List<Map<String, String>> messages = new ArrayList<>();
			messages.add(message("system", SYSTEM_PROMPT));
			messages.add(message("user", userPrompt));

			Map<String, Object> body = new HashMap<>();
			body.put("model", model);
			body.put("messages", messages);
			body.put("temperature", 0.7);
			body.put("max_tokens", 4096);
			body.put("stream", false);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
			JsonNode resp = objectMapper.readTree(restTemplate.postForObject(
					AiEndpointUtils.resolveChatUrl(aiSetting.getBaseUrl()), request, String.class));
			JsonNode choices = resp.path("choices");
			if (choices.isArray() && choices.size() > 0) {
				String content = choices.get(0).path("message").path("content").asText("");
				if (!content.trim().isEmpty()) {
					return content;
				}
			}
			log.error("AI 返回异常结构：{}", writeJson(resp));
			throw new ValidationException("AI 返回内容为空，请稍后重试");
		} catch (ValidationException e) {
			throw e;
		} catch (Exception e) {
			log.error("AI 句式情景生成请求失败：{}", e.getMessage(), e);
			throw new ValidationException("AI 生成失败：" + e.getMessage());
		}
	}

	/**
	 * 构建请求 AI 生成句式情景的用户提示词。
	 */
	private String buildUserPrompt(String grade, String unit, List<EnglishSentence> sentences, int scenarioCount) {
		StringBuilder sb = new StringBuilder();
		sb.append("请基于以下英语单元的重点句式，编写 ").append(scenarioCount).append(" 个贴近生活的情景对话练习：\n");
		sb.append("年级：").append(grade).append("\n");
		sb.append("单元：").append(unit).append("\n");
		sb.append("本单元重点句式（answer 必须是其中之一，尽量全部覆盖）：\n");
		for (EnglishSentence s : sentences) {
			String en = s.getEn() == null ? "" : s.getEn().trim();
			if (en.isEmpty()) {
				continue;
			}
			String zh = s.getZh() == null ? "" : s.getZh().trim();
			sb.append("- ").append(en).append(zh.isEmpty() ? "" : "（" + zh + "）").append("\n");
		}
		sb.append("\n要求：\n");
		sb.append("1) 每个情景 4-8 轮，A、B 交替你一言我一语，场景生活化、自然连贯；\n");
		sb.append("2) 每个情景至少一半轮次为作答轮：只给 cue（中文情景提示），不给 en；\n");
		sb.append("3) 作答轮的 answer 必须是上面列出的重点句式之一，options 给出 4 个英文句子（含 answer 与 3 个明显不合本情景的干扰项）；\n");
		sb.append("4) 作答轮的 cue 用中文描述学员要表达的意思，与 answer 严格对应；已给出轮次必须有 en 与 zh；\n");
		sb.append("5) explanation 用简短中文说明选择依据。");
		return sb.toString();
	}

	/**
	 * 将模型返回内容解析为 JSON 节点（容忍 ```json 代码块包裹与前后噪声）。
	 */
	private JsonNode parseRawJson(String raw) {
		String text = raw.trim();
		if (text.contains("```")) {
			int first = text.indexOf("```");
			int last = text.lastIndexOf("```");
			if (last > first) {
				text = text.substring(first + 3, last).trim();
			}
			int newline = text.indexOf('\n');
			if (newline > 0) {
				String firstLine = text.substring(0, newline).trim();
				if (firstLine.startsWith("json")) {
					text = text.substring(newline + 1).trim();
				}
			}
		}
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start >= 0 && end > start) {
			text = text.substring(start, end + 1);
		}
		try {
			return objectMapper.readTree(text);
		} catch (Exception e) {
			log.error("AI 返回内容无法解析为 JSON：{}", text.length() > 500 ? text.substring(0, 500) : text);
			throw new ValidationException("AI 返回内容格式异常，请重新生成");
		}
	}

	/**
	 * 将模型 JSON 规整为句式情景列表：对齐选项与答案下标、过滤无效场景。
	 */
	private List<EnglishPatternScenarioView> normalizeScenarios(JsonNode root) {
		List<EnglishPatternScenarioView> result = new ArrayList<>();
		JsonNode rawScenarios = firstNode(root, SCENARIO_ARRAY_KEYS);
		if (rawScenarios == null || !rawScenarios.isArray()) {
			return result;
		}
		for (JsonNode raw : rawScenarios) {
			EnglishPatternScenarioView view = new EnglishPatternScenarioView();
			view.setTitle(firstString(raw, "title", "name", "scene"));
			view.setContext(nullToEmpty(firstString(raw, "context", "background", "desc", "description")));

			JsonNode rawPatterns = firstNode(raw, "patterns", "sentences", "patternList");
			if (rawPatterns != null && rawPatterns.isArray()) {
				for (JsonNode p : rawPatterns) {
					if (p.isValueNode() && !p.asText().trim().isEmpty()) {
						view.getPatterns().add(p.asText().trim());
					}
				}
			}

			JsonNode rawTurns = firstNode(raw, TURN_ARRAY_KEYS);
			if (rawTurns != null && rawTurns.isArray()) {
				int idx = 0;
				int answerTotal = 0;
				for (JsonNode rawTurn : rawTurns) {
					EnglishPatternTurn turn = normalizeTurn(rawTurn, idx);
					if (turn == null) {
						continue;
					}
					if (turn.getAnswerIndex() != null && turn.getAnswerIndex() >= 0) {
						answerTotal++;
					}
					view.getTurns().add(turn);
					idx++;
				}
				if (answerTotal == 0) {
					// 整个情景没有可作答轮次，无法作为练习，跳过
					continue;
				}
			}
			if (!view.getTurns().isEmpty()) {
				result.add(view);
			}
		}
		return result;
	}

	/**
	 * 规整单轮：区分「已给出轮次」与「作答轮次」，对齐答案与候选项。
	 *
	 * @return 规整后的轮次；无效（既无 en 又无可作答选项）返回 null
	 */
	private EnglishPatternTurn normalizeTurn(JsonNode rawTurn, int idx) {
		String speaker = firstString(rawTurn, "speaker", "role", "who", "name");
		String finalSpeaker = (speaker == null || speaker.trim().isEmpty()) ? (idx % 2 == 0 ? "A" : "B") : speaker.trim();

		String en = firstString(rawTurn, "en", "english", "text", "sentence");
		if (en != null && !en.trim().isEmpty()) {
			EnglishPatternTurn turn = new EnglishPatternTurn();
			turn.setSpeaker(finalSpeaker);
			turn.setEn(en.trim());
			turn.setZh(nullToEmpty(firstString(rawTurn, "zh", "cn", "chinese", "translation")));
			turn.setAnswerIndex(-1);
			return turn;
		}

		List<String> options = new ArrayList<>();
		JsonNode rawOptions = firstNode(rawTurn, OPTION_ARRAY_KEYS);
		if (rawOptions != null && rawOptions.isArray()) {
			for (JsonNode o : rawOptions) {
				if (o.isValueNode()) {
					String text = o.asText().trim();
					if (!text.isEmpty() && !containsIgnoreCase(options, text)) {
						options.add(text);
					}
				}
			}
		}
		String answer = firstString(rawTurn, "answer", "correct", "expected");
		int answerIndex = -1;
		if (answer != null && !answer.trim().isEmpty()) {
			answer = answer.trim();
			answerIndex = indexOfIgnoreCase(options, answer);
			if (answerIndex < 0) {
				options.add(answer);
				answerIndex = options.size() - 1;
			}
		}
		if (options.size() < 2 || answerIndex < 0) {
			return null;
		}

		EnglishPatternTurn turn = new EnglishPatternTurn();
		turn.setSpeaker(finalSpeaker);
		turn.setCue(nullToEmpty(firstString(rawTurn, "cue", "hint", "prompt", "zh", "cn", "chinese")));
		turn.setZh(nullToEmpty(firstString(rawTurn, "zh", "cn", "chinese")));
		turn.setOptions(options);
		turn.setAnswerIndex(answerIndex);
		turn.setExplanation(nullToEmpty(firstString(rawTurn, "explanation", "explain", "reason")));
		return turn;
	}

	private static int indexOfIgnoreCase(List<String> options, String target) {
		for (int i = 0; i < options.size(); i++) {
			if (options.get(i).trim().equalsIgnoreCase(target.trim())) {
				return i;
			}
		}
		return -1;
	}

	private static boolean containsIgnoreCase(List<String> options, String target) {
		return indexOfIgnoreCase(options, target) >= 0;
	}

	private Map<String, String> message(String role, String content) {
		Map<String, String> msg = new HashMap<>();
		msg.put("role", role);
		msg.put("content", content);
		return msg;
	}

	private String writeJson(JsonNode node) {
		try {
			return objectMapper.writeValueAsString(node);
		} catch (Exception e) {
			return "{}";
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static void requireNotBlank(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new ValidationException(name + "不能为空");
		}
	}

	private static String firstString(JsonNode node, String... keys) {
		JsonNode found = firstNode(node, keys);
		return found == null || !found.isValueNode() ? null : found.asText();
	}

	private static JsonNode firstNode(JsonNode node, String... keys) {
		if (node == null) {
			return null;
		}
		for (String key : keys) {
			JsonNode hit = node.get(key);
			if (hit != null) {
				return hit;
			}
		}
		List<String> lowerKeys = new ArrayList<>();
		for (String key : keys) {
			lowerKeys.add(key.toLowerCase(Locale.ROOT));
		}
		Iterator<String> fields = node.fieldNames();
		while (fields.hasNext()) {
			String name = fields.next();
			if (lowerKeys.contains(name.toLowerCase(Locale.ROOT))) {
				return node.get(name);
			}
		}
		return null;
	}

	private static final String SYSTEM_PROMPT =
			"你是一名资深小学英语教研员，擅长把单元重点句式编入真实生活情景对话。"
					+ "用户会给你：年级、单元，以及本单元要考查的重点句式。请据此编写情景对话练习，直接输出一个 JSON 对象，"
					+ "不要输出任何解释、前后缀或代码块之外的文字。\n"
					+ "JSON 结构必须严格如下：\n"
					+ "{\n"
					+ "  \"scenarios\": [\n"
					+ "    {\n"
					+ "      \"title\": \"情景标题(中文简短，如：开学第一天)\",\n"
					+ "      \"context\": \"一句话情景说明(中文)\",\n"
					+ "      \"patterns\": [\"What's your name?\", \"My name is Lingling.\"],\n"
					+ "      \"turns\": [\n"
					+ "        {\"speaker\": \"A\", \"en\": \"Hello!\", \"zh\": \"你好！\"},\n"
					+ "        {\"speaker\": \"B\", \"cue\": \"告诉对方你叫玲玲\", \"answer\": \"My name is Lingling.\", "
					+ "\"options\": [\"My name is Lingling.\", \"How old are you?\", \"It's my schoolbag.\", \"Goodbye.\"], "
					+ "\"explanation\": \"自我介绍用 My name is...\"}\n"
					+ "      ]\n"
					+ "    }\n"
					+ "  ]\n"
					+ "}\n"
					+ "要求：\n"
					+ "1) 每个情景 4-8 轮，A、B 交替，场景生活化、自然连贯；\n"
					+ "2) 至少一半轮次为作答轮：只给 cue，不给 en；answer 必须是给定重点句式之一；\n"
					+ "3) options 恰好 4 个，包含 answer 与 3 个明显不合本情景的干扰项，干扰项也要是合法的英文句子；\n"
					+ "4) 已给出轮次必须有 en 与 zh；cue 用中文描述学员要表达的意思；\n"
					+ "5) explanation 用简短中文说明选择依据；所有字符串中的双引号必须转义，输出必须是合法 JSON。";

}
