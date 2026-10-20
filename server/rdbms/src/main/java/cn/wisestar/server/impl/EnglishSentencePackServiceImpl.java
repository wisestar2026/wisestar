package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AiPromptKeys;
import cn.wisestar.server.core.uitls.AiEndpointUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.english.EnglishSentenceExercise;
import cn.wisestar.server.domain.dto.english.EnglishSentencePackQuery;
import cn.wisestar.server.domain.dto.english.EnglishSentencePackView;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishSentencePack;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishSentencePackMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.service.EnglishSentencePackService;
import cn.wisestar.server.service.SystemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 英语单元句子同步练习内容包服务实现（预生成 + 固化落库 + 学员端只读）。
 *
 * <p>管理端按 版本+年级+册别+单元 取出单元重点句子，按固定分组逐组调用系统 AI 生成
 * 多题型练习（连词成句 / 句子默写 / 句子选择），确认后以业务键覆盖保存；
 * 学员端仅读取固化内容，不再触发实时 AI。</p>
 *
 * @author wisestar
 * @date 2026/10/15
 */
@Slf4j
@Service
public class EnglishSentencePackServiceImpl implements EnglishSentencePackService {

	/** 句子练习最少题量 */
	private static final int PRACTICE_MIN = 20;

	/** 句子练习最多题量 */
	private static final int PRACTICE_MAX = 25;

	/** 补题目标题量 */
	private static final int PRACTICE_TARGET = 24;

	/** 句子学习最少题量 */
	private static final int LEARN_MIN = 10;

	/** 句子学习最多题量 */
	private static final int LEARN_MAX = 15;

	/** 题量不足时的 AI 重试次数 */
	private static final int AI_ATTEMPTS = 2;

	/** 未配置模型时的兜底模型 */
	private static final String DEFAULT_MODEL = "deepseek-chat";

	/** 生成结果 JSON 允许的题目数组 key 别名 */
	private static final String[] EXERCISE_ARRAY_KEYS = { "exercises", "questions", "items", "list" };

	private final EnglishSentencePackMapper packMapper;
	private final EnglishSentenceMapper sentenceMapper;
	private final EnglishWordMapper wordMapper;
	private final SystemService systemService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public EnglishSentencePackServiceImpl(EnglishSentencePackMapper packMapper, EnglishSentenceMapper sentenceMapper,
			EnglishWordMapper wordMapper, SystemService systemService) {
		this.packMapper = packMapper;
		this.sentenceMapper = sentenceMapper;
		this.wordMapper = wordMapper;
		this.systemService = systemService;
		this.objectMapper = new ObjectMapper();
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(15_000);
		factory.setReadTimeout(180_000);
		this.restTemplate = new RestTemplate(factory);
	}

	// ------------------------------------------------------------------ 生成

	@Override
	public EnglishSentencePackView generate(String version, String grade, String term, String unit) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(term, "册别");
		requireNotBlank(unit, "单元");

		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term)
				.eq(EnglishSentence::getUnit, unit)
				.orderByAsc(EnglishSentence::getSort));
		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getTerm, term)
				.eq(EnglishWord::getUnit, unit));

		EnglishSentencePackView view = new EnglishSentencePackView();
		view.setVersion(version);
		view.setGrade(grade);
		view.setTerm(term);
		view.setUnit(unit);

		List<EnglishSentenceExercise> practice = generatePractice(grade, unit, sentences, words);
		practice = supplementPractice(practice, sentences, words);
		List<EnglishSentenceExercise> learn = buildLearnItems(sentences, practice);
		if (learn.size() < LEARN_MIN) {
			List<EnglishSentenceExercise> aiLearn = generateAiLearn(grade, unit, sentences, words);
			mergeExercises(learn, aiLearn);
			learn = ensureLearnCoverage(learn, sentences);
			if (learn.size() < LEARN_MIN) {
				learn = fillLearnFromPractice(learn, practice);
			}
			learn = sortLearn(learn);
		}
		practice = supplementFromLearn(practice, learn);
		practice = sortPractice(practice);
		if (practice.size() > PRACTICE_MAX) {
			practice = new ArrayList<>(practice.subList(0, PRACTICE_MAX));
		}

		List<EnglishSentenceExercise> all = new ArrayList<>(learn);
		all.addAll(practice);
		view.setContent(writeExercises(all));
		view.setCount(all.size());
		log.info("句子内容包生成完成：{} {} {} {}，句子学习 {} 题，句子练习 {} 题", version, grade, term, unit,
				learn.size(), practice.size());
		return view;
	}

	/**
	 * 调用 AI 生成单元句子练习，最多尝试两次；两次结果合并后按主题限流。
	 */
	private List<EnglishSentenceExercise> generatePractice(String grade, String unit,
			List<EnglishSentence> sentences, List<EnglishWord> words) {
		List<EnglishSentenceExercise> merged = new ArrayList<>();
		if (sentences.isEmpty()) {
			return merged;
		}
		for (int attempt = 1; attempt <= AI_ATTEMPTS; attempt++) {
			try {
				String raw = callChat(AiPromptKeys.resolve(systemService.getSystemAiSetting(), AiPromptKeys.EN_SENTENCE, null),
						buildUserPrompt(grade, unit, sentences, words, attempt));
				List<EnglishSentenceExercise> part = normalizePractice(parseRawJson(raw), sentences, words);
				mergeExercises(merged, part);
				if (merged.size() >= PRACTICE_MIN) {
					break;
				}
				log.warn("句子练习第 {} 次生成仅 {} 题，继续补充生成", attempt, merged.size());
			} catch (ValidationException e) {
				log.warn("句子练习第 {} 次生成失败：{}", attempt, e.getMessage());
				if (attempt == AI_ATTEMPTS && merged.isEmpty()) {
					throw e;
				}
			}
		}
		List<EnglishSentenceExercise> result = new ArrayList<>();
		Map<String, Integer> themeCount = new HashMap<>();
		for (EnglishSentenceExercise ex : merged) {
			String theme = isBlank(ex.getTheme()) ? "句式" + (result.size() + 1) : ex.getTheme().trim();
			int count = themeCount.getOrDefault(theme, 0);
			if (count >= 2) {
				continue;
			}
			themeCount.put(theme, count + 1);
			ex.setTheme(theme);
			result.add(ex);
		}
		return result;
	}

	/**
	 * 构建请求 AI 生成句子练习的用户提示词。
	 */
	private String buildUserPrompt(String grade, String unit, List<EnglishSentence> sentences,
			List<EnglishWord> words, int attempt) {
		StringBuilder sb = new StringBuilder();
		sb.append("请为本单元编写句子练习（stage=practice）：\n");
		sb.append("年级：").append(grade).append("\n");
		sb.append("单元：").append(unit).append("\n");
		sb.append("本单元重点句式（source 字段填写前面的编号）：\n");
		for (int i = 0; i < sentences.size(); i++) {
			EnglishSentence s = sentences.get(i);
			String zh = s.getZh() == null ? "" : s.getZh().trim();
			sb.append("S").append(i + 1).append(". ").append(s.getEn().trim()).append("（").append(zh).append("）\n");
		}
		sb.append("本单元核心词汇：\n");
		int wordLimit = Math.min(words.size(), 24);
		for (int i = 0; i < wordLimit; i++) {
			EnglishWord w = words.get(i);
			if (isBlank(w.getSpell())) {
				continue;
			}
			sb.append("- ").append(w.getSpell().trim());
			if (!isBlank(w.getMeaning())) {
				sb.append("（").append(w.getMeaning().trim()).append("）");
			}
			sb.append("\n");
		}
		sb.append("\n硬性要求：\n");
		sb.append("1) 生成 20-25 道题，覆盖全部 S1-S").append(sentences.size()).append(" 句式，每个句式至少 1 道；\n");
		sb.append("2) kind 混用：choice/fill/build/dictation 四种都至少 3 道；\n");
		sb.append("3) 每道题的英文句至少包含 1 个本单元核心词汇；\n");
		sb.append("4) 同一 theme 最多 2 道题，请变换生活场景；\n");
		sb.append("5) 难度递进：1 基础（choice/fill）、2 提高（build）、3 挑战（dictation/灵活改写）；\n");
		sb.append("6) 可沿用原句或围绕该句式改写，改写须语法正确、符合小学").append(grade).append("水平；\n");
		sb.append("7) choice 的 options 恰好 4 个且含正确项；fill 的 stem 用 ___ 挖空，answer 必须在 en 中；\n");
		sb.append("8) 输出严格 JSON，不要解释。");
		if (attempt > 1) {
			sb.append("\n注意：上一次题量不足或主题重复，请补齐题量并更换场景。");
		}
		return sb.toString();
	}

	/**
	 * 调用大模型生成句子练习。
	 */
	private String callChat(String systemPrompt, String userPrompt) {
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
			body.put("temperature", 0.5);
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
			log.error("AI 句子练习生成请求失败：{}", e.getMessage(), e);
			throw new ValidationException("AI 生成失败：" + e.getMessage());
		}
	}

	/**
	 * 课本句式少于 10 个时，补充生成 10-15 道句子学习连词成句变式。
	 */
	private List<EnglishSentenceExercise> generateAiLearn(String grade, String unit,
			List<EnglishSentence> sentences, List<EnglishWord> words) {
		try {
			String raw = callChat(LEARN_SYSTEM_PROMPT, buildLearnPrompt(grade, unit, sentences, words));
			return normalizeLearn(parseRawJson(raw), sentences, words);
		} catch (ValidationException e) {
			log.warn("句子学习补充生成失败：{}", e.getMessage());
			return new ArrayList<>();
		}
	}

	private String buildLearnPrompt(String grade, String unit, List<EnglishSentence> sentences,
			List<EnglishWord> words) {
		StringBuilder sb = new StringBuilder();
		sb.append("请编写本单元「句子学习」连词成句（stage=learn，kind=build）：\n");
		sb.append("年级：").append(grade).append("，单元：").append(unit).append("\n");
		sb.append("课本重点句式：\n");
		for (int i = 0; i < sentences.size(); i++) {
			EnglishSentence s = sentences.get(i);
			sb.append("S").append(i + 1).append(". ").append(s.getEn().trim())
					.append("（").append(isBlank(s.getZh()) ? "" : s.getZh().trim()).append("）\n");
		}
		sb.append("本单元核心词汇：");
		for (int i = 0; i < Math.min(words.size(), 16); i++) {
			if (!isBlank(words.get(i).getSpell())) {
				sb.append(words.get(i).getSpell().trim()).append(" ");
			}
		}
		sb.append("\n要求：\n");
		sb.append("1) 生成 10-15 道，覆盖 S1-S").append(sentences.size()).append(" 全部句式；\n");
		sb.append("2) 以课本句式为骨架做替换练习：替换人物、物品、时间、地点，保持语法正确、符合小学").append(grade).append("水平；\n");
		sb.append("3) 每道题英文句至少使用 1 个单元核心词，句子长度由短到长，difficulty 1/2/3 递进；\n");
		sb.append("4) zh 为完整中文句意，en 为正确英文句子，source 填对应句式编号；\n");
		sb.append("5) en 只能包含英文、空格与英文标点，禁止中文、___ 或下划线占位；zh 必须完整翻译 en，不得省略主语或宾语；\n");
		sb.append("6) 只输出 JSON：{\"exercises\":[{\"stage\":\"learn\",\"kind\":\"build\",\"difficulty\":1,\"source\":1,\"zh\":\"……\",\"en\":\"……\"}]}，不要解释。");
		return sb.toString();
	}

	private List<EnglishSentenceExercise> normalizeLearn(JsonNode root, List<EnglishSentence> sentences,
			List<EnglishWord> words) {
		List<EnglishSentenceExercise> result = new ArrayList<>();
		JsonNode rawExercises = firstNode(root, EXERCISE_ARRAY_KEYS);
		if (rawExercises == null || !rawExercises.isArray()) {
			return result;
		}
		for (JsonNode raw : rawExercises) {
			String en = text(raw, "en", "sentence", "answer");
			if (!isUsableSentence(en)) {
				continue;
			}
			EnglishSentenceExercise ex = new EnglishSentenceExercise();
			ex.setStage("learn");
			ex.setKind("build");
			int difficulty = readInt(raw, "difficulty", difficultyOf(en));
			ex.setDifficulty(Math.max(1, Math.min(3, difficulty)));
			int source = readInt(raw, "source", 0);
			EnglishSentence matched = source >= 1 && source <= sentences.size() ? sentences.get(source - 1) : null;
			if (matched == null) {
				matched = findSourceContaining(sentences, en);
			}
			ex.setSentenceId(matched == null ? matchSentenceId(sentences, en) : matched.getId());
			String zh = text(raw, "zh", "cn", "prompt", "stem", "title");
			en = repairConjunction(en, zh);
			boolean exactSource = matched != null && normalizeKey(en).equals(normalizeKey(matched.getEn()));
			if (isBlank(zh) || !looksLikeSentencePrompt(zh)) {
				if (!exactSource) {
					continue;
				}
				zh = matched.getZh();
			}
			if (!exactSource && (zh.contains("___") || !isReasonableVariantZh(zh, en)
					|| hasMultipleSentences(en) || !zhCoversWord(zh, en, words))) {
				continue;
			}
			ex.setZh(zh);
			ex.setEn(en.trim());
			ex.setTheme("句式变式");
			result.add(ex);
		}
		return dedupeExercises(result);
	}

	/**
	 * 补齐课本句式覆盖，最多 15 道；按难度重排。
	 */
	private List<EnglishSentenceExercise> ensureLearnCoverage(List<EnglishSentenceExercise> list,
			List<EnglishSentence> sentences) {
		List<EnglishSentenceExercise> result = new ArrayList<>(list);
		Map<String, Boolean> exactSentences = new HashMap<>();
		for (EnglishSentenceExercise ex : result) {
			if (!isBlank(ex.getEn())) {
				exactSentences.put(normalizeKey(ex.getEn()), Boolean.TRUE);
			}
		}
		int insertAt = 0;
		for (EnglishSentence s : sentences) {
			if (isBlank(s.getEn()) || exactSentences.containsKey(normalizeKey(s.getEn()))) {
				continue;
			}
			EnglishSentenceExercise ex = new EnglishSentenceExercise();
			ex.setStage("learn");
			ex.setKind("build");
			ex.setTheme("课本句式");
			ex.setEn(s.getEn().trim());
			ex.setZh(s.getZh());
			ex.setDifficulty(difficultyOf(s.getEn()));
			ex.setSentenceId(s.getId());
			result.add(insertAt++, ex);
			exactSentences.put(normalizeKey(s.getEn()), Boolean.TRUE);
		}
		if (result.size() > LEARN_MAX) {
			result = new ArrayList<>(result.subList(0, LEARN_MAX));
		}
		assignLearnDifficulty(result);
		return result;
	}

	private List<EnglishSentenceExercise> supplementFromLearn(List<EnglishSentenceExercise> list,
			List<EnglishSentenceExercise> learn) {
		List<EnglishSentenceExercise> result = new ArrayList<>(list);
		Map<String, Boolean> keys = new HashMap<>();
		for (EnglishSentenceExercise ex : result) {
			keys.put(ex.getKind() + "|" + normalizeKey(ex.getEn() == null ? "" : ex.getEn()), Boolean.TRUE);
		}
		int index = 0;
		for (EnglishSentenceExercise source : learn) {
			if (isBlank(source.getEn()) || result.size() >= PRACTICE_MAX) {
				break;
			}
			int choiceCount = countKind(result, "choice");
			int fillCount = countKind(result, "fill");
			int buildCount = countKind(result, "build");
			int dictationCount = countKind(result, "dictation");
			if (choiceCount >= 3 && fillCount >= 3 && buildCount >= 3 && dictationCount >= 3
					&& result.size() >= PRACTICE_MIN) {
				break;
			}
			index++;
			if (choiceCount < 3 && !isBlank(source.getZh())) {
				EnglishSentenceExercise choice = new EnglishSentenceExercise();
				choice.setStage("practice");
				choice.setKind("choice");
				choice.setDifficulty(1);
				choice.setTheme("拓展选择" + index);
				choice.setZh("哪句英文是“" + source.getZh().trim() + "”？");
				choice.setEn(source.getEn());
				choice.setAnswer(source.getEn());
				choice.setSentenceId(source.getSentenceId());
				List<String> options = new ArrayList<>();
				options.add(source.getEn());
				for (EnglishSentenceExercise other : learn) {
					if (options.size() >= 4) {
						break;
					}
					if (!isBlank(other.getEn()) && isChoiceSentence(other.getEn()) && !options.contains(other.getEn())) {
						options.add(other.getEn());
					}
				}
				if (options.size() == 4) {
					choice.setOptions(options);
					addIfAbsent(result, keys, choice);
				}
			}
			if (fillCount < 3) {
				String word = pickFillWord(source.getEn());
				if (word != null) {
					EnglishSentenceExercise fill = new EnglishSentenceExercise();
					fill.setStage("practice");
					fill.setKind("fill");
					fill.setDifficulty(1);
					fill.setTheme("拓展填空" + index);
					fill.setZh(source.getZh());
					fill.setEn(source.getEn());
					fill.setAnswer(word);
					fill.setStem(source.getEn().replaceFirst("(?i)(?<![A-Za-z])"
							+ java.util.regex.Pattern.quote(word) + "(?![A-Za-z])", "___"));
					fill.setHint(word.substring(0, 1));
					fill.setSentenceId(source.getSentenceId());
					addIfAbsent(result, keys, fill);
				}
			}
			if (buildCount < 3) {
				EnglishSentenceExercise build = new EnglishSentenceExercise();
				build.setStage("practice");
				build.setKind("build");
				build.setDifficulty(2);
				build.setTheme("拓展句式" + index);
				build.setZh(source.getZh());
				build.setEn(source.getEn());
				build.setSentenceId(source.getSentenceId());
				addIfAbsent(result, keys, build);
			}
			if (dictationCount < 3) {
				EnglishSentenceExercise dictation = new EnglishSentenceExercise();
				dictation.setStage("practice");
				dictation.setKind("dictation");
				dictation.setDifficulty(3);
				dictation.setTheme("拓展听写" + index);
				dictation.setZh(source.getZh());
				dictation.setEn(source.getEn());
				dictation.setSentenceId(source.getSentenceId());
				addIfAbsent(result, keys, dictation);
			}
			if (result.size() < PRACTICE_MIN) {
				EnglishSentenceExercise build = new EnglishSentenceExercise();
				build.setStage("practice");
				build.setKind("build");
				build.setDifficulty(2);
				build.setTheme("拓展句式" + index);
				build.setZh(source.getZh());
				build.setEn(source.getEn());
				build.setSentenceId(source.getSentenceId());
				addIfAbsent(result, keys, build);
			}
			if (result.size() < PRACTICE_MIN) {
				EnglishSentenceExercise dictation = new EnglishSentenceExercise();
				dictation.setStage("practice");
				dictation.setKind("dictation");
				dictation.setDifficulty(3);
				dictation.setTheme("拓展听写" + index);
				dictation.setZh(source.getZh());
				dictation.setEn(source.getEn());
				dictation.setSentenceId(source.getSentenceId());
				addIfAbsent(result, keys, dictation);
			}
		}
		if (result.size() > PRACTICE_MAX) {
			result = new ArrayList<>(result.subList(0, PRACTICE_MAX));
		}
		return result;
	}

	private static final java.util.Set<String> STOP_WORDS = new java.util.HashSet<>(java.util.Arrays.asList(
			"the", "and", "are", "was", "were", "for", "from", "with", "you", "your", "this", "that", "these", "those",
			"have", "has", "had", "into", "out", "down", "over", "some", "there", "their", "they", "she", "him", "her"));

	private static final String LEARN_SYSTEM_PROMPT =
			"你是一名资深小学英语教研员。只输出 JSON 对象，不要解释："
					+ "{\"exercises\":[{\"stage\":\"learn\",\"kind\":\"build\",\"difficulty\":1,"
					+ "\"source\":1,\"zh\":\"中文句意\",\"en\":\"英文句子\"}]}";

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
	 * 规整模型 JSON 为练习题列表：归一题型与难度、校验选项与填空、回填来源句子 ID。
	 */
	private List<EnglishSentenceExercise> normalizePractice(JsonNode root, List<EnglishSentence> sentences,
			List<EnglishWord> words) {
		List<EnglishSentenceExercise> result = new ArrayList<>();
		JsonNode rawExercises = firstNode(root, EXERCISE_ARRAY_KEYS);
		if (rawExercises == null || !rawExercises.isArray()) {
			return result;
		}
		for (JsonNode raw : rawExercises) {
			String stage = text(raw, "stage");
			if ("learn".equalsIgnoreCase(stage)) {
				continue;
			}
			String en = text(raw, "en", "sentence", "answer");
			String zh = text(raw, "zh", "cn", "prompt", "stem", "title");
			en = repairConjunction(en, zh);
			if (isBlank(en)) {
				continue;
			}
			EnglishSentenceExercise ex = new EnglishSentenceExercise();
			ex.setStage("practice");
			ex.setEn(en);
			ex.setZh(zh);
			ex.setHint(text(raw, "hint"));
			ex.setExplanation(text(raw, "explanation", "analysis"));
			int difficulty = readInt(raw, "difficulty", difficultyOf(en));
			ex.setDifficulty(Math.max(1, Math.min(3, difficulty)));
			int source = readInt(raw, "source", 0);
			String sourceId = source >= 1 && source <= sentences.size() ? sentences.get(source - 1).getId() : null;
			ex.setSentenceId(sourceId != null ? sourceId : matchSentenceId(sentences, en));
			String theme = text(raw, "theme", "topic", "scene");
			ex.setTheme(isBlank(theme) ? "句式" + (source > 0 ? source : result.size() + 1) : theme);

			String kind = text(raw, "kind", "type");
			kind = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
			if (!"build".equals(kind) && !"dictation".equals(kind) && !"choice".equals(kind) && !"fill".equals(kind)) {
				kind = "build";
			}

			if ("choice".equals(kind)) {
				String answer = text(raw, "answer");
				if (isBlank(answer)) {
					answer = en;
				}
				List<String> options = readStringArray(raw, "options", "choices");
				List<String> four = normalizeOptions(options, answer);
				EnglishSentence choiceSource = findExactSource(sentences, answer);
				String choiceZh = coreChoiceZh(ex.getZh());
				boolean zhMatched = choiceSource != null
						|| (!isBlank(choiceZh) && looksLikeSentencePrompt(choiceZh)
								&& zhCoversWord(choiceZh, answer, words) && zhCoversVerb(choiceZh, answer)
								&& zhCoversNoun(choiceZh, answer));
				if (four.size() < 4 || !isChoiceSentence(answer)
						|| (choiceSource == null && (wordCount(answer) < 4 || !zhMatched))) {
					kind = fallbackKind(ex, sentences, answer);
				} else {
					ex.setOptions(four);
					ex.setAnswer(answer);
					ex.setEn(answer);
					if (choiceSource != null && !isBlank(choiceSource.getZh())) {
						ex.setZh("哪句英文是“" + choiceSource.getZh().trim() + "”？");
					} else if (!looksLikeChoicePrompt(ex.getZh())) {
						ex.setZh("哪句英文是“" + choiceZh + "”？");
					}
				}
			}

			if ("fill".equals(kind)) {
				String answer = text(raw, "answer");
				EnglishSentence sourceSentence = findSource(sentences, ex.getSentenceId());
				String fillEn = sourceSentence != null && !isBlank(sourceSentence.getEn())
						? sourceSentence.getEn().trim() : en;
				if (isBlank(answer) || !isUsableSentence(fillEn) || !containsWord(fillEn, answer)
						|| wordCount(answer) != 1 || STOP_WORDS.contains(answer.trim().toLowerCase(Locale.ROOT))
						|| wordCount(fillEn) - wordCount(answer) < 1) {
					kind = "build";
				} else {
					ex.setEn(fillEn);
					ex.setAnswer(answer);
					ex.setStem(fillEn.replaceFirst("(?i)(?<![A-Za-z])"
							+ java.util.regex.Pattern.quote(answer) + "(?![A-Za-z])", "___"));
					ex.setHint(answer.substring(0, 1));
					if (sourceSentence != null && !isBlank(sourceSentence.getZh())) {
						ex.setZh(sourceSentence.getZh().trim());
					} else if (isBlank(ex.getZh()) || !looksLikeSentencePrompt(ex.getZh())) {
						ex.setZh("根据句意补全句子。");
					}
				}
			}

			if (!isUsableSentence(ex.getEn())) {
				continue;
			}
			if ("build".equals(kind) || "dictation".equals(kind)) {
				EnglishSentence matched = findSource(sentences, ex.getSentenceId());
				boolean exactSource = matched != null && normalizeKey(ex.getEn()).equals(normalizeKey(matched.getEn()));
				if (exactSource) {
					ex.setZh(matched.getZh().trim());
				} else if (!ex.getZh().contains("___")
						&& isReasonableVariantZh(ex.getZh(), ex.getEn())
						&& !hasMultipleSentences(ex.getEn())
						&& zhCoversWord(ex.getZh(), ex.getEn(), words)) {
					ex.setZh(ex.getZh().replace("___", "").trim());
					if (hasBrokenPunctuation(ex.getZh())) {
						continue;
					}
				} else {
					continue;
				}
			}
			ex.setKind(kind);
			if (!"fill".equals(kind)) {
				ex.setAnswer(null);
				ex.setStem(null);
			}
			if (!"choice".equals(kind)) {
				ex.setOptions(null);
			}
			result.add(ex);
		}
		return dedupeExercises(result);
	}

	/**
	 * 选择题答案或选项不合格时，按答案长度退化为词汇填空或连词成句。
	 */
	private String fallbackKind(EnglishSentenceExercise ex, List<EnglishSentence> sentences, String answer) {
		EnglishSentence source = findSource(sentences, ex.getSentenceId());
		if (source == null) {
			source = findSourceContaining(sentences, answer);
		}
		if (source == null || isBlank(source.getEn())) {
			return "build";
		}
		ex.setSentenceId(source.getId());
		ex.setEn(source.getEn().trim());
		ex.setZh(source.getZh());
		if (wordCount(answer) <= 3 && containsWord(source.getEn(), answer)) {
			ex.setKind("fill");
			ex.setAnswer(answer);
			ex.setStem(source.getEn().replaceFirst("(?i)(?<![A-Za-z])" + java.util.regex.Pattern.quote(answer) + "(?![A-Za-z])", "___"));
			ex.setHint(answer.substring(0, 1));
			return "fill";
		}
		return "build";
	}

	private EnglishSentence findSource(List<EnglishSentence> sentences, String id) {
		if (isBlank(id)) {
			return null;
		}
		for (EnglishSentence s : sentences) {
			if (id.equals(s.getId())) {
				return s;
			}
		}
		return null;
	}

	private EnglishSentence findExactSource(List<EnglishSentence> sentences, String text) {
		if (isBlank(text)) {
			return null;
		}
		String key = normalizeKey(text);
		for (EnglishSentence s : sentences) {
			if (!isBlank(s.getEn()) && key.equals(normalizeKey(s.getEn()))) {
				return s;
			}
		}
		return null;
	}

	private EnglishSentence findSourceContaining(List<EnglishSentence> sentences, String text) {
		if (isBlank(text)) {
			return null;
		}
		for (EnglishSentence s : sentences) {
			if (!isBlank(s.getEn()) && containsWord(s.getEn(), text)) {
				return s;
			}
		}
		return null;
	}

	private boolean isUsableSentence(String en) {
		return !isBlank(en) && !en.contains("___") && wordCount(en) >= 2
				&& en.matches("[\\x20-\\x7E]+") && en.matches(".*[A-Za-z].*");
	}

	private boolean isChoiceSentence(String en) {
		if (!isUsableSentence(en)) {
			return false;
		}
		String text = en.trim();
		char first = text.charAt(0);
		if (Character.isUpperCase(first) || Character.isDigit(first) || first == '"' || first == '\'') {
			return true;
		}
		return wordCount(text) >= 4;
	}

	private boolean looksLikeChoicePrompt(String zh) {
		return zh.contains("哪") || zh.contains("？") || zh.contains("?") || zh.contains("选择") || zh.contains("意思");
	}

	private boolean looksLikeSentencePrompt(String zh) {
		return zh.length() >= 6 || zh.contains("。") || zh.contains("！") || zh.contains("？")
				|| zh.contains("，") || zh.contains(",");
	}

	private List<String> normalizeOptions(List<String> options, String answer) {
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		if (options != null) {
			for (String option : options) {
				if (!isBlank(option) && isChoiceSentence(option)) {
					unique.add(option.trim());
				}
			}
		}
		unique.add(answer);
		List<String> list = new ArrayList<>(unique);
		if (list.size() <= 4) {
			return list;
		}
		List<String> four = new ArrayList<>();
		four.add(answer);
		for (String option : list) {
			if (four.size() >= 4) {
				break;
			}
			if (!four.contains(option)) {
				four.add(option);
			}
		}
		return four;
	}

	private List<EnglishSentenceExercise> dedupeExercises(List<EnglishSentenceExercise> list) {
		Map<String, Boolean> seen = new HashMap<>();
		List<EnglishSentenceExercise> result = new ArrayList<>();
		for (EnglishSentenceExercise ex : list) {
			String key = (ex.getStage() == null ? "" : ex.getStage()) + "|" + ex.getKind() + "|"
					+ normalizeKey(ex.getEn() == null ? "" : ex.getEn()) + "|"
					+ normalizeKey(ex.getAnswer() == null ? "" : ex.getAnswer());
			if (seen.containsKey(key)) {
				continue;
			}
			seen.put(key, Boolean.TRUE);
			result.add(ex);
		}
		return result;
	}

	private void mergeExercises(List<EnglishSentenceExercise> target, List<EnglishSentenceExercise> incoming) {
		List<EnglishSentenceExercise> merged = new ArrayList<>(target);
		merged.addAll(incoming);
		List<EnglishSentenceExercise> deduped = dedupeExercises(merged);
		target.clear();
		target.addAll(deduped);
	}

	/**
	 * 题量不足时用课本句式补足：每个句式最多补 2 道，主题互不重复。
	 */
	private List<EnglishSentenceExercise> supplementPractice(List<EnglishSentenceExercise> list,
			List<EnglishSentence> sentences, List<EnglishWord> words) {
		List<EnglishSentenceExercise> result = new ArrayList<>(list);
		Map<String, Boolean> keys = new HashMap<>();
		for (EnglishSentenceExercise ex : result) {
			keys.put(ex.getKind() + "|" + normalizeKey(ex.getEn() == null ? "" : ex.getEn()), Boolean.TRUE);
		}
		for (int i = 0; i < sentences.size() && result.size() < PRACTICE_MAX; i++) {
			EnglishSentence s = sentences.get(i);
			if (isBlank(s.getEn())) {
				continue;
			}
			boolean covered = false;
			for (EnglishSentenceExercise ex : result) {
				if (s.getId() != null && s.getId().equals(ex.getSentenceId())) {
					covered = true;
					break;
				}
			}
			if (!covered || result.size() < PRACTICE_TARGET) {
				EnglishSentenceExercise build = new EnglishSentenceExercise();
				build.setStage("practice");
				build.setKind("build");
				build.setDifficulty(2);
				build.setTheme("课本句式" + (i + 1));
				build.setZh(s.getZh());
				build.setEn(s.getEn().trim());
				build.setSentenceId(s.getId());
				addIfAbsent(result, keys, build);
			}

			if (result.size() < PRACTICE_TARGET && sentences.size() >= 4) {
				EnglishSentenceExercise choice = new EnglishSentenceExercise();
				choice.setStage("practice");
				choice.setKind("choice");
				choice.setDifficulty(1);
				choice.setTheme("句式选择" + (i + 1));
				choice.setZh("哪句英文是“" + (isBlank(s.getZh()) ? "" : s.getZh().trim()) + "”？");
				choice.setEn(s.getEn().trim());
				choice.setAnswer(s.getEn().trim());
				choice.setSentenceId(s.getId());
				List<String> options = new ArrayList<>();
				options.add(s.getEn().trim());
				for (EnglishSentence other : sentences) {
					if (options.size() >= 4) {
						break;
					}
					if (!isBlank(other.getEn()) && !options.contains(other.getEn().trim())) {
						options.add(other.getEn().trim());
					}
				}
				if (options.size() == 4) {
					choice.setOptions(options);
					addIfAbsent(result, keys, choice);
				}
			}

			if (result.size() < PRACTICE_TARGET) {
				String word = firstWordIn(s.getEn(), words);
				if (word != null) {
					EnglishSentenceExercise fill = new EnglishSentenceExercise();
					fill.setStage("practice");
					fill.setKind("fill");
					fill.setDifficulty(1);
					fill.setTheme("词汇运用" + (i + 1));
					fill.setZh(s.getZh());
					fill.setEn(s.getEn().trim());
					fill.setAnswer(word);
					fill.setStem(s.getEn().replaceFirst("(?i)(?<![A-Za-z])" + java.util.regex.Pattern.quote(word) + "(?![A-Za-z])", "___"));
					fill.setHint(word.substring(0, 1));
					fill.setSentenceId(s.getId());
					addIfAbsent(result, keys, fill);
				}
				EnglishSentenceExercise dictation = new EnglishSentenceExercise();
				dictation.setStage("practice");
				dictation.setKind("dictation");
				dictation.setDifficulty(3);
				dictation.setTheme("句式听写" + (i + 1));
				dictation.setZh(s.getZh());
				dictation.setEn(s.getEn().trim());
				dictation.setSentenceId(s.getId());
				addIfAbsent(result, keys, dictation);
			}
		}
		return result;
	}

	private List<EnglishSentenceExercise> fillLearnFromPractice(List<EnglishSentenceExercise> learn,
			List<EnglishSentenceExercise> practice) {
		List<EnglishSentenceExercise> result = new ArrayList<>(learn);
		Map<String, Boolean> keys = new HashMap<>();
		for (EnglishSentenceExercise ex : result) {
			if (!isBlank(ex.getEn())) {
				keys.put(normalizeKey(ex.getEn()), Boolean.TRUE);
			}
		}
		for (EnglishSentenceExercise p : practice) {
			if (result.size() >= LEARN_MIN || isBlank(p.getEn()) || keys.containsKey(normalizeKey(p.getEn()))) {
				continue;
			}
			EnglishSentenceExercise ex = new EnglishSentenceExercise();
			ex.setStage("learn");
			ex.setKind("build");
			ex.setTheme("练习巩固");
			ex.setEn(p.getEn());
			ex.setZh(p.getZh());
			ex.setDifficulty(difficultyOf(p.getEn()));
			ex.setSentenceId(p.getSentenceId());
			keys.put(normalizeKey(p.getEn()), Boolean.TRUE);
			result.add(ex);
		}
		if (result.size() > LEARN_MAX) {
			result = new ArrayList<>(result.subList(0, LEARN_MAX));
		}
		assignLearnDifficulty(result);
		return result;
	}

	private boolean isReasonableVariantZh(String zh, String en) {
		return !isBlank(zh) && looksLikeSentencePrompt(zh) && !isQuestion(zh)
				&& !hasBrokenPunctuation(zh) && !hasDanglingVerb(zh) && conjunctionMatches(zh, en)
				&& zhCoversVerb(zh, en) && zhCoversNumber(zh, en);
	}

	private boolean zhCoversNumber(String zh, String en) {
		if (isBlank(zh) || isBlank(en)) {
			return true;
		}
		String lower = en.toLowerCase(Locale.ROOT);
		if (containsWord(lower, "first") && !zh.contains("第一")) {
			return false;
		}
		if (containsWord(lower, "second") && !zh.contains("第二")) {
			return false;
		}
		return !containsWord(lower, "third") || zh.contains("第三");
	}

	private boolean zhCoversVerb(String zh, String en) {
		if (isBlank(zh) || isBlank(en)) {
			return true;
		}
		String[][] verbs = {
				{"go", "去"}, {"went", "去"}, {"going", "去"},
				{"learn", "学"}, {"learned", "学"}, {"learning", "学"},
				{"ride", "骑"}, {"riding", "骑"}, {"rode", "骑"},
				{"fly", "飞"}, {"flying", "飞"}, {"flew", "飞"},
				{"start", "开始"}, {"starts", "开始"}, {"started", "开始"},
				{"sing", "唱"}, {"singing", "唱"}, {"sang", "唱"},
				{"draw", "画"}, {"drawing", "画"}, {"drew", "画"},
				{"bark", "叫"}, {"barks", "叫"}, {"barked", "叫"},
				{"climb", "爬"}, {"climbing", "爬"},
				{"swim", "游"}, {"swimming", "游"}, {"swam", "游"},
				{"rain", "雨"}, {"rains", "雨"},
				{"hear", "听"}, {"heard", "听"},
				{"notice", "注意"}, {"noticed", "注意"},
				{"arrive", "到"}, {"arrived", "到"},
				{"come", "来"}, {"came", "来"},
				{"run", "跑"}, {"running", "跑"}, {"ran", "跑"},
				{"jump", "跳"}, {"jumping", "跳"}, {"jumped", "跳"},
				{"buy", "买"}, {"bought", "买"}, {"buying", "买"},
				{"help", "帮"}, {"helps", "帮"}, {"helped", "帮"},
				{"ring", "响"}, {"rings", "响"}, {"rang", "响"},
				{"give", "给"}, {"gave", "给"},
				{"study", "学"}, {"studied", "学"},
				{"prepare", "准备"}, {"preparing", "准备"},
				{"paint", "画"}, {"painting", "画"},
				{"finish", "完"}, {"finished", "完"},
				{"stop", "停"}, {"stops", "停"}, {"stopped", "停"},
				{"hit", "撞"}, {"hits", "撞"},
				{"play", "玩|吹|弹|踢|打|演奏|进行"}, {"plays", "玩|吹|弹|踢|打|演奏|进行"},
				{"playing", "玩|吹|弹|踢|打|演奏|进行"}, {"played", "玩|吹|弹|踢|打|演奏"},
				{"have", "有|举办|吃|进行"}, {"has", "有|举办|吃|进行"},
				{"having", "有|举办|吃|进行"}, {"had", "有|举办|吃|进行"},
				{"read", "读|看"}, {"reads", "读|看"}, {"reading", "读|看"},
				{"open", "开"}, {"opens", "开"}, {"opened", "开"},
				{"close", "关"}, {"closes", "关"}, {"closed", "关"},
				{"dance", "跳|舞"}, {"dances", "跳|舞"}, {"dancing", "跳|舞"},
				{"kick", "踢"}, {"kicks", "踢"}, {"kicking", "踢"},
				{"hold", "举办|举行|拿"}, {"holds", "举办|举行|拿"}, {"holding", "举办|举行|拿"},
				{"eat", "吃"}, {"eats", "吃"}, {"eating", "吃"},
				{"drink", "喝"}, {"drinks", "喝"}, {"drinking", "喝"},
				{"sleep", "睡"}, {"sleeps", "睡"}, {"sleeping", "睡"},
				{"write", "写"}, {"writes", "写"}, {"writing", "写"},
				{"watch", "看"}, {"watches", "看"}, {"watching", "看"},
				{"look", "看"}, {"looks", "看"}, {"looking", "看"},
				{"clean", "打扫|清洁"}, {"cleans", "打扫|清洁"}, {"cleaning", "打扫|清洁"},
				{"cook", "做饭|煮"}, {"cooks", "做饭|煮"}, {"cooking", "做饭|煮"},
				{"laugh", "笑"}, {"laughs", "笑"}, {"laughing", "笑"},
				{"cry", "哭"}, {"cries", "哭"}, {"crying", "哭"},
				{"talk", "说|谈"}, {"talks", "说|谈"}, {"talking", "说|谈"},
				{"speak", "说"}, {"speaks", "说"}, {"speaking", "说"},
				{"sit", "坐"}, {"sits", "坐"}, {"sitting", "坐"},
				{"stand", "站"}, {"stands", "站"}, {"standing", "站"},
				{"wait", "等"}, {"waits", "等"}, {"waiting", "等"},
				{"find", "找|发现"}, {"finds", "找|发现"}, {"found", "找|发现"},
				{"fall", "掉|落"}, {"falls", "掉|落"}, {"fell", "掉|落"},
				{"wake", "醒"}, {"wakes", "醒"}, {"woke", "醒"}
		};
		for (String[] pair : verbs) {
			if (!containsWord(en, pair[0])) {
				continue;
			}
			boolean covered = false;
			for (String meaning : pair[1].split("\\|")) {
				if (zh.contains(meaning)) {
					covered = true;
					break;
				}
			}
			if (!covered) {
				return false;
			}
		}
		return true;
	}

	private boolean zhCoversNoun(String zh, String en) {
		if (isBlank(zh) || isBlank(en)) {
			return true;
		}
		String[][] nouns = {
				{"suona", "唢呐"}, {"piano", "钢琴"}, {"guitar", "吉他"}, {"violin", "小提琴"},
				{"drum", "鼓"}, {"flute", "笛"}, {"phone", "电话"}, {"door", "门"},
				{"dog", "狗"}, {"cat", "猫"}, {"bike", "自行车"}, {"bicycle", "自行车"},
				{"park", "公园"}, {"mountain", "山"}, {"birthday", "生日"}, {"party", "派对|聚会"},
				{"soccer", "足球"}, {"football", "足球"}, {"basketball", "篮球"},
				{"book", "书"}, {"music", "音乐"}, {"homework", "作业"}, {"picture", "图片|画"},
				{"flower", "花"}, {"tree", "树"}, {"bird", "鸟"}, {"fish", "鱼"}, {"horse", "马"},
				{"alarm", "闹钟"}, {"clock", "钟"}, {"store", "商店"}, {"shop", "商店"},
				{"school", "学校"}, {"teacher", "老师"}, {"friend", "朋友"}, {"family", "家庭|家人"},
				{"mother", "妈妈|母亲"}, {"father", "爸爸|父亲"}, {"weather", "天气"},
				{"rain", "雨"}, {"snow", "雪"}, {"sun", "太阳"}, {"moon", "月亮"},
				{"star", "星星"}, {"wind", "风"}, {"cloud", "云"}
		};
		for (String[] pair : nouns) {
			if (!containsWord(en, pair[0])) {
				continue;
			}
			boolean covered = false;
			for (String meaning : pair[1].split("\\|")) {
				if (zh.contains(meaning)) {
					covered = true;
					break;
				}
			}
			if (!covered) {
				return false;
			}
		}
		return true;
	}

	private boolean hasDanglingVerb(String zh) {
		if (isBlank(zh)) {
			return false;
		}
		return zh.matches(".*(开始|变得|成为|进行|出现|发生|很|非常|太)[，,。！？!?]?$")
				|| zh.matches(".*(开始|变得|成为)[，,].*");
	}

	private boolean conjunctionMatches(String zh, String en) {
		if (isBlank(zh) || isBlank(en)) {
			return true;
		}
		String lower = en.toLowerCase(Locale.ROOT);
		if (zh.contains("但是") && !lower.contains("but")) {
			return false;
		}
		return !zh.contains("因为") || lower.contains("because");
	}

	private boolean hasBrokenPunctuation(String zh) {
		return !isBlank(zh) && zh.matches(".*[，,、][。！？!?].*");
	}

	private boolean hasMultipleSentences(String en) {
		if (isBlank(en)) {
			return false;
		}
		String text = en.trim().replaceAll("[.!?]+$", "");
		return text.matches(".*[.!?].*");
	}

	private String repairConjunction(String en, String zh) {
		if (isBlank(en)) {
			return en;
		}
		String text = en.replaceAll(",\\s*", ", ").trim();
		if (!isBlank(zh) && zh.contains("但是") && !text.toLowerCase(Locale.ROOT).contains("but")) {
			text = text.replaceFirst("(?i)\\bbecause\\b", "but");
		}
		return text;
	}

	private boolean isQuestion(String zh) {
		if (isBlank(zh)) {
			return false;
		}
		String text = zh.trim();
		return text.endsWith("？") || text.endsWith("?");
	}

	private String coreChoiceZh(String zh) {
		if (isBlank(zh)) {
			return "";
		}
		String text = zh.trim();
		int first = Math.max(text.indexOf('“'), text.indexOf('"'));
		int last = Math.max(text.lastIndexOf('”'), text.lastIndexOf('"'));
		if (first >= 0 && last > first) {
			return text.substring(first + 1, last);
		}
		return text;
	}

	private boolean zhCoversWord(String zh, String en, List<EnglishWord> words) {
		if (isBlank(zh) || isBlank(en) || words == null) {
			return true;
		}
		for (EnglishWord word : words) {
			if (isBlank(word.getSpell()) || !containsWord(en, word.getSpell())) {
				continue;
			}
			String meaning = firstChinese(word.getMeaning());
			if (meaning != null) {
				return zh.contains(meaning);
			}
		}
		return true;
	}

	private String firstChinese(String text) {
		if (isBlank(text)) {
			return null;
		}
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\u4e00-\u9fa5]{1,6}").matcher(text);
		return m.find() ? m.group() : null;
	}

	private String pickFillWord(String en) {
		if (isBlank(en)) {
			return null;
		}
		String best = null;
		for (String token : en.split("[^A-Za-z']+")) {
			if (token.length() >= 3 && !STOP_WORDS.contains(token.toLowerCase())) {
				if (best == null || token.length() > best.length()) {
					best = token;
				}
			}
		}
		return best;
	}

	private int countKind(List<EnglishSentenceExercise> list, String kind) {
		int count = 0;
		for (EnglishSentenceExercise ex : list) {
			if (kind.equals(ex.getKind())) {
				count++;
			}
		}
		return count;
	}

	private void addIfAbsent(List<EnglishSentenceExercise> list, Map<String, Boolean> keys, EnglishSentenceExercise ex) {
		String key = ex.getKind() + "|" + normalizeKey(ex.getEn());
		if (!keys.containsKey(key)) {
			keys.put(key, Boolean.TRUE);
			list.add(ex);
		}
	}

	private String firstWordIn(String en, List<EnglishWord> words) {
		for (EnglishWord w : words) {
			String spell = w.getSpell();
			if (!isBlank(spell) && containsWord(en, spell.trim())) {
				return spell.trim();
			}
		}
		return null;
	}

	/**
	 * 构建 10-15 道「句子学习」连词成句：优先覆盖全部课本句式，不足时用练习题补足。
	 */
	private List<EnglishSentenceExercise> buildLearnItems(List<EnglishSentence> sentences,
			List<EnglishSentenceExercise> practice) {
		List<List<EnglishSentence>> groups = new ArrayList<>();
		for (EnglishSentence s : sentences) {
			if (!isBlank(s.getEn())) {
				List<EnglishSentence> one = new ArrayList<>();
				one.add(s);
				groups.add(one);
			}
		}
		while (groups.size() > LEARN_MAX) {
			int best = 0;
			int bestLength = Integer.MAX_VALUE;
			for (int i = 0; i < groups.size() - 1; i++) {
				int length = groupWords(groups.get(i)) + groupWords(groups.get(i + 1));
				if (length < bestLength) {
					bestLength = length;
					best = i;
				}
			}
			List<EnglishSentence> merged = new ArrayList<>(groups.get(best));
			merged.addAll(groups.get(best + 1));
			groups.set(best, merged);
			groups.remove(best + 1);
		}

		List<EnglishSentenceExercise> result = new ArrayList<>();
		for (List<EnglishSentence> group : groups) {
			StringBuilder en = new StringBuilder();
			StringBuilder zh = new StringBuilder();
			for (EnglishSentence s : group) {
				if (en.length() > 0) {
					en.append(' ');
					zh.append(' ');
				}
				en.append(s.getEn().trim());
				if (!isBlank(s.getZh())) {
					zh.append(s.getZh().trim());
				}
			}
			EnglishSentenceExercise ex = new EnglishSentenceExercise();
			ex.setStage("learn");
			ex.setKind("build");
			ex.setTheme("课本句式");
			ex.setEn(en.toString());
			ex.setZh(zh.toString());
			ex.setDifficulty(difficultyOf(en.toString()));
			ex.setSentenceId(group.get(0).getId());
			result.add(ex);
		}

		Map<String, Boolean> seen = new HashMap<>();
		for (EnglishSentenceExercise ex : result) {
			seen.put(normalizeKey(ex.getEn()), Boolean.TRUE);
		}
		for (EnglishSentenceExercise p : practice) {
			if (result.size() >= LEARN_MIN) {
				break;
			}
			if (!"build".equals(p.getKind()) || !isUsableSentence(p.getEn())
					|| seen.containsKey(normalizeKey(p.getEn()))) {
				continue;
			}
			String candidate = normalizeKey(p.getEn());
			boolean redundant = false;
			for (String existing : seen.keySet()) {
				if (candidate.contains(existing) || existing.contains(candidate)) {
					redundant = true;
					break;
				}
			}
			if (redundant) {
				continue;
			}
			EnglishSentenceExercise ex = new EnglishSentenceExercise();
			ex.setStage("learn");
			ex.setKind("build");
			ex.setTheme(p.getTheme());
			ex.setEn(p.getEn());
			ex.setZh(p.getZh());
			ex.setDifficulty(difficultyOf(p.getEn()));
			ex.setSentenceId(p.getSentenceId());
			ex.setHint(p.getHint());
			seen.put(normalizeKey(ex.getEn()), Boolean.TRUE);
			result.add(ex);
		}
		assignLearnDifficulty(result);
		return result;
	}

	/**
	 * 句子学习按句子长度排序后三等分赋难度，保证从基础到挑战递进。
	 */
	private void assignLearnDifficulty(List<EnglishSentenceExercise> list) {
		if (list.isEmpty()) {
			return;
		}
		list.sort((a, b) -> Integer.compare(wordCount(a.getEn()), wordCount(b.getEn())));
		int n = list.size();
		for (int i = 0; i < n; i++) {
			int level = n == 1 ? 1 : 1 + (i * 3 / n);
			list.get(i).setDifficulty(Math.min(3, level));
		}
	}

	private int groupWords(List<EnglishSentence> group) {
		int count = 0;
		for (EnglishSentence s : group) {
			if (s.getEn() != null) {
				count += s.getEn().trim().split("\\s+").length;
			}
		}
		return count;
	}

	private List<EnglishSentenceExercise> sortPractice(List<EnglishSentenceExercise> list) {
		List<EnglishSentenceExercise> sorted = new ArrayList<>(list);
		sorted.sort((a, b) -> {
			int da = a.getDifficulty() == null ? 1 : a.getDifficulty();
			int db = b.getDifficulty() == null ? 1 : b.getDifficulty();
			if (da != db) {
				return Integer.compare(da, db);
			}
			return Integer.compare(kindRank(a.getKind()), kindRank(b.getKind()));
		});
		return sorted;
	}

	private List<EnglishSentenceExercise> sortLearn(List<EnglishSentenceExercise> list) {
		List<EnglishSentenceExercise> sorted = new ArrayList<>(list);
		sorted.sort((a, b) -> {
			int da = a.getDifficulty() == null ? 1 : a.getDifficulty();
			int db = b.getDifficulty() == null ? 1 : b.getDifficulty();
			if (da != db) {
				return Integer.compare(da, db);
			}
			return Integer.compare(wordCount(a.getEn()), wordCount(b.getEn()));
		});
		return sorted;
	}

	private int kindRank(String kind) {
		if ("choice".equals(kind)) {
			return 0;
		}
		if ("fill".equals(kind)) {
			return 1;
		}
		if ("build".equals(kind)) {
			return 2;
		}
		return 3;
	}

	private int wordCount(String s) {
		return isBlank(s) ? 0 : s.trim().split("\\s+").length;
	}

	private int difficultyOf(String en) {
		int words = wordCount(en);
		if (words <= 7) {
			return 1;
		}
		if (words <= 14) {
			return 2;
		}
		return 3;
	}

	private int readInt(JsonNode node, String key, int fallback) {
		JsonNode value = node.path(key);
		if (value.isInt() || value.isLong()) {
			return value.asInt();
		}
		if (value.isTextual()) {
			try {
				return Integer.parseInt(value.asText().trim());
			} catch (NumberFormatException ignored) {
				return fallback;
			}
		}
		return fallback;
	}

	private boolean containsWord(String sentence, String word) {
		if (isBlank(sentence) || isBlank(word)) {
			return false;
		}
		return java.util.regex.Pattern.compile("(?i)(?<![A-Za-z])" + java.util.regex.Pattern.quote(word.trim()) + "(?![A-Za-z])")
				.matcher(sentence).find();
	}

	/**
	 * 依据英文句子匹配来源句子 ID（忽略大小写与首尾空白与结尾标点）。
	 */
	private String matchSentenceId(List<EnglishSentence> sentences, String en) {
		if (isBlank(en)) {
			return null;
		}
		String target = normalizeKey(en);
		for (EnglishSentence s : sentences) {
			if (s.getEn() != null && normalizeKey(s.getEn()).equals(target)) {
				return s.getId();
			}
		}
		return null;
	}

	private static String normalizeKey(String s) {
		return s.trim().toLowerCase(Locale.ROOT).replaceAll("[.!?。！？\\s]+$", "");
	}

	// ------------------------------------------------------------------ 保存 / 查询

	@Override
	public EnglishSentencePackView savePack(EnglishSentencePackView pack) {
		if (pack == null) {
			throw new ValidationException("请先生成内容再保存");
		}
		requireNotBlank(pack.getVersion(), "教材版本");
		requireNotBlank(pack.getGrade(), "年级");
		requireNotBlank(pack.getTerm(), "册别");
		requireNotBlank(pack.getUnit(), "单元");
		if (isBlank(pack.getContent())) {
			throw new ValidationException("请先生成内容再保存");
		}

		EnglishSentencePack entity = packMapper.selectOne(Wrappers.<EnglishSentencePack>lambdaQuery()
				.eq(EnglishSentencePack::getVersion, pack.getVersion())
				.eq(EnglishSentencePack::getGrade, pack.getGrade())
				.eq(EnglishSentencePack::getTerm, pack.getTerm())
				.eq(EnglishSentencePack::getUnit, pack.getUnit())
				.last("limit 1"));
		boolean update = entity != null;
		if (entity == null) {
			entity = new EnglishSentencePack();
		}
		entity.setVersion(pack.getVersion());
		entity.setGrade(pack.getGrade());
		entity.setTerm(pack.getTerm());
		entity.setUnit(pack.getUnit());
		entity.setContent(pack.getContent());
		entity.setCount(countExercises(pack.getContent()));

		if (update) {
			packMapper.updateById(entity);
		} else {
			packMapper.insert(entity);
		}
		return toView(packMapper.selectById(entity.getId()));
	}

	@Override
	public PaginationResponse<EnglishSentencePackView> listPacks(EnglishSentencePackQuery query) {
		LambdaQueryWrapper<EnglishSentencePack> wrapper = Wrappers.<EnglishSentencePack>lambdaQuery()
				.eq(notBlank(query.getVersion()), EnglishSentencePack::getVersion, query.getVersion())
				.eq(notBlank(query.getGrade()), EnglishSentencePack::getGrade, query.getGrade())
				.eq(notBlank(query.getTerm()), EnglishSentencePack::getTerm, query.getTerm())
				.eq(notBlank(query.getUnit()), EnglishSentencePack::getUnit, query.getUnit())
				.orderByDesc(EnglishSentencePack::getCreateAt);

		Page<EnglishSentencePack> page = new Page<>(query.getCurrent(), query.getPageSize());
		Page<EnglishSentencePack> result = packMapper.selectPage(page, wrapper);

		List<EnglishSentencePackView> views = result.getRecords().stream().map(pack -> {
			EnglishSentencePackView view = toView(pack);
			// 列表不下发正文，控制传输体积
			view.setContent(null);
			return view;
		}).collect(Collectors.toList());

		return new PaginationResponse<>(result.getTotal(), views);
	}

	@Override
	public EnglishSentencePackView getPack(String id) {
		EnglishSentencePack pack = packMapper.selectById(id);
		if (pack == null) {
			throw new ValidationException("句子练习内容包不存在或已被删除");
		}
		return toView(pack);
	}

	@Override
	public void deletePack(String id) {
		packMapper.deleteById(id);
	}

	@Override
	public List<String> listUnits(String version, String grade, String term) {
		requireNotBlank(version, "教材版本");
		requireNotBlank(grade, "年级");
		requireNotBlank(term, "册别");

		Set<String> units = new LinkedHashSet<>();
		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.select(EnglishSentence::getUnit)
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term));
		for (EnglishSentence s : sentences) {
			if (notBlank(s.getUnit())) {
				units.add(s.getUnit().trim());
			}
		}
		return new ArrayList<>(units);
	}

	// ------------------------------------------------------------------ 学员端只读

	@Override
	public List<EnglishSentenceExercise> getExercises(String version, String grade, String term, String unit,
			String stage) {
		EnglishSentencePack pack = findLatest(version, grade, term, unit);
		if (pack == null || isBlank(pack.getContent())) {
			return new ArrayList<>();
		}
		String wanted = isBlank(stage) ? "practice" : stage.trim().toLowerCase(Locale.ROOT);
		try {
			JsonNode arr = objectMapper.readTree(pack.getContent());
			List<EnglishSentenceExercise> result = new ArrayList<>();
			if (arr.isArray()) {
				for (JsonNode node : arr) {
					EnglishSentenceExercise ex = objectMapper.treeToValue(node, EnglishSentenceExercise.class);
					String exStage = isBlank(ex.getStage()) ? "practice" : ex.getStage();
					if (wanted.equalsIgnoreCase(exStage)) {
						result.add(ex);
					}
				}
			}
			return result;
		} catch (Exception e) {
			log.error("句子练习内容包解析失败：{}", e.getMessage());
			return new ArrayList<>();
		}
	}

	// ------------------------------------------------------------------ 辅助

	private EnglishSentencePack findLatest(String version, String grade, String term, String unit) {
		if (isBlank(version) || isBlank(grade) || isBlank(unit)) {
			return null;
		}
		return packMapper.selectOne(Wrappers.<EnglishSentencePack>lambdaQuery()
				.eq(EnglishSentencePack::getVersion, version)
				.eq(EnglishSentencePack::getGrade, grade)
				.eq(EnglishSentencePack::getUnit, unit)
				.eq(notBlank(term), EnglishSentencePack::getTerm, term)
				.orderByDesc(EnglishSentencePack::getCreateAt)
				.last("limit 1"));
	}

	private int countExercises(String content) {
		if (isBlank(content)) {
			return 0;
		}
		try {
			JsonNode node = objectMapper.readTree(content);
			return node.isArray() ? node.size() : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	private String writeExercises(List<EnglishSentenceExercise> list) {
		try {
			return objectMapper.writeValueAsString(list);
		} catch (Exception e) {
			return "[]";
		}
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private static <T> List<List<T>> partition(List<T> list, int size) {
		List<List<T>> result = new ArrayList<>();
		if (list == null || list.isEmpty()) {
			return result;
		}
		for (int i = 0; i < list.size(); i += size) {
			result.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
		}
		return result;
	}

	private static Map<String, String> message(String role, String content) {
		Map<String, String> map = new HashMap<>();
		map.put("role", role);
		map.put("content", content);
		return map;
	}

	private static JsonNode firstNode(JsonNode root, String... keys) {
		for (String key : keys) {
			JsonNode node = root.path(key);
			if (node.isArray()) {
				return node;
			}
		}
		return null;
	}

	private static String text(JsonNode node, String... keys) {
		for (String key : keys) {
			JsonNode value = node.path(key);
			if (value.isTextual() && !value.asText().trim().isEmpty()) {
				return value.asText().trim();
			}
		}
		return null;
	}

	private static List<String> readStringArray(JsonNode node, String... keys) {
		for (String key : keys) {
			JsonNode value = node.path(key);
			if (value.isArray()) {
				List<String> list = new ArrayList<>();
				for (JsonNode item : value) {
					if (item.isTextual() && !item.asText().trim().isEmpty()) {
						list.add(item.asText().trim());
					}
				}
				return list;
			}
		}
		return null;
	}

	private EnglishSentencePackView toView(EnglishSentencePack pack) {
		EnglishSentencePackView view = new EnglishSentencePackView();
		view.setId(pack.getId());
		view.setVersion(pack.getVersion());
		view.setGrade(pack.getGrade());
		view.setTerm(pack.getTerm());
		view.setUnit(pack.getUnit());
		view.setContent(pack.getContent());
		view.setCount(pack.getCount());
		view.setCreateAt(pack.getCreateAt());
		return view;
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static boolean notBlank(String s) {
		return !isBlank(s);
	}

	private static void requireNotBlank(String value, String name) {
		if (isBlank(value)) {
			throw new ValidationException(name + "不能为空");
		}
	}

}
