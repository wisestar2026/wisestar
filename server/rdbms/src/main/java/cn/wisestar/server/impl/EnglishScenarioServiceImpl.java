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

	/** 空位后直接跟字母（___s、___ed 等）会被拼成错误词形 */
	private static final Pattern BLANK_SUFFIX = Pattern.compile("_{2,}[A-Za-z]");

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

		Map<String, String> meanings = new HashMap<>();
		Set<String> allowed = new HashSet<>();
		for (EnglishScenarioWord word : words) {
			String spell = word.getSpell() == null ? "" : word.getSpell().trim().toLowerCase();
			if (spell.isEmpty()) {
				continue;
			}
			allowed.add(spell);
			String meaning = cleanMeaning(word.getMeaning());
			if (!meaning.isEmpty()) {
				meanings.put(spell, meaning);
			}
		}

		String raw = callChat(grade, unit, words);
		List<EnglishScenarioView> scenarios = normalizeScenarios(parseRawJson(raw), meanings, allowed);
		if (scenarios.isEmpty()) {
			// 首轮全部被质量闸门拦下时重试一次
			raw = callChat(grade, unit, words);
			scenarios = normalizeScenarios(parseRawJson(raw), meanings, allowed);
		}
		// 只有首轮被闸门拦下（场景/台词/空位不足）时才做二次校对，避免每单元都翻倍耗时
		if (!scenarios.isEmpty() && needsRepair(parseRawJson(raw), scenarios)) {
			List<EnglishScenarioView> repaired = normalizeScenarios(
					parseRawJson(repairChat(grade, unit, raw)), meanings, allowed);
			if (repaired.size() >= scenarios.size()) {
				scenarios = repaired;
			}
		}
		return scenarios;
	}

	private static final java.util.regex.Pattern POS_PREFIX = java.util.regex.Pattern
			.compile("(?i)^\\s*(?:v|n|adj|adv|prep|pron|conj|num|art|phr|abbr|aux|interj)\\.\\s*");

	/**
	 * 判断首轮结果是否需要二次校对：有场景/台词被闸门丢弃、或空位少于 3 个。
	 */
	private boolean needsRepair(JsonNode root, List<EnglishScenarioView> accepted) {
		JsonNode rawScenarios = firstNode(root, SCENARIO_ARRAY_KEYS);
		if (rawScenarios == null || !rawScenarios.isArray() || rawScenarios.size() > accepted.size()) {
			return true;
		}
		for (EnglishScenarioView view : accepted) {
			List<EnglishScenarioLine> lines = view.getLines();
			if (lines == null || lines.size() < 4) {
				return true;
			}
			int blanks = 0;
			for (EnglishScenarioLine line : lines) {
				blanks += line.getAnswers() == null ? 0 : line.getAnswers().size();
			}
			if (blanks < 3) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 二次校对：把首轮结果交回模型逐行纠错（语法、搭配、译文、连贯性），返回同结构 JSON。
	 */
	private String repairChat(String grade, String unit, String raw) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("下面是你为「").append(grade).append(" ").append(unit)
				.append("」编写的情景对话练习 JSON。请逐行校对并只修正有问题的部分：\n");
		prompt.append("1) 把缺省词填回 [[...]] 空位后，句子必须通顺、语法正确（时态、主谓一致、冠词、单复数、固定搭配）；\n");
		prompt.append("2) [[...]] 里的词必须来自本段 words 列表且拼写词形正确，禁止 memorys/kisss/hanged/feeded/tooths 这类生造词形；\n");
		prompt.append("3) zh 必须是该句准确、自然的中文，不得出现 [[ ]] 或英文单词；\n");
		prompt.append("4) 同一情景内台词要连贯、不重复，删掉与情景无关或前后矛盾的句子；不同情景之间不得复用相同台词；\n");
		prompt.append("5) 保持 JSON 结构、字段名、情景标题不变，输出完整 JSON，不要输出解释。\n");
		prompt.append("待校对 JSON：\n").append(raw);
		return postChat(prompt.toString(), 0.2);
	}

	/**
	 * 调用大模型生成情景对话。
	 */
	/**
	 * 词库释义常带词性前缀与多义项（如「v. 批评，抨击；袭击」），只取第一义项，避免污染中文译文。
	 */
	private String cleanMeaning(String meaning) {
		if (meaning == null) {
			return "";
		}
		String value = meaning.trim();
		int cut = value.indexOf('；');
		if (cut < 0) {
			cut = value.indexOf(';');
		}
		if (cut > 0) {
			value = value.substring(0, cut);
		}
		return POS_PREFIX.matcher(value).replaceAll("").trim();
	}

	private String callChat(String grade, String unit, List<EnglishScenarioWord> words) {
		int scenarioCount = words.size() >= 9 ? 4 : 3;
		return postChat(buildUserPrompt(grade, unit, words, scenarioCount), 0.3);
	}

	/**
	 * 调用大模型对话接口。
	 */
	private String postChat(String userPrompt, double temperature) {
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
			body.put("temperature", temperature);
			body.put("max_tokens", 8192);
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
			String meaning = cleanMeaning(w.getMeaning());
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
		sb.append("本组重点考查单词（组内多个对话合起来覆盖全部，单个对话只需自然用到 3-5 个）：")
				.append(String.join("、", targets));
		if (!optional.isEmpty()) {
			sb.append("\n本组也会自然出现的功能词（只用于把句子说通顺，不要为了考查而强行挖空）：")
					.append(String.join("、", optional));
		}
		sb.append("\n\n要求：\n");
		sb.append("1) 每个对话 6-10 轮，两个角色 A、B 交替你一言我一语，场景生活化、前后连贯，能读出完整的小情节；\n");
		sb.append("2) 先写出完整、语法正确的英文句，再把本行要考查的 1 个目标词用 [[word]] 包裹；不要使用 ___；\n");
		sb.append("3) [[...]] 里的词必须逐字来自上面的重点考查单词；一行最多标记 1 个；没有目标词的行不要加任何标记；\n");
		sb.append("4) 每个对话只挖 3-5 个空，其余台词必须是完整的自然英文；组内多个对话合起来覆盖本组单词；\n");
		sb.append("5) 挖空的那句要写成该词最常见、最像课本例句的句子（先想好搭配再写整句），"
				+ "某个词在当前情节里放不进去就换一个词，禁止为了塞词而硬造句子；\n");
		sb.append("5) 禁止生造词形（memorys、kisss、hanged、feeded、tooths、diarys、dishs、encourageing 等一律禁止），"
				+ "必须使用正确拼写和正确词形；\n");
		sb.append("6) 主谓一致、时态一致、冠词与单复数正确；禁止让 themselves/severally 这类词充当主语或生硬作表语；\n");
		sb.append("7) 禁止连续重复词（a a / the the / is is / can can / to to 等）；同一个词不要在同一行标记两次；\n");
		sb.append("8) 每行都必须是课本水平、通顺自然的英文，句子成分完整，不得出现残句或答非所问；\n");
		sb.append("9) zh 是该行英文的自然中文翻译，不得出现 [[ ]] 标记，也不得夹带英文单词；\n");
		sb.append("10) answers 可以留空，系统会根据 [[...]] 自动生成。");
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
	private List<EnglishScenarioView> normalizeScenarios(JsonNode root, Map<String, String> meanings,
			Set<String> allowed) {
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
				Set<String> bank = new HashSet<>(view.getWords());
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
					if (BLANK_SUFFIX.matcher(en).find()) {
						// 标记替换后空位直接跟字母（___s / ___ed）会被拼成错误词形，整行丢弃
						continue;
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
					boolean badAnswer = false;
					for (String answer : answers) {
						if (answer.isEmpty()
								|| (!allowed.isEmpty() && !matchesAllowed(answer, allowed))
								|| (!bank.isEmpty() && !matchesAllowed(answer, bank))) {
							badAnswer = true;
							break;
						}
					}
					if (badAnswer) {
						// 答案不在本段词库或拼写对不上，整行丢弃，避免出现无解空位
						continue;
					}

					EnglishScenarioLine line = new EnglishScenarioLine();
					String speaker = firstString(rawLine, "speaker", "role", "who", "name");
					line.setSpeaker((speaker == null || speaker.trim().isEmpty()) ? (idx % 2 == 0 ? "A" : "B") : speaker.trim());
					line.setEn(en);
					line.setZh(repairChinese(
							nullToEmpty(firstString(rawLine, "zh", "cn", "chinese", "translation")), meanings));
					line.setAnswers(answers);
					view.getLines().add(line);
					blankTotal += blanks;
					idx++;
				}
				if (blankTotal == 0 || view.getLines().size() < 4) {
					// 空位缺失或台词过少，无法作为练习，跳过
					continue;
				}
			}
			if (!view.getLines().isEmpty() && !hasDuplicateLines(result, view)) {
				result.add(view);
			}
		}
		return result;
	}

	/**
	 * 判断待加入的情景是否与已有情景重复（台词集合高度重合即视为重复）。
	 */
	private static boolean hasDuplicateLines(List<EnglishScenarioView> existing, EnglishScenarioView candidate) {
		List<String> keys = new ArrayList<>();
		for (EnglishScenarioLine line : candidate.getLines()) {
			if (line.getEn() != null && !line.getEn().trim().isEmpty()) {
				keys.add(line.getEn().trim().toLowerCase());
			}
		}
		if (keys.isEmpty()) {
			return false;
		}
		for (EnglishScenarioView view : existing) {
			int hit = 0;
			for (EnglishScenarioLine line : view.getLines()) {
				String en = line.getEn() == null ? "" : line.getEn().trim().toLowerCase();
				if (!en.isEmpty() && keys.contains(en)) {
					hit++;
				}
			}
			if (hit >= Math.max(2, keys.size() / 2)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 修复中文译文里残留的 [[word]] 标记：能查到词义就替换为中文词义，否则去掉标记只留单词。
	 */
	private static String repairChinese(String zh, Map<String, String> meanings) {
		if (zh == null || zh.isEmpty()) {
			return "";
		}
		Matcher matcher = TARGET_MARKER.matcher(zh);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String word = matcher.group(1).trim().toLowerCase();
			String meaning = meanings.get(word);
			matcher.appendReplacement(sb, Matcher.quoteReplacement(meaning == null ? word : meaning));
		}
		matcher.appendTail(sb);
		String repaired = sb.toString();
		if (repaired.contains("[") || repaired.contains("]")) {
			repaired = repaired.replace("[", "").replace("]", "");
		}
		return repaired.trim();
	}

	/**
	 * 判断答案是否属于本单元词表中的词（容忍常见词形变化）。
	 */
	private static boolean matchesAllowed(String answer, Set<String> allowed) {
		String word = answer.trim().toLowerCase();
		if (allowed.contains(word)) {
			return true;
		}
		for (String base : allowed) {
			if (base.length() < 3) {
				continue;
			}
			if (word.equals(base + "s") || word.equals(base + "es") || word.equals(base + "ed")
					|| word.equals(base + "d") || word.equals(base + "ing") || word.equals(base + "er")
					|| word.equals(base + "est")) {
				return true;
			}
			if (base.endsWith("y") && word.equals(base.substring(0, base.length() - 1) + "ies")) {
				return true;
			}
			if (base.endsWith("e") && word.equals(base.substring(0, base.length() - 1) + "ing")) {
				return true;
			}
		}
		return false;
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
