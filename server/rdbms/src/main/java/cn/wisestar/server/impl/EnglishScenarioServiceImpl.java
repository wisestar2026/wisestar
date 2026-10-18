package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.AiPromptKeys;
import cn.wisestar.server.core.uitls.AiEndpointUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.english.EnglishScenarioLine;
import cn.wisestar.server.domain.dto.english.EnglishScenarioView;
import cn.wisestar.server.domain.dto.english.EnglishScenarioWord;
import cn.wisestar.server.service.EnglishScenarioService;
import cn.wisestar.server.service.SystemService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 英语情景对话生成服务实现（按分组子集生成）。
 *
 * <p>依据调用方给定的一组单元单词，调用系统 AI 设置（与 AI 问答/单元内容包同一套
 * enabled/token/models/baseUrl 配置）生成 3-4 个生活情景对话，把目标单词从英文台词中
 * 挖空形成填空练习。结果不落库，由「情景内容包服务」负责分组编排与固化。</p>
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

	/** AI 输出中用于标出目标词的标记，如 [[apple]] */
	private static final Pattern TARGET_MARKER = Pattern.compile("\\[\\[\\s*([^\\]]+?)\\s*\\]\\]?");

	/** 仅作句子骨架、不适合强行挖空的功能词 */
	private static final Set<String> FUNCTION_WORDS = new HashSet<>(Arrays.asList(
			"a", "an", "the", "i", "you", "he", "she", "it", "we", "they",
			"my", "your", "his", "her", "its", "our", "their", "me", "him", "us", "them",
			"this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been",
			"do", "does", "did", "have", "has", "had", "can", "could", "will", "would",
			"shall", "should", "may", "might", "must", "to", "of", "in", "on", "at",
			"for", "with", "from", "by", "about", "and", "or", "but", "so",
			"what", "where", "when", "who", "why", "how", "yes", "no", "not", "very", "too",
			"also", "here", "there", "up", "down",
			"mr", "ms", "mrs", "miss"));

	/** 生成结果 JSON 允许的场景数组 key 别名 */
	private static final String[] SCENARIO_ARRAY_KEYS = { "scenarios", "dialogs", "dialogues", "conversations" };

	/** 生成结果 JSON 允许的台词数组 key 别名 */
	private static final String[] LINE_ARRAY_KEYS = { "lines", "dialog", "turns", "messages" };

	private final SystemService systemService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public EnglishScenarioServiceImpl(SystemService systemService) {
		this.systemService = systemService;
		this.objectMapper = new ObjectMapper();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(15_000);
		factory.setReadTimeout(180_000);
		this.restTemplate = new RestTemplate(factory);
	}

	@Override
	public List<EnglishScenarioView> generateFromWords(String grade, String unit, List<EnglishScenarioWord> words) {
		requireNotBlank(grade, "年级");
		requireNotBlank(unit, "单元");
		if (words == null || words.isEmpty()) {
			return new ArrayList<>();
		}

		String raw = callChat(grade, unit, words);
		JsonNode root = parseRawJson(raw);
		return normalizeScenarios(root);
	}

	/**
	 * 调用大模型生成情景对话。
	 */
	private String callChat(String grade, String unit, List<EnglishScenarioWord> words) {
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
			messages.add(message("system", AiPromptKeys.resolve(aiSetting, AiPromptKeys.EN_SCENARIO, null)));
			messages.add(message("user", userPrompt));

			Map<String, Object> body = new HashMap<>();
			body.put("model", model);
			body.put("messages", messages);
			body.put("temperature", 0.3);
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
	 * 判断单词是否只由功能词构成（如 a、be (am,is,are)、to），这类词不适合强行挖空。
	 */
	private boolean isFunctionWord(String spell) {
		if (spell == null) {
			return true;
		}
		String normalized = spell.toLowerCase().replaceAll("[^a-z ]", " ").trim();
		if (normalized.isEmpty()) {
			return true;
		}
		for (String part : normalized.split("\\s+")) {
			if (!FUNCTION_WORDS.contains(part)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 构建请求 AI 生成情景对话的用户提示词。
	 */
	private String buildUserPrompt(String grade, String unit, List<EnglishScenarioWord> words, int scenarioCount) {
		StringBuilder sb = new StringBuilder();
		sb.append("请基于以下英语单元单词，编写 ").append(scenarioCount).append(" 个贴近生活的情景对话：\n");
		sb.append("年级：").append(grade).append("\n");
		sb.append("单元：").append(unit).append("\n");
		List<String> targets = new ArrayList<>();
		List<String> optional = new ArrayList<>();
		for (EnglishScenarioWord w : words) {
			String meaning = w.getMeaning() == null ? "" : w.getMeaning().trim();
			String item = meaning.isEmpty() ? w.getSpell() : w.getSpell() + "（" + meaning + "）";
			if (isFunctionWord(w.getSpell())) {
				optional.add(item);
			} else {
				targets.add(item);
			}
		}
		// 整组都是功能词时退回全量，避免无词可考
		if (targets.isEmpty()) {
			targets.addAll(optional);
			optional.clear();
		}
		sb.append("本组重点考查单词（务必全部考查到）：").append(String.join("、", targets));
		if (!optional.isEmpty()) {
			sb.append("\n本组也会自然出现的功能词（只用于把句子说通顺，不要为了考查而强行挖空）：")
					.append(String.join("、", optional));
		}
		sb.append("\n\n要求：\n");
		sb.append("1) 每个对话 4-8 轮，两个角色 A、B 交替你一言我一语，场景要生活化、自然连贯；\n");
		sb.append("2) 先写出完整、语法正确的英文句，再把每行要考查的 1 个目标词用 [[word]] 包裹；不要使用 ___；\n");
		sb.append("3) [[...]] 里的词必须逐字来自上面的重点考查单词；一行最多标记 1 个；没有目标词的行不要加任何标记；\n");
		sb.append("4) 每个对话至少自然用到 ").append(Math.min(3, targets.size()))
				.append(" 个重点单词；优先把目标词放在它天然的语法位置；\n");
		sb.append("5) 禁止连续重复词（a a / the the / is is / can can / to to 等）；同一个词不要在同一行标记两次；\n");
		sb.append("6) 标记后不得留下残句：主语、谓语、宾语等必要成分必须完整；宁可换一种说法，也不要硬塞目标词；\n");
		sb.append("7) 每行必须是通顺、正确、自然的英文；zh 为该行英文的中文翻译；\n");
		sb.append("8) answers 可以留空，系统会根据 [[...]] 自动生成。");
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
					Matcher marker = TARGET_MARKER.matcher(en);
					if (marker.find()) {
						// 标记模式：只保留第一个 [[word]] 作为空位，其余标记恢复原词，避免一行多空
						marker.reset();
						StringBuffer sb = new StringBuffer();
						while (marker.find()) {
							String word = marker.group(1).trim();
							if (answers.isEmpty() && !word.isEmpty()) {
								marker.appendReplacement(sb, Matcher.quoteReplacement(BLANK));
								answers.add(word.toLowerCase());
							} else {
								marker.appendReplacement(sb, Matcher.quoteReplacement(word));
							}
						}
						marker.appendTail(sb);
						en = sb.toString();
					}
					JsonNode rawAnswers = firstNode(rawLine, "answers", "words", "blanks");
					if (rawAnswers != null && rawAnswers.isArray()) {
						for (JsonNode a : rawAnswers) {
							if (a.isValueNode()) {
								answers.add(a.asText().trim().toLowerCase());
							}
						}
					}
					int blanks = countBlanks(en);
					if (!answers.isEmpty()) {
						// 标记模式已给出答案，忽略模型返回的 answers
						while (answers.size() > blanks) {
							answers.remove(answers.size() - 1);
						}
					}
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

}
