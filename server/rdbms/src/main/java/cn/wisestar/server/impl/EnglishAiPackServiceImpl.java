package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AiPromptKeys;
import cn.wisestar.server.core.uitls.AiEndpointUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.english.EnglishAiPackQuery;
import cn.wisestar.server.domain.dto.english.EnglishAiPackView;
import cn.wisestar.server.domain.dto.english.EnglishGrammarView;
import cn.wisestar.server.domain.dto.english.PackSyncResult;
import cn.wisestar.server.domain.model.EnglishAiPack;
import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishAiPackMapper;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishAiPackService;
import cn.wisestar.server.service.SystemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 单元内容包服务实现。
 *
 * <p>功能：调用系统 AI 设置（SystemInfo.AiSetting：SiliconFlow 平台，与 AI 问答同一套
 * enabled/token/models 配置）为指定 版本+年级+单元 生成整套英语学习内容；生成结果先
 * 以内容包形式预览/保存，确认后再同步到正式词库（t_english_word）与语法库
 * （t_english_grammar），供后续学生端单词记忆、语法学习闭环使用。</p>
 *
 * @author wisestar
 * @date 2026/9/9
 */
@Slf4j
@Service
public class EnglishAiPackServiceImpl implements EnglishAiPackService {

	/** 未配置模型时的兜底模型 */
	private static final String DEFAULT_MODEL = "deepseek-chat";

	/** 生成结果 JSON 允许的 key 别名 */
	private static final String[] WORD_ARRAY_KEYS = { "words", "unitWords", "wordList", "wordsList" };

	private final EnglishAiPackMapper packMapper;
	private final EnglishWordMapper wordMapper;
	private final EnglishGrammarMapper grammarMapper;
	private final SystemService systemService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public EnglishAiPackServiceImpl(EnglishAiPackMapper packMapper, EnglishWordMapper wordMapper,
			EnglishGrammarMapper grammarMapper, SystemService systemService) {
		this.packMapper = packMapper;
		this.wordMapper = wordMapper;
		this.grammarMapper = grammarMapper;
		this.systemService = systemService;
		this.objectMapper = new ObjectMapper();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(15_000);
		factory.setReadTimeout(180_000);
		this.restTemplate = new RestTemplate(factory);
	}

	// ------------------------------------------------------------------ 查询

	@Override
	public PaginationResponse<EnglishAiPackView> listPacks(EnglishAiPackQuery query) {
		LambdaQueryWrapper<EnglishAiPack> wrapper = Wrappers.<EnglishAiPack>lambdaQuery()
				.eq(query.getVersion() != null, EnglishAiPack::getVersion, query.getVersion())
				.eq(query.getGrade() != null, EnglishAiPack::getGrade, query.getGrade())
				.eq(query.getUnit() != null, EnglishAiPack::getUnit, query.getUnit())
				.orderByDesc(EnglishAiPack::getCreateAt);

		Page<EnglishAiPack> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishAiPack> result = packMapper.selectPage(page, wrapper);

		List<EnglishAiPackView> views = result.getRecords().stream().map(pack -> {
			EnglishAiPackView view = toView(pack);
			// 列表不下发 content 正文，控制传输体积
			view.setContent(null);
			return view;
		}).collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
	}

	@Override
	public EnglishAiPackView getPack(String id) {
		EnglishAiPack pack = packMapper.selectById(id);
		if (pack == null) {
			throw new ValidationException("内容包不存在或已被删除");
		}
		return toView(pack);
	}

	@Override
	public void deletePack(String id) {
		packMapper.deleteById(id);
	}

	// ------------------------------------------------------------------ 生成

	@Override
	public EnglishAiPackView generateUnit(String version, String grade, String unit, String topic) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(unit, "单元");

		String raw = callChat(version, grade, unit, topic);
		JsonNode root = parseRawJson(raw);
		ObjectNode content = normalizeContent(root, version, grade, unit, topic);

		EnglishAiPackView view = new EnglishAiPackView();
		view.setVersion(version);
		view.setGrade(grade);
		view.setUnit(unit);
		view.setTopic(topic);
		view.setTitle(content.path("title").asText());
		view.setContent(writeJson(content));
		view.setWordCount(content.path("words").size());
		return view;
	}

	@Override
	public List<EnglishGrammarView> generateGrammarPreview(String version, String grade, String term, String unit) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(unit, "单元");

		List<EnglishGrammar> existing = grammarMapper.selectList(Wrappers.<EnglishGrammar>lambdaQuery()
				.eq(EnglishGrammar::getVersion, version)
				.eq(EnglishGrammar::getGrade, grade)
				.eq(StringUtils.hasText(term), EnglishGrammar::getTerm, term)
				.eq(EnglishGrammar::getUnit, unit)
				.orderByAsc(EnglishGrammar::getSort));
		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getUnit, unit)
				.orderByAsc(EnglishWord::getSpell));

		StringBuilder prompt = new StringBuilder();
		prompt.append("请为以下小学英语单元批量生成语法点教学内容：\n");
		prompt.append("教材版本：").append(version).append("\n");
		prompt.append("年级：").append(grade).append("\n");
		if (StringUtils.hasText(term)) {
			prompt.append("册别：").append(term).append("\n");
		}
		prompt.append("单元：").append(unit).append("\n");
		if (!words.isEmpty()) {
			prompt.append("本单元核心词汇：").append(words.stream().map(EnglishWord::getSpell)
					.limit(30).collect(Collectors.joining(", "))).append("\n");
		}
		if (!existing.isEmpty()) {
			prompt.append("已存在的语法点标题（请沿用这些标题，逐个补齐内容）：")
					.append(existing.stream().map(EnglishGrammar::getTitle).collect(Collectors.joining("；")))
					.append("\n请再补充 1-2 个本单元其他重点语法，总数 3-5 个。\n");
		}
		else {
			prompt.append("请梳理本单元 3-5 个重点语法点。\n");
		}

		String raw = callAi(GRAMMAR_SYSTEM_PROMPT, prompt.toString(), 0.45, 8192);
		JsonNode root = parseRawJson(raw);
		JsonNode array = firstNode(root, "grammars", "grammarPoints", "grammarList");
		if (array == null) {
			array = firstNode(root, "grammar", "grammarPoint");
		}
		List<EnglishGrammarView> result = new ArrayList<>();
		if (array == null) {
			return result;
		}
		List<JsonNode> nodes = new ArrayList<>();
		if (array.isArray()) {
			array.forEach(nodes::add);
		}
		else if (array.isObject()) {
			nodes.add(array);
		}
		Map<String, EnglishGrammar> byTitle = new HashMap<>();
		existing.forEach(item -> byTitle.put(normalizeTitle(item.getTitle()), item));
		int index = 0;
		for (JsonNode node : nodes) {
			if (node == null || !node.isObject()) {
				continue;
			}
			String title = firstString(node, "title", "name");
			if (!StringUtils.hasText(title)) {
				continue;
			}
			EnglishGrammar match = byTitle.get(normalizeTitle(title));
			EnglishGrammarView view = new EnglishGrammarView();
			view.setId(match == null ? null : match.getId());
			view.setTitle(title.trim());
			view.setContent(nullToEmpty(firstString(node, "content", "explanation", "detail")));
			view.setStructure(nullToEmpty(firstString(node, "structure", "pattern", "formula")));
			view.setTextbook(writeJson(normalizeGrammarItems(firstNode(node, "textbook", "textbookSentences", "texts"), true)));
			view.setSamples(writeJson(normalizeGrammarItems(firstNode(node, "samples", "examples", "exampleList"), false)));
			view.setExamples(view.getSamples());
			view.setExercises(writeJson(normalizeGrammarExercises(firstNode(node, "exercises", "practice", "quiz"))));
			view.setVersion(version);
			view.setGrade(grade);
			view.setTerm(term);
			view.setUnit(unit);
			view.setSection(match == null ? null : match.getSection());
			view.setSort(match == null ? existing.size() + index + 1 : match.getSort());
			result.add(view);
			index++;
		}
		if (result.isEmpty()) {
			throw new ValidationException("AI 未返回可用语法点，请重试");
		}
		return result;
	}

	/** 语法内容生成系统提示词。 */
	private static final String GRAMMAR_SYSTEM_PROMPT = "你是一名资深小学英语教研员，负责把单元语法讲清楚并配足练习。"
			+ "只输出 JSON 对象，不要输出解释或 Markdown 代码块。JSON 结构：\n"
			+ "{\"grammars\":[{\"title\":\"语法点标题\",\"content\":\"面向小学生的详细中文讲解，分 2-4 句，说明什么时候用、怎么变、常见错误\",\"structure\":\"句式结构或变化规则，如：主语 + be 动词 + 表语\",\"textbook\":[{\"en\":\"课本原句\",\"zh\":\"中文\",\"source\":\"Unit X Part A\"}],\"samples\":[{\"en\":\"典型例句\",\"zh\":\"中文\",\"note\":\"这句体现的语法规律\"}],\"exercises\":[{\"type\":\"choice\",\"q\":\"含空格的题干\",\"options\":[\"选项1\",\"选项2\",\"选项3\",\"选项4\"],\"answer\":\"正确选项文本\",\"explanation\":\"答案解析\"},{\"type\":\"build\",\"q\":\"中文提示\",\"answer\":\"完整英文句子\",\"explanation\":\"词序与语法说明\"}]}]}\n"
			+ "要求：textbook 恰好 3 句，尽量贴近该单元课本原句；samples 恰好 3 句并带 note；"
			+ "exercises 恰好 3 题，其中至少 2 题 choice（选正确单词/语法形态）、1 题 build（连词成句），答案与解析必须准确；"
			+ "所有题目要围绕该语法点，不要与单元单词题、句子题重复；英文必须自然、符合该年级水平；所有字符串中的双引号必须转义。";

	/** 将课本原文或例句规整为 [{en, zh, source/note}]。 */
	private ArrayNode normalizeGrammarItems(JsonNode raw, boolean textbook) {
		ArrayNode result = objectMapper.createArrayNode();
		if (raw == null) {
			return result;
		}
		List<JsonNode> items = new ArrayList<>();
		if (raw.isArray()) {
			raw.forEach(items::add);
		}
		else if (raw.isObject() || raw.isTextual()) {
			items.add(raw);
		}
		for (JsonNode item : items) {
			String en;
			String zh = "";
			if (item.isTextual()) {
				en = item.asText().trim();
			}
			else {
				en = nullToEmpty(firstString(item, "en", "english", "sentence", "text")).trim();
				zh = nullToEmpty(firstString(item, "zh", "chinese", "translation")).trim();
			}
			if (!StringUtils.hasText(en)) {
				continue;
			}
			ObjectNode node = result.addObject();
			node.put("en", en);
			node.put("zh", zh);
			if (item.isObject()) {
				if (textbook) {
					node.put("source", nullToEmpty(firstString(item, "source", "from", "part")));
				}
				else {
					node.put("note", nullToEmpty(firstString(item, "note", "explanation", "analysis")));
				}
			}
		}
		return result;
	}

	/** 将语法练习规整为 [{type, q, options, answer, explanation}]，兼容答案给字母的情况。 */
	private ArrayNode normalizeGrammarExercises(JsonNode raw) {
		ArrayNode result = objectMapper.createArrayNode();
		if (raw == null || !raw.isArray()) {
			return result;
		}
		for (JsonNode item : raw) {
			if (item == null || !item.isObject()) {
				continue;
			}
			String q = nullToEmpty(firstString(item, "q", "question", "stem")).trim();
			String answer = nullToEmpty(firstString(item, "answer", "correct", "key")).trim();
			ObjectNode exercise = result.addObject();
			ArrayNode options = exercise.putArray("options");
			JsonNode rawOptions = firstNode(item, "options", "choices");
			if (rawOptions != null && rawOptions.isArray()) {
				for (JsonNode option : rawOptions) {
					if (option.isTextual() && StringUtils.hasText(option.asText())) {
						options.add(option.asText().trim());
					}
				}
			}
			if (options.size() > 0 && answer.length() == 1) {
				int optionIndex = Character.toUpperCase(answer.charAt(0)) - 'A';
				if (optionIndex >= 0 && optionIndex < options.size()) {
					answer = options.get(optionIndex).asText();
				}
			}
			boolean build = options.isEmpty();
			exercise.put("type", build ? "build" : "choice");
			exercise.put("q", q);
			exercise.put("answer", answer);
			exercise.put("explanation", nullToEmpty(firstString(item, "explanation", "analysis", "note")));
		}
		return result;
	}

	/** 标题去重键：去空格并转小写。 */
	private String normalizeTitle(String title) {
		return title == null ? "" : title.replaceAll("\\s+", "").toLowerCase();
	}

	/**
	 * 调用大模型生成整套单元内容。
	 */
	private String callChat(String version, String grade, String unit, String topic) {
		SystemInfo.AiSetting aiSetting = systemService.getSystemAiSetting();
		if (aiSetting == null || aiSetting.getEnabled() == null || !aiSetting.getEnabled()) {
			throw new ValidationException("AI 服务未启用：请先在系统设置中开启 AI 并完成平台配置");
		}
		return callAi(AiPromptKeys.resolve(aiSetting, AiPromptKeys.EN_PACK, null),
				buildUserPrompt(version, grade, unit, topic), 0.5, 4096);
	}

	/** 通用 AI 调用：系统提示词 + 用户提示词，供内容包与语法点补齐复用。 */
	private String callAi(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
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
			messages.add(message("system", systemPrompt));
			messages.add(message("user", userPrompt));

			Map<String, Object> body = new HashMap<>();
			body.put("model", model);
			body.put("messages", messages);
			body.put("temperature", temperature);
			body.put("max_tokens", maxTokens);
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
			log.error("AI 内容生成请求失败：{}", e.getMessage(), e);
			throw new ValidationException("AI 生成失败：" + e.getMessage());
		}
	}

	/**
	 * 构建请求 AI 生成单元学习内容的用户提示词，并附带词库中已存在的本单元单词。
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param unit    单元
	 * @param topic   单元主题，可为空
	 * @return 拼接完成的提示词文本
	 */
	private String buildUserPrompt(String version, String grade, String unit, String topic) {
		StringBuilder sb = new StringBuilder();
		sb.append("请为以下英语单元生成整套学习内容：\n");
		sb.append("教材版本：").append(version).append("\n");
		sb.append("年级：").append(grade).append("\n");
		sb.append("单元：").append(unit).append("\n");
		if (topic != null && !topic.trim().isEmpty()) {
			sb.append("主题：").append(topic.trim()).append("\n");
		}

		// 带上词库中已存在的本单元单词，保证生成词表与既有词库衔接
		List<EnglishWord> existing = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getUnit, unit)
				.orderByAsc(EnglishWord::getSpell));
		if (!existing.isEmpty()) {
			String spells = existing.stream().map(EnglishWord::getSpell).collect(Collectors.joining(", "));
			sb.append("\n本单元词库中已有以下单词（务必全部包含在 words 中，并补全音标/释义/例句）：\n")
					.append(spells).append("\n");
		}

		sb.append("\n单词数量要求：总计 10 个左右。");
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
			// 去掉首行语言标记（如 json）
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
	 * 将模型 JSON 规整为落库内容结构：{title, words[], grammar{...}}。
	 */
	private ObjectNode normalizeContent(JsonNode root, String version, String grade, String unit, String topic) {
		ObjectNode content = objectMapper.createObjectNode();

		String fallbackTitle = (topic != null && !topic.trim().isEmpty())
				? topic.trim()
				: grade + " " + unit + " 单元学习内容";
		String title = firstString(root, "title", "unitTitle");
		content.put("title", (title == null || title.trim().isEmpty()) ? fallbackTitle : title.trim());

		// 单词表
		ArrayNode words = content.putArray("words");
		JsonNode rawWords = firstNode(root, WORD_ARRAY_KEYS);
		if (rawWords != null && rawWords.isArray()) {
			for (JsonNode w : rawWords) {
				String spell = firstString(w, "spell", "word");
				if (spell == null || spell.trim().isEmpty()) {
					continue;
				}
				ObjectNode word = words.addObject();
				word.put("spell", spell.trim().toLowerCase());
				word.put("phonetic", nullToEmpty(firstString(w, "phonetic", "symbol")));
				word.put("meaning", nullToEmpty(firstString(w, "meaning", "definition")));
				word.put("example", nullToEmpty(firstString(w, "example", "exampleSentence", "sentence")));
				word.put("exampleZh", nullToEmpty(firstString(w, "exampleZh", "exampleCn", "translation")));
			}
		}

		// 语法点
		JsonNode grammarNode = firstNode(root, "grammar", "grammarPoint");
		ObjectNode grammar = content.putObject("grammar");
		String gTitle = grammarNode == null ? null : firstString(grammarNode, "title", "name");
		grammar.put("title", (gTitle == null || gTitle.trim().isEmpty())
				? grade + " " + unit + " 语法重点"
				: gTitle.trim());
		grammar.put("content", grammarNode == null
				? ""
				: nullToEmpty(firstString(grammarNode, "content", "explanation", "detail")));
		grammar.put("structure", grammarNode == null
				? ""
				: nullToEmpty(firstString(grammarNode, "structure", "pattern", "formula")));
		grammar.set("textbook", normalizeGrammarItems(
				grammarNode == null ? null : firstNode(grammarNode, "textbook", "textbookSentences", "texts"), true));
		JsonNode rawSamples = grammarNode == null ? null
				: firstNode(grammarNode, "samples", "examples", "exampleList");
		grammar.set("samples", normalizeGrammarItems(rawSamples, false));
		ArrayNode examples = grammar.putArray("examples");
		if (grammarNode != null) {
			JsonNode rawExamples = firstNode(grammarNode, "examples", "exampleList");
			if (rawExamples != null && rawExamples.isArray()) {
				for (JsonNode e : rawExamples) {
					if (e.isObject()) {
						examples.addObject()
								.put("en", nullToEmpty(firstString(e, "en", "english", "sentence")))
								.put("zh", nullToEmpty(firstString(e, "zh", "chinese", "translation")));
					} else if (e.isTextual() && !e.asText().trim().isEmpty()) {
						examples.addObject().put("en", e.asText().trim()).put("zh", "");
					}
				}
			}
		}
		ArrayNode exercises = grammar.putArray("exercises");
		if (grammarNode != null) {
			JsonNode rawExercises = firstNode(grammarNode, "exercises", "practice", "quiz");
			if (rawExercises != null && rawExercises.isArray()) {
				for (JsonNode x : rawExercises) {
					ObjectNode ex = exercises.addObject();
					ex.put("q", nullToEmpty(firstString(x, "q", "question", "stem")));
					ArrayNode options = ex.putArray("options");
					JsonNode rawOptions = firstNode(x, "options", "choices");
					if (rawOptions != null && rawOptions.isArray()) {
						for (JsonNode o : rawOptions) {
							if (o.isTextual() && !o.asText().trim().isEmpty()) {
								options.add(o.asText().trim());
							}
						}
					}
					ex.put("answer", nullToEmpty(firstString(x, "answer", "correct", "key")));
					ex.put("explanation", nullToEmpty(firstString(x, "explanation", "analysis")));
				}
			}
		}
		return content;
	}

	// ------------------------------------------------------------------ 保存 / 同步

	@Override
	public EnglishAiPackView savePack(EnglishAiPackView pack) {
		if (pack == null || pack.getContent() == null || pack.getContent().trim().isEmpty()) {
			throw new ValidationException("请先生成内容再保存");
		}
		requireNotBlank(pack.getVersion(), "教材版本");
		requireNotBlank(pack.getGrade(), "年级");
		requireNotBlank(pack.getUnit(), "单元");

		// 同 版本+年级+单元 已存在内容包时覆盖更新，避免历史堆叠
		EnglishAiPack entity = packMapper.selectOne(Wrappers.<EnglishAiPack>lambdaQuery()
				.eq(EnglishAiPack::getVersion, pack.getVersion())
				.eq(EnglishAiPack::getGrade, pack.getGrade())
				.eq(EnglishAiPack::getUnit, pack.getUnit())
				.last("limit 1"));
		boolean update = entity != null;

		if (entity == null) {
			entity = new EnglishAiPack();
		}
		entity.setVersion(pack.getVersion());
		entity.setGrade(pack.getGrade());
		entity.setUnit(pack.getUnit());
		entity.setTopic(pack.getTopic());
		entity.setTitle(pack.getTitle());
		entity.setContent(pack.getContent());
		entity.setWordCount(pack.getWordCount() == null ? 0 : pack.getWordCount());

		if (update) {
			packMapper.updateById(entity);
		} else {
			packMapper.insert(entity);
		}
		return toView(packMapper.selectById(entity.getId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PackSyncResult syncToBank(String id) {
		EnglishAiPack pack = packMapper.selectById(id);
		if (pack == null) {
			throw new ValidationException("内容包不存在或已被删除");
		}

		JsonNode content;
		try {
			content = objectMapper.readTree(pack.getContent());
		} catch (Exception e) {
			throw new ValidationException("内容包 JSON 解析失败，无法同步");
		}

		int wordsAdded = 0;
		int wordsUpdated = 0;

		JsonNode words = content.path("words");
		if (words.isArray()) {
			for (JsonNode w : words) {
				String spell = w.path("spell").asText("");
				if (spell.trim().isEmpty()) {
					continue;
				}
				boolean exists = wordMapper.selectCount(Wrappers.<EnglishWord>lambdaQuery()
						.eq(EnglishWord::getVersion, pack.getVersion())
						.eq(EnglishWord::getGrade, pack.getGrade())
						.eq(EnglishWord::getUnit, pack.getUnit())
						.eq(EnglishWord::getSpell, spell.trim())) > 0;
				if (exists) {
					// 刷新释义/音标/例句
					EnglishWord patch = new EnglishWord();
					patch.setPhonetic(textOrNull(w.path("phonetic").asText()));
					patch.setMeaning(textOrNull(w.path("meaning").asText()));
					String example = w.path("example").asText("");
					if (example.trim().isEmpty()) {
						example = w.path("exampleZh").asText("");
					}
					patch.setExampleSentence(textOrNull(example));
					wordMapper.update(patch, Wrappers.<EnglishWord>lambdaUpdate()
							.eq(EnglishWord::getVersion, pack.getVersion())
							.eq(EnglishWord::getGrade, pack.getGrade())
							.eq(EnglishWord::getUnit, pack.getUnit())
							.eq(EnglishWord::getSpell, spell.trim()));
					wordsUpdated++;
				} else {
					EnglishWord word = new EnglishWord();
					word.setId(UUID.randomUUID().toString().replace("-", ""));
					word.setSpell(spell.trim().toLowerCase());
					word.setPhonetic(textOrNull(w.path("phonetic").asText()));
					word.setMeaning(textOrNull(w.path("meaning").asText()));
					String example = w.path("example").asText("");
					if (example.trim().isEmpty()) {
						example = w.path("exampleZh").asText("");
					}
					word.setExampleSentence(textOrNull(example));
					word.setVersion(pack.getVersion());
					word.setGrade(pack.getGrade());
					word.setUnit(pack.getUnit());
					wordMapper.insert(word);
					wordsAdded++;
				}
			}
		}

		// 语法库同步（按 版本+年级+学期+单元+小节+标题 覆盖更新）
		int grammarSynced = 0;
		JsonNode grammar = content.path("grammar");
		if (grammar.isObject()) {
			String gTitle = grammar.path("title").asText("");
			if (!gTitle.trim().isEmpty()) {
				EnglishGrammar exist = grammarMapper.selectOne(Wrappers.<EnglishGrammar>lambdaQuery()
						.eq(pack.getVersion() != null, EnglishGrammar::getVersion, pack.getVersion())
						.isNull(pack.getVersion() == null, EnglishGrammar::getVersion)
						.eq(pack.getGrade() != null, EnglishGrammar::getGrade, pack.getGrade())
						.isNull(pack.getGrade() == null, EnglishGrammar::getGrade)
						.eq(pack.getUnit() != null, EnglishGrammar::getUnit, pack.getUnit())
						.isNull(pack.getUnit() == null, EnglishGrammar::getUnit)
						.eq(EnglishGrammar::getTitle, gTitle.trim())
						.last("limit 1"));
				boolean isNew = exist == null;
				if (isNew) {
					exist = new EnglishGrammar();
				}
				exist.setVersion(pack.getVersion());
				exist.setGrade(pack.getGrade());
				exist.setUnit(pack.getUnit());
				exist.setTitle(gTitle.trim());
				exist.setContent(textOrNull(grammar.path("content").asText()));
				exist.setStructure(textOrNull(grammar.path("structure").asText()));
				exist.setTextbook(writeJson(grammar.path("textbook")));
				exist.setSamples(writeJson(grammar.path("samples")));
				exist.setExamples(writeJson(grammar.path("examples")));
				exist.setExercises(writeJson(grammar.path("exercises")));
				if (exist.getSort() == null) {
					exist.setSort(0);
				}
				if (isNew) {
					grammarMapper.insert(exist);
				} else {
					grammarMapper.updateById(exist);
				}
				grammarSynced = 1;
			}
		}
		return new PackSyncResult(wordsAdded, wordsUpdated, grammarSynced);
	}

	// ------------------------------------------------------------------ 辅助

	/**
	 * 构造单条聊天消息（角色 + 内容）。
	 *
	 * @param role    消息角色，如 system、user
	 * @param content 消息内容
	 * @return 包含 role、content 的消息 Map
	 */
	private Map<String, String> message(String role, String content) {
		Map<String, String> msg = new HashMap<>();
		msg.put("role", role);
		msg.put("content", content);
		return msg;
	}

	/**
	 * 将 JSON 节点序列化为字符串，序列化失败时返回空对象字符串。
	 *
	 * @param node 待序列化的 JSON 节点
	 * @return 序列化后的 JSON 字符串，失败时返回 {@code "{}"}
	 */
	private String writeJson(JsonNode node) {
		try {
			return objectMapper.writeValueAsString(node);
		} catch (Exception e) {
			return "{}";
		}
	}

	/**
	 * 将 null 字符串规整为空字符串，其它原样返回。
	 *
	 * @param s 待处理字符串
	 * @return 非 null 的字符串
	 */
	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	/**
	 * 去除首尾空白后返回文本，为空或仅含空白时返回 null。
	 *
	 * @param s 待处理字符串
	 * @return 规整后的文本，或 null
	 */
	private static String textOrNull(String s) {
		return (s == null || s.trim().isEmpty()) ? null : s.trim();
	}

	/**
	 * 校验字符串非空白，否则抛出校验异常。
	 *
	 * @param value 待校验的值
	 * @param name  字段名称，用于异常提示
	 */
	private static void requireNotBlank(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new ValidationException(name + "不能为空");
		}
	}

	/**
	 * 按候选键顺序读取第一个命中的字符串字段值。
	 *
	 * @param node JSON 节点
	 * @param keys 候选字段名（按优先级排列）
	 * @return 命中的字符串值，未命中或非值节点时返回 null
	 */
	private static String firstString(JsonNode node, String... keys) {
		JsonNode found = firstNode(node, keys);
		return found == null || !found.isValueNode() ? null : found.asText();
	}

	/**
	 * 按候选键读取第一个命中的子节点，先精确匹配再做大小写不敏感兜底。
	 *
	 * @param node JSON 节点
	 * @param keys 候选字段名（按优先级排列）
	 * @return 命中的子节点，未命中时返回 null
	 */
	private static JsonNode firstNode(JsonNode node, String... keys) {
		if (node == null) {
			return null;
		}
		Iterator<String> fields = node.fieldNames();
		// 先精确匹配
		for (String key : keys) {
			JsonNode hit = node.get(key);
			if (hit != null) {
				return hit;
			}
		}
		// 大小写不敏感兜底
		List<String> lowerKeys = new ArrayList<>();
		for (String key : keys) {
			lowerKeys.add(key.toLowerCase());
		}
		while (fields.hasNext()) {
			String name = fields.next();
			if (lowerKeys.contains(name.toLowerCase())) {
				return node.get(name);
			}
		}
		return null;
	}

	/**
	 * 将内容包实体转换为视图对象。
	 *
	 * @param pack 内容包实体
	 * @return 内容包视图
	 */
	private EnglishAiPackView toView(EnglishAiPack pack) {
		EnglishAiPackView view = new EnglishAiPackView();
		view.setId(pack.getId());
		view.setVersion(pack.getVersion());
		view.setGrade(pack.getGrade());
		view.setUnit(pack.getUnit());
		view.setTopic(pack.getTopic());
		view.setTitle(pack.getTitle());
		view.setContent(pack.getContent());
		view.setWordCount(pack.getWordCount());
		view.setCreateAt(pack.getCreateAt());
		return view;
	}

}
