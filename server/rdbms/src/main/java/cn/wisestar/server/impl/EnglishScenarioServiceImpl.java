package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.AiEndpointUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.english.EnglishScenarioLine;
import cn.wisestar.server.domain.dto.english.EnglishScenarioView;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishScenarioService;
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
import java.util.List;
import java.util.Map;

/**
 * 英语情景对话生成服务实现。
 *
 * <p>依据指定 版本+年级+册别+单元 的单词，调用系统 AI 设置（与 AI 问答/单元内容包同一套
 * enabled/token/models/baseUrl 配置）实时生成 3-4 个生活情景对话，把单元目标单词从英文
 * 台词中挖空形成填空练习。结果不落库，每次调用按需重新生成。</p>
 *
 * @author wisestar
 * @date 2026/10/12
 */
@Slf4j
@Service
public class EnglishScenarioServiceImpl implements EnglishScenarioService {

	/** 未配置模型时的兜底模型 */
	private static final String DEFAULT_MODEL = "deepseek-chat";

	/** 英文台词中的空位标记（统一为三个下划线） */
	private static final String BLANK = "___";

	/** 生成结果 JSON 允许的场景数组 key 别名 */
	private static final String[] SCENARIO_ARRAY_KEYS = { "scenarios", "dialogs", "dialogues", "conversations" };

	/** 生成结果 JSON 允许的台词数组 key 别名 */
	private static final String[] LINE_ARRAY_KEYS = { "lines", "dialog", "turns", "messages" };

	private final EnglishWordMapper wordMapper;
	private final SystemService systemService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public EnglishScenarioServiceImpl(EnglishWordMapper wordMapper, SystemService systemService) {
		this.wordMapper = wordMapper;
		this.systemService = systemService;
		this.objectMapper = new ObjectMapper();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(15_000);
		factory.setReadTimeout(180_000);
		this.restTemplate = new RestTemplate(factory);
	}

	@Override
	public List<EnglishScenarioView> generateScenarios(String version, String grade, String term, String unit) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(unit, "单元");

		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(term != null && !term.trim().isEmpty(), EnglishWord::getTerm, term)
				.eq(EnglishWord::getUnit, unit)
				.orderByAsc(EnglishWord::getSpell));
		if (words.isEmpty()) {
			return new ArrayList<>();
		}

		String raw = callChat(grade, unit, words);
		JsonNode root = parseRawJson(raw);
		return normalizeScenarios(root);
	}

	/**
	 * 调用大模型生成情景对话。
	 */
	private String callChat(String grade, String unit, List<EnglishWord> words) {
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

		int scenarioCount = words.size() >= 9 ? 4 : 3;
		String userPrompt = buildUserPrompt(grade, unit, words, scenarioCount);

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
			log.error("AI 情景对话生成请求失败：{}", e.getMessage(), e);
			throw new ValidationException("AI 生成失败：" + e.getMessage());
		}
	}

	/**
	 * 构建请求 AI 生成情景对话的用户提示词。
	 */
	private String buildUserPrompt(String grade, String unit, List<EnglishWord> words, int scenarioCount) {
		StringBuilder sb = new StringBuilder();
		sb.append("请基于以下英语单元单词，编写 ").append(scenarioCount).append(" 个贴近生活的情景对话：\n");
		sb.append("年级：").append(grade).append("\n");
		sb.append("单元：").append(unit).append("\n");
		sb.append("本单元单词（务必全部考查到）：");
		List<String> parts = new ArrayList<>();
		for (EnglishWord w : words) {
			String meaning = w.getMeaning() == null ? "" : w.getMeaning().trim();
			parts.add(meaning.isEmpty() ? w.getSpell() : w.getSpell() + "（" + meaning + "）");
		}
		sb.append(String.join("、", parts));
		sb.append("\n\n要求：\n");
		sb.append("1) 每个对话 4-8 轮，两个角色 A、B 交替你一言我一语，场景要生活化、自然连贯；\n");
		sb.append("2) 每个对话考查本单元若干单词：把要考查的单词从英文台词中挖空，用三个下划线 ___ 表示；\n");
		sb.append("3) 每行 en 里的 ___ 数量必须与 answers 数组长度一致，answers 按空位出现顺序给出小写单词；无空的台词 answers 为 []；\n");
		sb.append("4) 一行可有 0-2 个空，每个对话至少考查 3 个单词，所有给出单词尽量覆盖；\n");
		sb.append("5) zh 为该行英文的中文翻译。");
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
	 * 将模型 JSON 规整为情景对话列表：统一空位标记、对齐 answers、过滤无效场景。
	 */
	private List<EnglishScenarioView> normalizeScenarios(JsonNode root) {
		List<EnglishScenarioView> result = new ArrayList<>();
		JsonNode rawScenarios = firstNode(root, SCENARIO_ARRAY_KEYS);
		if (rawScenarios == null || !rawScenarios.isArray()) {
			return result;
		}
		for (JsonNode raw : rawScenarios) {
			EnglishScenarioView view = new EnglishScenarioView();
			view.setTitle(firstString(raw, "title", "name", "scene"));

			JsonNode rawWords = firstNode(raw, "words", "wordList");
			if (rawWords != null && rawWords.isArray()) {
				for (JsonNode w : rawWords) {
					if (w.isValueNode() && !w.asText().trim().isEmpty()) {
						view.getWords().add(w.asText().trim().toLowerCase());
					}
				}
			}

			JsonNode rawLines = firstNode(raw, LINE_ARRAY_KEYS);
			if (rawLines != null && rawLines.isArray()) {
				int idx = 0;
				int blankTotal = 0;
				for (JsonNode rawLine : rawLines) {
					String en = firstString(rawLine, "en", "english", "text", "sentence");
					if (en == null || en.trim().isEmpty()) {
						continue;
					}
					en = en.replaceAll("_{2,}", BLANK).trim();

					List<String> answers = new ArrayList<>();
					JsonNode rawAnswers = firstNode(rawLine, "answers", "words", "blanks");
					if (rawAnswers != null && rawAnswers.isArray()) {
						for (JsonNode a : rawAnswers) {
							if (a.isValueNode()) {
								answers.add(a.asText().trim().toLowerCase());
							}
						}
					}
					int blanks = countBlanks(en);
					// 对齐答案数量：不足补空、超出截断，避免前后端空位错位
					while (answers.size() < blanks) {
						answers.add("");
					}
					if (answers.size() > blanks) {
						answers = new ArrayList<>(answers.subList(0, blanks));
					}

					EnglishScenarioLine line = new EnglishScenarioLine();
					String speaker = firstString(rawLine, "speaker", "role", "who", "name");
					line.setSpeaker((speaker == null || speaker.trim().isEmpty()) ? (idx % 2 == 0 ? "A" : "B") : speaker.trim());
					line.setEn(en);
					line.setZh(nullToEmpty(firstString(rawLine, "zh", "cn", "chinese", "translation")));
					line.setAnswers(answers);
					view.getLines().add(line);
					blankTotal += blanks;
					idx++;
				}
				if (blankTotal == 0) {
					// 整个场景没有任何空位，无法作为练习，跳过
					continue;
				}
			}
			if (!view.getLines().isEmpty()) {
				result.add(view);
			}
		}
		return result;
	}

	/**
	 * 统计英文台词中的空位数量。
	 */
	private static int countBlanks(String en) {
		int count = 0;
		int from = 0;
		while (true) {
			int at = en.indexOf(BLANK, from);
			if (at < 0) {
				break;
			}
			count++;
			from = at + BLANK.length();
		}
		return count;
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
			lowerKeys.add(key.toLowerCase());
		}
		java.util.Iterator<String> fields = node.fieldNames();
		while (fields.hasNext()) {
			String name = fields.next();
			if (lowerKeys.contains(name.toLowerCase())) {
				return node.get(name);
			}
		}
		return null;
	}

	private static final String SYSTEM_PROMPT =
			"你是一名资深小学英语教研员，擅长把单元词汇编入真实生活情景对话。"
					+ "用户会给你：年级、单元，以及本单元要考查的单词。请据此编写情景对话练习，直接输出一个 JSON 对象，"
					+ "不要输出任何解释、前后缀或代码块之外的文字。\n"
					+ "JSON 结构必须严格如下：\n"
					+ "{\n"
					+ "  \"scenarios\": [\n"
					+ "    {\n"
					+ "      \"title\": \"情景标题(中文简短，如：在水果店)\",\n"
					+ "      \"words\": [\"apple\", \"buy\"],\n"
					+ "      \"lines\": [\n"
					+ "        {\"speaker\": \"A\", \"en\": \"I want to buy an ___.\", \"zh\": \"我想买一个苹果。\", \"answers\": [\"apple\"]},\n"
					+ "        {\"speaker\": \"B\", \"en\": \"OK, here you are.\", \"zh\": \"好的，给你。\", \"answers\": []}\n"
					+ "      ]\n"
					+ "    }\n"
					+ "  ]\n"
					+ "}\n"
					+ "要求：\n"
					+ "1) 对话自然、口语化、符合该年级水平；\n"
					+ "2) 被挖空的单词必须是给定单词表里的单词，用 ___ (三个下划线) 表示；\n"
					+ "3) 每行 en 的 ___ 数量必须与该行 answers 数组长度完全一致，answers 小写；\n"
					+ "4) 一行可有 0-2 个空，不要连续挖空相邻单词；\n"
					+ "5) 所有字符串中的双引号必须转义，输出必须是合法 JSON。";

}
